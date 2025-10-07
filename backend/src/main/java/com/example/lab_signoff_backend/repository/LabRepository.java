package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Lab;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for Lab entity operations.
 *
 * Provides CRUD operations and custom query methods for Lab documents
 * stored in MongoDB.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
public interface LabRepository extends MongoRepository<Lab, String> {
    // You can add finders like: List<Lab> findByTitleContainingIgnoreCase(String
    // q);
}
