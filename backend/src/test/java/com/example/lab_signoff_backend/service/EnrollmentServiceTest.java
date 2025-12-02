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
    void enrollUser_createsWithRole() {
        when(repository.existsByUserIdAndClassId("uNew", "cNew")).thenReturn(false);
        when(repository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment e = service.enrollUser("uNew", "cNew", EnrollmentRole.TEACHER);
        assertEquals(EnrollmentRole.TEACHER, e.getRole());
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
    void dropEnrollment_notFoundThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.dropEnrollment("missing"));
    }

    @Test
    void completeEnrollment_notFoundThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.completeEnrollment("missing"));
    }

    @Test
    void upgradeToTA_updatesRole() {
        Enrollment enrollment = new Enrollment("u6", "c6", EnrollmentRole.STUDENT);
        when(repository.findById("en6")).thenReturn(Optional.of(enrollment));
        when(repository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment updated = service.upgradeToTA("en6", "teacher");
        assertEquals(EnrollmentRole.TA, updated.getRole());
    }

    @Test
    void getStaff_combinesTeachersAndTAs() {
        Enrollment teacher = new Enrollment("t1", "c1", EnrollmentRole.TEACHER);
        Enrollment ta = new Enrollment("ta1", "c1", EnrollmentRole.TA);
        when(repository.findByClassIdAndRole("c1", EnrollmentRole.TA)).thenReturn(new java.util.ArrayList<>(List.of(ta)));
        when(repository.findByClassIdAndRole("c1", EnrollmentRole.TEACHER)).thenReturn(new java.util.ArrayList<>(List.of(teacher)));

        List<Enrollment> staff = service.getStaff("c1");
        assertEquals(2, staff.size());
    }

    @Test
    void isEnrolled_delegatesExists() {
        when(repository.existsByUserIdAndClassId("u1", "c1")).thenReturn(true);
        assertTrue(service.isEnrolled("u1", "c1"));
    }

    @Test
    void isActiveStaff_checksRoleAndStatus() {
        Enrollment staff = new Enrollment("u4", "c4", EnrollmentRole.TA);
        when(repository.findByUserIdAndClassId("u4", "c4")).thenReturn(Optional.of(staff));

        assertTrue(service.isActiveStaff("u4", "c4"));
        assertFalse(service.isActiveStudent("u4", "c4"));
    }

    @Test
    void isActiveStudent_returnsFalseWhenNotFound() {
        when(repository.findByUserIdAndClassId("u-miss", "c-miss")).thenReturn(Optional.empty());
        assertFalse(service.isActiveStudent("u-miss", "c-miss"));
    }

    @Test
    void isActiveTA_returnsFalseWhenStatusDropped() {
        Enrollment ta = new Enrollment("u7", "c7", EnrollmentRole.TA);
        ta.setStatus(EnrollmentStatus.DROPPED);
        when(repository.findByUserIdAndClassId("u7", "c7")).thenReturn(Optional.of(ta));

        assertFalse(service.isActiveTA("u7", "c7"));
    }

    @Test
    void dropEnrollment_withMissingIdThrows() {
        when(repository.findById("none")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.dropEnrollment("none"));
    }

    @Test
    void completeEnrollment_withMissingIdThrows() {
        when(repository.findById("none")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.completeEnrollment("none"));
    }

    @Test
    void getEnrollment_foundReturnsOptional() {
        Enrollment enrollment = new Enrollment("u10", "c10", EnrollmentRole.STUDENT);
        when(repository.findById("e1")).thenReturn(Optional.of(enrollment));
        Optional<Enrollment> result = service.getEnrollmentById("e1");
        assertTrue(result.isPresent());
    }

    @Test
    void upgradeToTA_missingThrows() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.upgradeToTA("missing", "teacher"));
    }

    @Test
    void changeRole_updatesAndPersists() {
        Enrollment enrollment = new Enrollment("u12", "c12", EnrollmentRole.STUDENT);
        when(repository.findById("enr-12")).thenReturn(Optional.of(enrollment));
        when(repository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment updated = service.changeRole("enr-12", EnrollmentRole.TEACHER, "admin");
        assertEquals(EnrollmentRole.TEACHER, updated.getRole());
    }

    @Test
    void changeRole_missingThrows() {
        when(repository.findById("missing-role")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.changeRole("missing-role", EnrollmentRole.TA, "admin"));
    }

    @Test
    void getActiveStudentsAndTAs_delegatesQueries() {
        service.getActiveStudents("cActive");
        verify(repository).findByClassIdAndRoleAndStatus("cActive", EnrollmentRole.STUDENT, EnrollmentStatus.ACTIVE);

        service.getActiveTAs("cActive");
        verify(repository).findByClassIdAndRoleAndStatus("cActive", EnrollmentRole.TA, EnrollmentStatus.ACTIVE);
    }

    @Test
    void getStudentsAndTAsAndEnrollments_delegatesQueries() {
        service.getStudents("classS");
        verify(repository).findByClassIdAndRole("classS", EnrollmentRole.STUDENT);

        service.getTAs("classS");
        verify(repository).findByClassIdAndRole("classS", EnrollmentRole.TA);

        service.getEnrollmentsByUser("uX");
        verify(repository).findByUserId("uX");

        service.getActiveEnrollmentsByUser("uX");
        verify(repository).findByUserIdAndStatus("uX", EnrollmentStatus.ACTIVE);
    }

    @Test
    void isActiveStaff_falseWhenNotStaffOrInactive() {
        Enrollment enrollment = new Enrollment("u13", "c13", EnrollmentRole.STUDENT);
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        when(repository.findByUserIdAndClassId("u13", "c13")).thenReturn(Optional.of(enrollment));

        assertFalse(service.isActiveStaff("u13", "c13"));
    }

    @Test
    void enrollUser_whenExistsThrows() {
        when(repository.existsByUserIdAndClassId("u1", "c1")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.enrollUser("u1", "c1", EnrollmentRole.TA));
    }

    @Test
    void enrollStudent_savesWhenNotExists() {
        when(repository.existsByUserIdAndClassId("u2", "c2")).thenReturn(false);
        when(repository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment e = service.enrollStudent("u2", "c2");

        assertEquals("u2", e.getUserId());
        verify(repository).save(any(Enrollment.class));
    }

    @Test
    void isActiveTA_trueWhenActive() {
        Enrollment ta = new Enrollment("u8", "c8", EnrollmentRole.TA);
        ta.setStatus(EnrollmentStatus.ACTIVE);
        when(repository.findByUserIdAndClassId("u8", "c8")).thenReturn(Optional.of(ta));

        assertTrue(service.isActiveTA("u8", "c8"));
    }

    @Test
    void isActiveStaff_missingReturnsFalse() {
        when(repository.findByUserIdAndClassId("missing", "class")).thenReturn(Optional.empty());
        assertFalse(service.isActiveStaff("missing", "class"));
    }

    @Test
    void getEnrollmentsByClass_returnsList() {
        Enrollment e = new Enrollment("u11", "c11", EnrollmentRole.STUDENT);
        when(repository.findByClassId("c11")).thenReturn(List.of(e));
        assertEquals(1, service.getEnrollmentsByClass("c11").size());
    }

    @Test
    void countingDelegatesToRepository() {
        when(repository.countByClassIdAndRole("classX", EnrollmentRole.STUDENT)).thenReturn(5L);
        when(repository.countByClassIdAndRole("classX", EnrollmentRole.TA)).thenReturn(2L);

        assertEquals(5L, service.countStudents("classX"));
        assertEquals(2L, service.countTAs("classX"));
    }

}
