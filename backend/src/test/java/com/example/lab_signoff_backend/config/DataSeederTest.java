package com.example.lab_signoff_backend.config;

import com.example.lab_signoff_backend.model.*;
import com.example.lab_signoff_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.CommandLineRunner;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSeederTest {

    @Test
    void initDatabase_runsWithoutErrorsWithEmptyRepos() throws Exception {
        UserRepository userRepo = mock(UserRepository.class);
        ClassRepository classRepo = mock(ClassRepository.class);
        EnrollmentRepository enrollmentRepo = mock(EnrollmentRepository.class);
        LabRepository labRepo = mock(LabRepository.class);
        GroupRepository groupRepo = mock(GroupRepository.class);
        HelpQueueItemRepository helpRepo = mock(HelpQueueItemRepository.class);
        SignoffEventRepository signoffRepo = mock(SignoffEventRepository.class);

        // return empty lookups so seeder creates everything
        when(userRepo.findByEmail(any())).thenReturn(Optional.empty());
        when(classRepo.findByCourseCodeAndTermAndSection(any(), any(), any())).thenReturn(Optional.empty());

        Answer<Object> assignId = invocation -> {
            Object arg = invocation.getArgument(0);
            try {
                arg.getClass().getMethod("setId", String.class).invoke(arg, UUID.randomUUID().toString());
            } catch (Exception ignored) { }
            return arg;
        };

        when(userRepo.save(any(User.class))).thenAnswer(assignId);
        when(classRepo.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(assignId);
        when(enrollmentRepo.save(any(Enrollment.class))).thenAnswer(assignId);
        when(labRepo.save(any(Lab.class))).thenAnswer(assignId);
        when(groupRepo.save(any(Group.class))).thenAnswer(assignId);
        when(helpRepo.save(any(HelpQueueItem.class))).thenAnswer(assignId);
        when(signoffRepo.save(any(SignoffEvent.class))).thenAnswer(assignId);

        DataSeeder seeder = new DataSeeder();
        CommandLineRunner runner = seeder.initDatabase(userRepo, classRepo, enrollmentRepo, labRepo, groupRepo, helpRepo, signoffRepo);

        runner.run(new String[]{});

        verify(helpRepo).deleteAll();
        verify(signoffRepo).deleteAll();
        verify(groupRepo).deleteAll();
        verify(labRepo).deleteAll();
        verify(enrollmentRepo, atLeastOnce()).save(any());
        verify(classRepo).save(any(com.example.lab_signoff_backend.model.Class.class));
        verify(userRepo, atLeastOnce()).save(any(User.class));
    }
}
