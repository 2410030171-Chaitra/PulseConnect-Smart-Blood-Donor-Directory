package com.pulseconnect.mongo.repo;

import com.pulseconnect.mongo.doc.DonorDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DonorDocRepository extends MongoRepository<DonorDoc, String> {
    void deleteByDonorId(Long donorId);
}
