package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

    @InjectMocks
    private LabService labService;

    @Test
    void getByJoinCode_nullReturnsEmptyAndSkipsRepo() {
        assertTrue(labService.getByJoinCode(null).isEmpty());
        verify(labRepository, never()).findByJoinCodeIgnoreCase(anyString());
    }

    @Test
    void getByJoinCode_trimsAndDelegates() {
        Lab lab = new Lab("c1", "Title", 1, "inst");
        when(labRepository.findByJoinCodeIgnoreCase("CODE")).thenReturn(Optional.of(lab));

        Optional<Lab> result = labService.getByJoinCode("  CODE  ");

        assertTrue(result.isPresent());
        verify(labRepository).findByJoinCodeIgnoreCase("CODE");
    }

    @Test
    void getById_nullReturnsEmptyAndSkipsRepo() {
        assertTrue(labService.getById(null).isEmpty());
        verify(labRepository, never()).findById(anyString());
    }

    @Test
    void getById_returnsValueWhenPresent() {
        Lab lab = new Lab("c1", "Title", 1, "inst");
        when(labRepository.findById("lab-1")).thenReturn(Optional.of(lab));

        Optional<Lab> result = labService.getById("lab-1");

        assertTrue(result.isPresent());
        verify(labRepository).findById("lab-1");
    }
}
