package com.example.lab_signoff_backend.repository;

import com.example.lab_signoff_backend.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository; 

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    /**
     * Find all groups associated with a specific lab
     *
     * @param labId The lab identifier to search for
     * @return List of groups associated with the lab
     */
    List<Group> findByLabId(String labId);
}
