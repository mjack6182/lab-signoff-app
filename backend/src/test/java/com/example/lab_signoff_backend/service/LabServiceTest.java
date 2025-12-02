package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
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
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

    @InjectMocks
    private LabService labService;

    @Test
    void getByJoinCode_trimsAndDelegates() {
        Lab lab = new Lab();
        when(labRepository.findByJoinCodeIgnoreCase("ABC123")).thenReturn(Optional.of(lab));

        Optional<Lab> result = labService.getByJoinCode("  ABC123 ");

        assertTrue(result.isPresent());
        assertEquals(lab, result.get());
    }

    @Test
    void getByJoinCode_whenNullReturnsEmpty() {
        assertTrue(labService.getByJoinCode(null).isEmpty());
    }

    @Test
    void getById_whenNullReturnsEmpty() {
        assertTrue(labService.getById(null).isEmpty());
    }

    @Test
    void upsert_andGetAll_delegatesToRepository() {
        Lab lab = new Lab();
        when(labRepository.save(any(Lab.class))).thenReturn(lab);
        when(labRepository.findAll()).thenReturn(List.of(lab));

        Lab saved = labService.upsert(lab);
        List<Lab> labs = labService.getAll();

        assertEquals(lab, saved);
        assertEquals(1, labs.size());
    }

    @Test
    void labExists_usesRepository() {
        when(labRepository.existsById("lab-1")).thenReturn(true);

        assertTrue(labService.labExists("lab-1"));
    }
}
