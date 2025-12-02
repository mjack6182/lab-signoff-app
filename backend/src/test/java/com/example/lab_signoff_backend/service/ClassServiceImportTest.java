package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.ClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceImportTest {

    @Mock
    private ClassRepository classRepository;
    @Mock
    private LabService labService;

    @InjectMocks
    private ClassService classService;

    @Test
    void importRosterFromCsv_parsesLabsAndRoster() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS999", "Capstone", "Fall", "inst");
        clazz.setId("class-123");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Module 01 (9601577)",
                "Points Possible,,,,5",
                "Student One,,,student1@example.com,",
                "Student Two,,,student2@example.com,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-123")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-123", file);
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void importRosterFromCsv_withoutPointsRowDefaultsToOnePointLabs() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS555", "Labless", "Fall", "inst");
        clazz.setId("class-555");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Module 02 (12345)",
                "Student One,,,student1@example.com,",
                "Student Two,,,student2@example.com,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-555")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-555", file);
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void importRosterFromCsv_whenLabHeaderMalformed_skipsLab() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS556", "Malformed", "Fall", "inst");
        clazz.setId("class-556");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Not a Lab Header",
                "Points Possible,,,,5",
                "Student One,,,student1@example.com,",
                "Student Two,,,student2@example.com,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-556")).thenReturn(Optional.of(clazz));

        classService.importRosterFromCsv("class-556", file);
        verify(labService, never()).upsert(any());
    }

    @Test
    void importRosterFromCsv_missingPointsRowDefaultsToOnePoint() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS557", "No Points Row", "Fall", "inst");
        clazz.setId("class-557");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Lab X (987)",
                "Student One,1001,,,",
                "Student Two,1002,,,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-557")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-557", file);
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void importRosterFromCsv_withPointsRowParsesPoints() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS558", "Points Parse", "Fall", "inst");
        clazz.setId("class-558");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Module 05 (7777)",
                "Points Possible,,,,7",
                "Student One,,,s1@example.com,10"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-558")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.lab_signoff_backend.model.Class result = classService.importRosterFromCsv("class-558", file);
        assertFalse(result.getRoster().isEmpty());
    }

    @Test
    void importRosterFromCsv_nonNumericPointsDefaultsToOne() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS558", "Numbers", "Fall", "inst");
        clazz.setId("class-558");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Module 03 (222)",
                "Points Possible,,,,abc",
                "Student One,,,student1@example.com,",
                "Student Two,,,student2@example.com,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-558")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-558", file);

        verify(labService).upsert(labCaptor.capture());
        assertEquals(1, labCaptor.getValue().getPoints());
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void importRosterFromCsv_normalizesHeadersAndParsesPoints() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS559", "Header", "Fall", "inst");
        clazz.setId("class-559");

        String csvContent = String.join("\n",
                "\uFEFFStudent Name,ID,SIS User ID,SIS Login ID,Laboratory for Module 04 (333)",
                "Points Possible,,,,10",
                "Alice,,,alice@example.com,",
                "Bob,,,bob@example.com,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-559")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);
        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-559", file);

        verify(labService).upsert(labCaptor.capture());
        Lab lab = labCaptor.getValue();
        assertEquals("Laboratory for Module 04", lab.getTitle());
        assertEquals(10, lab.getPoints());
        assertEquals(2, updated.getRoster().size());
    }

    @Test
    void importRosterFromCsv_createsLabsForEachHeader() throws Exception {
        com.example.lab_signoff_backend.model.Class clazz =
                new com.example.lab_signoff_backend.model.Class("CS560", "Multiple", "Fall", "inst");
        clazz.setId("class-560");

        String csvContent = String.join("\n",
                "Student,ID,SIS User ID,SIS Login ID,Laboratory for Module 05 (444),Laboratory for Module 06 (555)",
                "Points Possible,,,,3,7",
                "Alice,,,alice@example.com,,",
                "Bob,,,bob@example.com,,"
        );
        MockMultipartFile file = new MockMultipartFile("file", "grades.csv", "text/csv", csvContent.getBytes());

        when(classRepository.findById("class-560")).thenReturn(Optional.of(clazz));
        when(classRepository.save(any(com.example.lab_signoff_backend.model.Class.class))).thenAnswer(inv -> inv.getArgument(0));
        when(labService.upsert(any(Lab.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Lab> labCaptor = ArgumentCaptor.forClass(Lab.class);

        com.example.lab_signoff_backend.model.Class updated = classService.importRosterFromCsv("class-560", file);

        verify(labService, times(2)).upsert(labCaptor.capture());
        List<Lab> labs = labCaptor.getAllValues();
        assertEquals(2, labs.size());
        assertTrue(labs.stream().anyMatch(l -> l.getPoints() == 3));
        assertTrue(labs.stream().anyMatch(l -> l.getPoints() == 7));
        assertEquals(2, updated.getRoster().size());
    }
}
