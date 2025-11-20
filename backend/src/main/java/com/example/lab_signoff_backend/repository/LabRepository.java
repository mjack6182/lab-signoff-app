package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Lab;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabRepository extends MongoRepository<Lab, String> {
    List<Lab> findByClassId(String classId);

    /**
     * Find a lab by its join code (case-insensitive).
     *
     * @param joinCode The unique join code students use
     * @return Optional containing the lab if found
     */
    Optional<Lab> findByJoinCodeIgnoreCase(String joinCode);
}
