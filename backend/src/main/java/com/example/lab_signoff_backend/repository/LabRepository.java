package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Lab;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LabRepository extends MongoRepository<Lab, String> {
    // You can add finders like: List<Lab> findByTitleContainingIgnoreCase(String
    // q);
}
