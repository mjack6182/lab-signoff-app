package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private LabService labService;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GroupService groupService;

    @Test
    void getGroupsByLabId_autoInitializesCheckpointProgress() {
        Group group = new Group();
        group.setId("id-1");
        group.setGroupId("g1");
        group.setLabId("lab-1");

        Lab lab = new Lab("class-1", "Lab 1", 2, "creator");

        when(groupRepository.findByLabId("lab-1")).thenReturn(List.of(group));
        when(labRepository.findById("lab-1")).thenReturn(Optional.of(lab));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Group> groups = groupService.getGroupsByLabId("lab-1");

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getCheckpointProgress().size());
    }

    @Test
    void updateCheckpointProgress_setsStatusAndMetadata() {
        Group group = new Group();
        group.setId("id-2");
        group.setGroupId("g2");
        group.setLabId("lab-2");

        Lab lab = new Lab("class-2", "Lab 2", 1, "creator");

        when(groupRepository.findByGroupId("g2")).thenReturn(Optional.of(group));
        when(labRepository.findById("lab-2")).thenReturn(Optional.of(lab));
        when(groupRepository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckpointProgress progress = groupService.updateCheckpointProgress(
                "g2",
                1,
                SignoffAction.PASS.name(),
                "instructor-1",
                "Dr. Who",
                "Looks good",
                10
        );

        assertEquals(SignoffAction.PASS, progress.getStatus());
        assertEquals("instructor-1", progress.getSignedOffBy());
        assertEquals(10, progress.getPointsAwarded());
        assertEquals(1, progress.getCheckpointNumber());
        assertNotNull(progress.getTimestamp());
    }
}
