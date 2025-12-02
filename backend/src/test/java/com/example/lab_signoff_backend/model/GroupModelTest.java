package com.example.lab_signoff_backend.model;

import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.GroupStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GroupModelTest {

    @Test
    void addAndRemoveMembers_updateList() {
        Group group = new Group();
        GroupMember member = new GroupMember();
        member.setUserId("u1");
        group.addMember(member);
        group.addMember(member); // duplicate ignored
        assertEquals(1, group.getMembers().size());

        group.removeMember("u1");
        assertTrue(group.getMembers().isEmpty());
    }

    @Test
    void statusTransitions_setFlagsAndTimestamps() {
        Group group = new Group();
        group.startProgress();
        assertEquals(GroupStatus.IN_PROGRESS, group.getStatus());

        group.complete();
        assertEquals(GroupStatus.COMPLETED, group.getStatus());
        assertNotNull(group.getCompletedAt());

        group.signOff();
        assertEquals(GroupStatus.SIGNED_OFF, group.getStatus());
        assertTrue(group.getLastUpdatedAt().isAfter(group.getCreatedAt()) || group.getLastUpdatedAt().equals(group.getCreatedAt()));
    }

    @Test
    void deprecatedConstructor_handlesInvalidStatus() {
        Group group = new Group("id", "gid", "lab", java.util.List.of("u1"), "bad-status");
        assertEquals(GroupStatus.IN_PROGRESS, group.getStatus());
        assertEquals(1, group.getMembers().size());
        assertEquals("u1", group.getMembers().getFirst().getUserId());
    }
}
