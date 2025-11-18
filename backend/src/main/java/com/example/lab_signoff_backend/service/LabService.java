package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    /**
     * Retrieves all labs belonging to a specific class
     *
     * @param classId The class identifier
     * @return List of labs for the class
     */
    public List<Lab> getLabsByClassId(String classId) {
        return repo.findByClassId(classId);
    }

    /**
     * Find a lab using its public join code.
     *
     * @param joinCode The join code provided by students
     * @return Optional containing the lab if it exists
     */
    public Optional<Lab> getByJoinCode(String joinCode) {
        if (joinCode == null) {
            return Optional.empty();
        }
        return repo.findByJoinCodeIgnoreCase(joinCode.trim());
    }

    /**
     * Find a lab by its identifier.
     *
     * @param id The lab id
     * @return Optional containing the lab if found
     */
    public Optional<Lab> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return repo.findById(id);
    }
}
