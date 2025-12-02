package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceRandomizeTest {

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
    void randomizeGroups_respectsGenerationNumber() {
        Lab lab = new Lab("class-1", "Lab", 2, "inst");
        lab.setId("lab-1");
        when(labService.getAll()).thenReturn(List.of(lab));
        when(enrollmentService.getActiveStudents("class-1")).thenReturn(List.of(
                new Enrollment("s1", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT),
                new Enrollment("s2", "class-1", com.example.lab_signoff_backend.model.enums.EnrollmentRole.STUDENT)
        ));
        User u1 = new User("a", "a@x.com", "A", null, List.of("Student")); u1.setId("s1");
        User u2 = new User("b", "b@x.com", "B", null, List.of("Student")); u2.setId("s2");
        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));
        Group existing = new Group(); existing.setGenerationNumber(3); existing.setLabId("lab-1");
        when(groupRepository.findByLabId("lab-1")).thenReturn(List.of(existing));
        when(groupRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Group> groups = groupService.randomizeGroups("lab-1");
        assertEquals(4, groups.getFirst().getGenerationNumber());
    }

    @Test
    void randomizeGroups_labNotFoundThrows() {
        when(labService.getAll()).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> groupService.randomizeGroups("missing"));
    }
}
