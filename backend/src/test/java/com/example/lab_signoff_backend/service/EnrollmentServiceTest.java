package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Enrollment;
import com.example.lab_signoff_backend.model.enums.EnrollmentRole;
import com.example.lab_signoff_backend.model.enums.EnrollmentStatus;
import com.example.lab_signoff_backend.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository repository;

    @InjectMocks
    private EnrollmentService service;

    @Test
    void enrollStudent_createsNewEnrollment() {
        when(repository.existsByUserIdAndClassId("u1", "c1")).thenReturn(false);
        when(repository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment enrollment = service.enrollStudent("u1", "c1");

        assertEquals(EnrollmentRole.STUDENT, enrollment.getRole());
        assertEquals("u1", enrollment.getUserId());
        verify(repository).save(any(Enrollment.class));
    }

    @Test
    void enrollStudent_whenAlreadyExists_throws() {
        when(repository.existsByUserIdAndClassId("u1", "c1")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.enrollStudent("u1", "c1"));
    }

    @Test
    void dropEnrollment_updatesStatus() {
        Enrollment enrollment = new Enrollment("u2", "c2", EnrollmentRole.STUDENT);
        when(repository.findById("enr-1")).thenReturn(Optional.of(enrollment));
        when(repository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment result = service.dropEnrollment("enr-1");

        assertEquals(EnrollmentStatus.DROPPED, result.getStatus());
    }

    @Test
    void completeEnrollment_updatesStatus() {
        Enrollment enrollment = new Enrollment("u3", "c3", EnrollmentRole.STUDENT);
        when(repository.findById("enr-2")).thenReturn(Optional.of(enrollment));
        when(repository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment result = service.completeEnrollment("enr-2");

        assertEquals(EnrollmentStatus.COMPLETED, result.getStatus());
    }

    @Test
    void isActiveStaff_checksRoleAndStatus() {
        Enrollment staff = new Enrollment("u4", "c4", EnrollmentRole.TA);
        when(repository.findByUserIdAndClassId("u4", "c4")).thenReturn(Optional.of(staff));

        assertTrue(service.isActiveStaff("u4", "c4"));
        assertFalse(service.isActiveStudent("u4", "c4"));
    }

    @Test
    void countingDelegatesToRepository() {
        when(repository.countByClassIdAndRole("classX", EnrollmentRole.STUDENT)).thenReturn(5L);
        when(repository.countByClassIdAndRole("classX", EnrollmentRole.TA)).thenReturn(2L);

        assertEquals(5L, service.countStudents("classX"));
        assertEquals(2L, service.countTAs("classX"));
    }
}
