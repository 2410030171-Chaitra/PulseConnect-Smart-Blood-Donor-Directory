package com.pulseconnect.mongo;

import com.pulseconnect.model.Donor;
import com.pulseconnect.model.User;
import com.pulseconnect.mongo.doc.DonorDoc;
import com.pulseconnect.mongo.doc.UserDoc;
import com.pulseconnect.mongo.repo.DonorDocRepository;
import com.pulseconnect.mongo.repo.UserDocRepository;
import com.pulseconnect.repository.DonorRepository;
import com.pulseconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MongoSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MongoSyncRunner.class);

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final UserDocRepository userDocRepository;
    private final DonorDocRepository donorDocRepository;

    @Value("${mongo.sync.enabled:true}")
    private boolean syncEnabled;

    public MongoSyncRunner(UserRepository userRepository,
                           DonorRepository donorRepository,
                           UserDocRepository userDocRepository,
                           DonorDocRepository donorDocRepository) {
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.userDocRepository = userDocRepository;
        this.donorDocRepository = donorDocRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!syncEnabled) {
            log.info("Mongo sync disabled (mongo.sync.enabled=false)");
            return;
        }
        try {
            log.info("[Mongo] Synchronizing relational data to MongoDB for Compass view...");

            // Users
            List<UserDoc> userDocs = userRepository.findAll().stream().map(this::toUserDoc).collect(Collectors.toList());
            userDocRepository.deleteAll();
            userDocRepository.saveAll(userDocs);

            // Donors
            List<DonorDoc> donorDocs = donorRepository.findAll().stream().map(this::toDonorDoc).collect(Collectors.toList());
            donorDocRepository.deleteAll();
            donorDocRepository.saveAll(donorDocs);

            log.info("[Mongo] Sync complete: users={} donors={}", userDocs.size(), donorDocs.size());
        } catch (Exception ex) {
            log.warn("[Mongo] Sync skipped (Mongo not reachable or misconfigured): {}", ex.getMessage());
        }
    }

    private UserDoc toUserDoc(User u) {
        return new UserDoc(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getPhoneNumber(),
                u.getBloodGroup() != null ? u.getBloodGroup().name() : null,
                u.getCity(),
                u.getLatitude(),
                u.getLongitude(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getStatus() != null ? u.getStatus().name() : null
        );
    }

    private DonorDoc toDonorDoc(Donor d) {
        User u = d.getUser();
        return new DonorDoc(
                d.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getFullName() : d.getName(),
                u != null ? u.getPhoneNumber() : d.getPhone(),
                u != null && u.getBloodGroup() != null ? u.getBloodGroup().name() : d.getBloodGroup(),
                u != null ? u.getCity() : d.getCity(),
                d.getIsEligible(),
                d.getIsAvailable(),
                d.getPriorityScore(),
                u != null ? u.getLatitude() : null,
                u != null ? u.getLongitude() : null
        );
    }
}
