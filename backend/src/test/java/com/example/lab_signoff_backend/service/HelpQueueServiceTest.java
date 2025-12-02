package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.HelpQueueItem;
import com.example.lab_signoff_backend.model.enums.HelpQueueStatus;
import com.example.lab_signoff_backend.repository.HelpQueueItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpQueueServiceTest {

    @Mock
    private HelpQueueItemRepository repository;

    @InjectMocks
    private HelpQueueService service;

    @Test
    void raiseHand_assignsNextPositionAndPersistsDescription() {
        HelpQueueItem existing = new HelpQueueItem("lab-1", "group-old", "student-a", 1);
        when(repository.findByLabIdAndStatusIn(eq("lab-1"), any())).thenReturn(List.of(existing));

        HelpQueueItem last = new HelpQueueItem("lab-1", "group-last", "student-b", 2);
        last.setPosition(2);
        when(repository.findFirstByLabIdOrderByPositionDesc("lab-1")).thenReturn(Optional.of(last));
        when(repository.save(any(HelpQueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpQueueItem saved = service.raiseHand("lab-1", "group-new", "student-new", "Need guidance");

        ArgumentCaptor<HelpQueueItem> captor = ArgumentCaptor.forClass(HelpQueueItem.class);
        verify(repository).save(captor.capture());

        HelpQueueItem captured = captor.getValue();
        assertEquals(3, captured.getPosition()); // next after last
        assertEquals("Need guidance", captured.getDescription());
        assertEquals(HelpQueueStatus.WAITING, captured.getStatus());
        assertEquals(saved.getGroupId(), captured.getGroupId());
    }

    @Test
    void raiseHand_whenGroupAlreadyActive_throws() {
        HelpQueueItem active = new HelpQueueItem("lab-1", "group-1", "student", 1);
        when(repository.findByLabIdAndStatusIn(eq("lab-1"), any())).thenReturn(List.of(active));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.raiseHand("lab-1", "group-1", "student", "help"));

        assertTrue(ex.getMessage().contains("active help request"));
        verify(repository, never()).save(any());
    }

    @Test
    void claimRequest_transitionsWaitingToClaimed() {
        HelpQueueItem queueItem = new HelpQueueItem("lab-1", "group-1", "student", 1);
        when(repository.findById("item-1")).thenReturn(Optional.of(queueItem));
        when(repository.save(any(HelpQueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpQueueItem updated = service.claimRequest("item-1", "ta-1");

        assertEquals(HelpQueueStatus.CLAIMED, updated.getStatus());
        assertEquals("ta-1", updated.getClaimedBy());
        verify(repository).save(queueItem);
    }

    @Test
    void resolveRequest_whenNotClaimed_throws() {
        HelpQueueItem queueItem = new HelpQueueItem("lab-1", "group-1", "student", 1);
        when(repository.findById("item-2")).thenReturn(Optional.of(queueItem));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveRequest("item-2"));

        assertTrue(ex.getMessage().contains("claimed"));
        verify(repository, never()).save(any());
    }

    @Test
    void resolveRequest_transitionsClaimedToResolved() {
        HelpQueueItem claimed = new HelpQueueItem("lab-1", "group-1", "student", 1);
        claimed.claim("ta-1");
        when(repository.findById("item-3")).thenReturn(Optional.of(claimed));
        when(repository.save(any(HelpQueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpQueueItem resolved = service.resolveRequest("item-3");

        assertEquals(HelpQueueStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    void cancelRequest_marksCancelledWhenActive() {
        HelpQueueItem waiting = new HelpQueueItem("lab-1", "group-1", "student", 1);
        when(repository.findById("item-4")).thenReturn(Optional.of(waiting));
        when(repository.save(any(HelpQueueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HelpQueueItem cancelled = service.cancelRequest("item-4");

        assertEquals(HelpQueueStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.isCancelled());
    }

    @Test
    void hasActiveRequest_checksWaitingOrClaimed() {
        HelpQueueItem waiting = new HelpQueueItem("lab-1", "group-1", "student", 1);
        when(repository.findByLabIdAndStatusIn(eq("lab-1"), any())).thenReturn(List.of(waiting));

        assertTrue(service.hasActiveRequest("lab-1", "group-1"));
        assertFalse(service.hasActiveRequest("lab-1", "other-group"));
    }

    @Test
    void countActiveItems_addsWaitingAndClaimed() {
        when(repository.countByLabIdAndStatus("lab-1", HelpQueueStatus.WAITING)).thenReturn(2L);
        when(repository.countByLabIdAndStatus("lab-1", HelpQueueStatus.CLAIMED)).thenReturn(3L);

        long total = service.countActiveItems("lab-1");

        assertEquals(5L, total);
        verify(repository).countByLabIdAndStatus("lab-1", HelpQueueStatus.WAITING);
        verify(repository).countByLabIdAndStatus("lab-1", HelpQueueStatus.CLAIMED);
    }
}
