package com.pulseconnect.mongo.repo;

import com.pulseconnect.mongo.doc.UserDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserDocRepository extends MongoRepository<UserDoc, String> {
    void deleteByUserId(Long userId);
}
