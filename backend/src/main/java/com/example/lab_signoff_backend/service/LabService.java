package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service class for Lab entity business logic.
 *
 * Provides methods for managing lab assignments including retrieval,
 * creation, updates, and validation.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@Service
public class LabService {
    private final LabRepository repo;

    /**
     * Constructor for LabService.
     *
     * @param repo The LabRepository for database operations
     */
    public LabService(LabRepository repo) {
        this.repo = repo;
    }

    /**
     * Retrieves all labs from the database.
     *
     * @return List of all labs
     */
    public List<Lab> getAll() {
        return repo.findAll();
    }

    /**
     * Creates or updates a lab in the database.
     *
     * @param lab The lab to save or update
     * @return The saved lab
     */
    public Lab upsert(Lab lab) {
        return repo.save(lab);
    }

    /**
     * Check if a lab exists by ID
     *
     * @param id The lab identifier
     * @return true if the lab exists, false otherwise
     */
    public boolean labExists(String id) {
        return repo.existsById(id);
    }
}