package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.model.embedded.CheckpointProgress;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.SignoffAction;
import com.example.lab_signoff_backend.model.User;
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
    void randomizeGroups_distributesStudentsIntoGroups() {
        Lab lab = new Lab("class-1", "Lab 1", 3, "inst");
        lab.setId("lab-1");
        Enrollment e1 = new Enrollment("s1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        Enrollment e2 = new Enrollment("s2", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        Enrollment e3 = new Enrollment("s3", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);

        User u1 = new User("auth0|s1", "s1@example.com", "S1", null, List.of("Student")); u1.setId("s1");
        User u2 = new User("auth0|s2", "s2@example.com", "S2", null, List.of("Student")); u2.setId("s2");
        User u3 = new User("auth0|s3", "s3@example.com", "S3", null, List.of("Student")); u3.setId("s3");

        when(labService.getAll()).thenReturn(List.of(lab));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(e1, e2, e3));
        when(userService.getAllUsers()).thenReturn(List.of(u1, u2, u3));
        when(groupRepository.findByLabId("lab-1")).thenReturn(List.of());
        when(groupRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Group> groups = groupService.randomizeGroups("lab-1");

        int memberCount = groups.stream().mapToInt(g -> g.getMembers().size()).sum();
        assertEquals(3, memberCount);
        assertFalse(groups.isEmpty());
    }

    @Test
    void randomizeGroups_noStudentsThrows() {
        Lab lab = new Lab("class-1", "Lab 1", 3, "inst");
        lab.setId("lab-1");
        when(labService.getAll()).thenReturn(List.of(lab));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of());

        assertThrows(RuntimeException.class, () -> groupService.randomizeGroups("lab-1"));
    }

    @Test
    void bulkUpdateGroups_validatesStudents() {
        Lab lab = new Lab("class-1", "Lab 1", 3, "inst");
        lab.setId("lab-1");
        when(labService.getAll()).thenReturn(List.of(lab));
        Enrollment e = new Enrollment("s1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(e));

        Group group = new Group();
        group.setGroupId("g1");
        GroupMember member = new GroupMember();
        member.setUserId("bad");
        group.setMembers(List.of(member));

        assertThrows(RuntimeException.class, () -> groupService.bulkUpdateGroups("lab-1", List.of(group)));
    }

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

    @Test
    void bulkUpdateGroups_successSaves() {
        Lab lab = new Lab("class-1", "Lab 1", 3, "inst");
        lab.setId("lab-1");
        when(labService.getAll()).thenReturn(List.of(lab));
        Enrollment e = new Enrollment("s1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT);
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(e));
        when(groupRepository.findByLabId("lab-1")).thenReturn(List.of());
        when(groupRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        Group group = new Group();
        GroupMember member = new GroupMember();
        member.setUserId("s1");
        group.setMembers(List.of(member));

        List<Group> saved = groupService.bulkUpdateGroups("lab-1", List.of(group));
        assertEquals("lab-1", saved.getFirst().getLabId());
    }

    @Test
    void updateCheckpointProgress_missingGroupThrows() {
        when(groupRepository.findByGroupId("g-miss")).thenReturn(Optional.empty());
        when(groupRepository.findById("g-miss")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> groupService.updateCheckpointProgress(
                "g-miss", 1, SignoffAction.PASS.name(), "u", "n", "", null));
    }

    @Test
    void getAll_initializesCheckpointsWhenEmpty() {
        Group g = new Group();
        g.setLabId("lab-init");
        g.setCheckpointProgress(new java.util.ArrayList<>());
        when(groupRepository.findAll()).thenReturn(List.of(g));
        Lab lab = new Lab("class-x", "Lab", 2, "inst");
        lab.setId("lab-init");
        when(labRepository.findById("lab-init")).thenReturn(Optional.of(lab));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Group> result = groupService.getAll();

        assertEquals(2, result.getFirst().getCheckpointProgress().size());
    }

    @Test
    void calculateOptimalGroupCount_respectsMinSize() {
        int groups = (int) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                groupService, "calculateOptimalGroupCount", 5, 3, 4);
        assertTrue(groups >= 1);
    }
}
