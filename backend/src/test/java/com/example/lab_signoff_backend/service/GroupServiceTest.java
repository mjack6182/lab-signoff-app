package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.repository.LabRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    @Mock
    private GroupRepository groupRepo;

    @Mock
    private LabRepository labRepo;

    @Mock
    private LabService labService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private GroupService groupService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateCheckpointProgress_createsAndUpdatesCheckpoint() {
        // Arrange
        String groupId = "group123";
        int checkpointNum = 1;

        Group group = new Group();
        group.setId(groupId);
        group.setLabId("lab1");
        group.setCheckpointProgress(new ArrayList<>());

        Lab lab = new Lab();
        lab.setId("lab1");
        lab.setPoints(3);

        when(groupRepo.findByGroupId(groupId)).thenReturn(Optional.of(group));
        when(labRepo.findById("lab1")).thenReturn(Optional.of(lab));
        when(groupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CheckpointProgress result = groupService.updateCheckpointProgress(
                groupId,
                checkpointNum,
                "COMPLETE",  // FIXED — matches enum
                "teacher123",
                "Dr. Smith",
                "Good job",
                10
        );

        // Assert
        assertNotNull(result);
        assertEquals(checkpointNum, result.getCheckpointNumber());
        assertEquals(SignoffAction.COMPLETE, result.getStatus());  // FIXED
        assertEquals("teacher123", result.getSignedOffBy());
        assertEquals("Dr. Smith", result.getSignedOffByName());
        assertEquals("Good job", result.getNotes());
        assertEquals(10, result.getPointsAwarded());
    }

    @Test
    void updateCheckpointProgress_throwsWhenGroupMissing() {
        when(groupRepo.findByGroupId("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> groupService.updateCheckpointProgress(
                        "missing",
                        1,
                        "COMPLETE",  // FIXED
                        null,
                        null,
                        null,
                        null
                )
        );

        assertTrue(ex.getMessage().contains("Group not found"));
    }

    @Test
    void autoInitCheckpoints_initializesOnlyIfEmpty() {
        // Arrange
        Group group = new Group();
        group.setId("g1");
        group.setLabId("lab123");
        group.setCheckpointProgress(new ArrayList<>());

        Lab lab = new Lab();
        lab.setId("lab123");
        lab.setPoints(4);

        when(labRepo.findById("lab123")).thenReturn(Optional.of(lab));
        when(groupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act (upsert should call autoInitCheckpoints internally)
        groupService.upsert(group);

        // Assert
        assertEquals(4, group.getCheckpointProgress().size());
    }
}
