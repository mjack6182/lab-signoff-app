package com.example.lab_signoff_backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Group model class representing a student group for a lab assignment.
 * This class is mapped to the "groups" collection in MongoDB.
 *
 * Groups contain information about students working together on labs,
 * their progress, and signoff status.
 */
@Document(collection = "groups")
public class Group {
    /**
     * Unique identifier for the group
     */
    @Id
    private String id;

    /**
     * Custom group identifier (e.g., "Group-1", "Team-A")
     */
    private String groupId;

    /**
     * Reference to the lab this group is associated with
     */
    private String labId;

    /**
     * List of student IDs or names who are members of this group
     */
    private List<String> members;

    /**
     * Current status of the group (e.g., "pending", "in-progress", "completed", "signed-off")
     */
    private String status;

    /**
     * Default constructor for Group
     * Required for Spring Data MongoDB serialization/deserialization
     */
    public Group() {
    }

    /**
     * Constructor for creating a Group with all information
     *
     * @param id      Unique identifier for the group
     * @param groupId Custom group identifier
     * @param labId   Identifier of the lab this group belongs to
     * @param members List of member identifiers
     * @param status  Current status of the group
     */
    public Group(String id, String groupId, String labId, List<String> members, String status) {
        this.id = id;
        this.groupId = groupId;
        this.labId = labId;
        this.members = members;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getLabId() {
        return labId;
    }

    public void setLabId(String labId) {
        this.labId = labId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
