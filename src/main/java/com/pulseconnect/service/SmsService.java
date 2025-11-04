package com.pulseconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Provider-agnostic SMS service with pluggable backends.
 * Supported providers: twilio, fast2sms, log (no-op logger).
 */
@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${sms.provider:log}")
    private String providerName;

    @Value("${sms.defaultCountryCode:+91}")
    private String defaultCountryCode;

    // Twilio config
    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;
    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;
    @Value("${twilio.phone.number:}")
    private String twilioFromNumber;

    // Fast2SMS config
    @Value("${fast2sms.api.key:}")
    private String fast2SmsApiKey;
    @Value("${fast2sms.sender.id:}")
    private String fast2SmsSenderId;
    @Value("${fast2sms.route:v3}")
    private String fast2SmsRoute;

    private volatile SmsProvider provider;

    @PostConstruct
    public void init() {
        if (!smsEnabled) {
            log.info("[SMS] Disabled via configuration");
            provider = new SmsProvider.LogProvider();
            return;
        }
        String name = providerName == null ? "" : providerName.trim().toLowerCase();
        switch (name) {
            case "twilio" -> provider = SmsProvider.twilio(twilioAccountSid, twilioAuthToken, twilioFromNumber, defaultCountryCode);
            case "fast2sms" -> provider = SmsProvider.fast2sms(fast2SmsApiKey, fast2SmsSenderId, fast2SmsRoute);
            default -> {
                log.warn("[SMS] Unknown provider '{}', using 'log'", providerName);
                provider = new SmsProvider.LogProvider();
            }
        }
        if (!provider.isReady()) {
            log.warn("[SMS] Provider '{}' is not ready (missing config). SMS will be skipped.", name);
        } else {
            log.info("[SMS] Provider '{}' initialized.", name);
        }
    }

    public boolean isEnabled() { return smsEnabled && provider != null && provider.isReady(); }

    /**
     * Send an SMS to many recipients efficiently:
     * - de-duplicates numbers
     * - batches by provider capacity
     * - parallelizes with a small pool
     */
    public List<String> sendBulk(List<String> toNumbers, String message) {
        if (toNumbers == null || toNumbers.isEmpty()) return Collections.emptyList();

        if (!isEnabled()) {
            log.info("[SMS] Skipped (disabled or not initialized). Intended recipients: {}", toNumbers.size());
            return Collections.emptyList();
        }

        // De-duplicate and clean inputs (provider will perform final normalization)
        List<String> unique = toNumbers.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        // Chunking based on provider hints
        int batchSize = Math.max(1, provider.recommendedBatchSize());
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < unique.size(); i += batchSize) {
            batches.add(unique.subList(i, Math.min(i + batchSize, unique.size())));
        }

        int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<List<String>>> futures = new ArrayList<>();
        for (List<String> batch : batches) {
            futures.add(pool.submit(() -> provider.sendBulk(batch, message)));
        }
        List<String> sent = new ArrayList<>();
        for (Future<List<String>> f : futures) {
            try { sent.addAll(f.get(60, TimeUnit.SECONDS)); }
            catch (Exception ex) { log.warn("[SMS] Batch failed: {}", ex.getMessage()); }
        }
        pool.shutdown();
        log.info("[SMS] Sent {} / {} messages via {}", sent.size(), unique.size(), provider.getName());
        return sent;
    }
}

interface SmsProvider {
    String getName();
    boolean isReady();
    int recommendedBatchSize();
    List<String> sendBulk(List<String> toNumbers, String message);

    static SmsProvider twilio(String sid, String token, String from, String defaultCountry) {
        return new TwilioProvider(sid, token, from, defaultCountry);
    }
    static SmsProvider fast2sms(String apiKey, String senderId, String route) {
        return new Fast2SmsProvider(apiKey, senderId, route);
    }

    /** A no-op provider that just logs intended recipients (useful for dev). */
    class LogProvider implements SmsProvider {
        private static final Logger log = LoggerFactory.getLogger(LogProvider.class);
        @Override public String getName() { return "log"; }
        @Override public boolean isReady() { return true; }
        @Override public int recommendedBatchSize() { return 200; }
        @Override public List<String> sendBulk(List<String> toNumbers, String message) {
            log.info("[SMS/log] Would send to {} numbers. First few: {}", toNumbers.size(),
                    toNumbers.stream().limit(5).toList());
            return new ArrayList<>(toNumbers);
        }
    }
}

// --- Provider implementations ---

class TwilioProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(TwilioProvider.class);
    private final String sid; private final String token; private final String from; private final String defaultCountry;
    private volatile boolean initialized = false;

    TwilioProvider(String sid, String token, String from, String defaultCountry) {
        this.sid = sid; this.token = token; this.from = from; this.defaultCountry = (defaultCountry == null ? "+91" : defaultCountry);
        try {
            if (isReady()) {
                com.twilio.Twilio.init(sid, token);
                initialized = true;
            }
        } catch (Exception ex) {
            log.warn("[SMS/twilio] Failed to init: {}", ex.getMessage());
        }
    }
    @Override public String getName() { return "twilio"; }
    @Override public boolean isReady() { return notBlank(sid) && notBlank(token) && notBlank(from); }
    @Override public int recommendedBatchSize() { return 50; }
    @Override public List<String> sendBulk(List<String> toNumbers, String message) {
        if (!initialized) return List.of();
        List<String> sent = new ArrayList<>();
        for (String raw : toNumbers) {
            String to = normalizeE164(raw);
            if (to == null) continue;
            try {
                com.twilio.rest.api.v2010.account.Message.creator(
                        new com.twilio.type.PhoneNumber(to),
                        new com.twilio.type.PhoneNumber(from),
                        message
                ).create();
                sent.add(to);
            } catch (com.twilio.exception.ApiException api) {
                log.warn("[SMS/twilio] Failed to send to {}: {}", to, api.getMessage());
            } catch (Exception ex) {
                log.warn("[SMS/twilio] Error sending to {}: {}", to, ex.getMessage());
            }
        }
        return sent;
    }
    private String normalizeE164(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.startsWith("+")) return raw;
        if (digits.length() == 10 && defaultCountry != null) {
            String cc = defaultCountry.startsWith("+") ? defaultCountry : "+" + defaultCountry;
            return cc + digits;
        }
        if (raw.startsWith("+")) return raw; // already E.164
        return null;
    }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}

class Fast2SmsProvider implements SmsProvider {
    private static final Logger log = LoggerFactory.getLogger(Fast2SmsProvider.class);
    private final String apiKey; private final String senderId; private final String route;

    Fast2SmsProvider(String apiKey, String senderId, String route) {
        this.apiKey = apiKey; this.senderId = senderId; this.route = (route == null ? "v3" : route);
    }
    @Override public String getName() { return "fast2sms"; }
    @Override public boolean isReady() { return notBlank(apiKey); }
    @Override public int recommendedBatchSize() { return 200; }

    @Override
    public List<String> sendBulk(List<String> toNumbers, String message) {
        if (!isReady() || toNumbers == null || toNumbers.isEmpty()) return List.of();

        // Fast2SMS expects Indian mobile numbers without country code, comma-separated
        List<String> normalized = toNumbers.stream()
                .map(this::normalizeIndia)
                .filter(s -> s != null)
                .toList();
        if (normalized.isEmpty()) return List.of();

        String nums = String.join(",", normalized);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("authorization", apiKey);
        headers.add("accept", "application/json");
        headers.add("content-type", "application/x-www-form-urlencoded");

        org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("sender_id", senderId == null ? "FSTSMS" : senderId);
        body.add("message", message);
        body.add("language", "english");
        body.add("route", route);
        body.add("numbers", nums);

        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String,String>> request =
                new org.springframework.http.HttpEntity<>(body, headers);

        String url = "https://www.fast2sms.com/dev/bulkV2";
        try {
            org.springframework.web.client.RestTemplate rest = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> resp = rest.postForEntity(url, request, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("[SMS/fast2sms] Sent to {} numbers", normalized.size());
                return normalized;
            } else {
                log.warn("[SMS/fast2sms] Failed with status {}: {}", resp.getStatusCode(), resp.getBody());
            }
        } catch (Exception ex) {
            log.warn("[SMS/fast2sms] Error: {}", ex.getMessage());
        }
        return List.of();
    }

    private String normalizeIndia(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 10) return digits;
        if (digits.startsWith("91") && digits.length() == 12) return digits.substring(2);
        if (raw.startsWith("+91") && digits.length() == 12) return digits.substring(2);
        return null;
    }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
