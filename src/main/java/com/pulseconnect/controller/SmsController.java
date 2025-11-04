package com.pulseconnect.controller;

import com.pulseconnect.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple controller to send a single SMS using Fast2SMS (or the active provider).
 * Endpoint: POST /sendSMS
 * Body: { "message": "text", "number": "9999999999" }
 *
 * Notes:
 * - API key is configured via environment/property (see application.properties FAST2SMS_API_KEY)
 * - Enable CORS so that the static frontend can call this from localhost.
 */
@RestController
@CrossOrigin(origins = "*") // allow simple local testing from any origin
public class SmsController {
    private static final Logger log = LoggerFactory.getLogger(SmsController.class);

    private final SmsService smsService;

    public SmsController(SmsService smsService) { this.smsService = smsService; }

    @PostMapping(path = "/sendSMS", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> sendSms(@RequestBody Map<String, String> body) {
        Map<String, Object> resp = new HashMap<>();

        // 1) Validate input
        String message = (body.getOrDefault("message", "") + "").trim();
        String numberRaw = (body.getOrDefault("number", "") + "").trim();
        String digits = numberRaw.replaceAll("[^0-9]", "");

        if (message.isEmpty()) {
            resp.put("status", "error");
            resp.put("info", "Message must not be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }
        if (digits.length() != 10) { // minimal India-only validation for Fast2SMS
            resp.put("status", "error");
            resp.put("info", "Provide a valid 10-digit Indian mobile number");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        // 2) Send via provider-backed service (Fast2SMS when sms.provider=fast2sms)
        try {
            List<String> sent = smsService.sendBulk(List.of(digits), message);
            if (sent.isEmpty()) {
                resp.put("status", "error");
                resp.put("info", "SMS not sent (provider disabled or misconfigured)");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resp);
            }
            resp.put("status", "success");
            resp.put("info", "Message sent successfully");
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.warn("/sendSMS failed: {}", ex.getMessage());
            resp.put("status", "error");
            resp.put("info", "Failed to send SMS: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * Bulk endpoint: POST /sendSMS/bulk
     * Body can be either:
     * { "message": "...", "numbers": ["9999999999", "8888888888"] }
     * or { "message": "...", "numbersCsv": "9999999999,8888888888" }
     */
    @PostMapping(path = "/sendSMS/bulk", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> sendSmsBulk(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();

        String message = (String.valueOf(body.getOrDefault("message", "")).trim());
        if (message.isEmpty()) {
            resp.put("status", "error");
            resp.put("info", "Message must not be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        // Collect numbers from array or CSV
        java.util.List<String> rawList = new java.util.ArrayList<>();
        Object arr = body.get("numbers");
        if (arr instanceof java.util.List<?> list) {
            for (Object o : list) if (o != null) rawList.add(String.valueOf(o));
        }
        if (rawList.isEmpty() && body.get("numbersCsv") != null) {
            String csv = String.valueOf(body.get("numbersCsv"));
            for (String s : csv.split(",")) rawList.add(s.trim());
        }

        if (rawList.isEmpty()) {
            resp.put("status", "error");
            resp.put("info", "Provide 'numbers' array or 'numbersCsv'");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        // Normalize to 10-digit Indian mobiles for Fast2SMS
        java.util.Set<String> valid = new java.util.LinkedHashSet<>();
        java.util.List<String> invalid = new java.util.ArrayList<>();
        for (String s : rawList) {
            String digits = s == null ? "" : s.replaceAll("[^0-9]", "");
            if (digits.length() == 10) valid.add(digits); else invalid.add(s);
        }
        // Safety cap to avoid accidental floods
        int MAX = 200;
        java.util.List<String> toSend = new java.util.ArrayList<>(valid).subList(0, Math.min(valid.size(), MAX));

        if (toSend.isEmpty()) {
            resp.put("status", "error");
            resp.put("info", "No valid 10-digit numbers provided");
            resp.put("invalid", invalid);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        java.util.List<String> sent = smsService.sendBulk(toSend, message);
        resp.put("status", sent.isEmpty() ? "error" : "success");
        resp.put("requested", rawList.size());
        resp.put("valid", toSend.size());
        resp.put("sent", sent.size());
        resp.put("invalid", invalid);
        resp.put("info", sent.isEmpty() ? "SMS not sent (provider disabled/misconfigured)" :
                ("Sent to " + sent.size() + " number(s)"));
        return ResponseEntity.ok(resp);
    }
}