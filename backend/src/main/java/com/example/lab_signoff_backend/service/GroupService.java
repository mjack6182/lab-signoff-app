package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {
    private final GroupRepository repo;

    public GroupService(GroupRepository repo) {
        this.repo = repo;
    }

    /**
     * Retrieve all groups for a specific lab
     *
     * @param labId The lab identifier
     * @return List of groups associated with the lab
     */
    public List<Group> getGroupsByLabId(String labId) {
        return repo.findByLabId(labId);
    }

    /**
     * Retrieve all groups
     *
     * @return List of all groups
     */
    public List<Group> getAll() {
        return repo.findAll();
    }

    /**
     * Create or update a group
     *
     * @param group The group to save
     * @return The saved group
     */
    public Group upsert(Group group) {
        return repo.save(group);
    }
}
