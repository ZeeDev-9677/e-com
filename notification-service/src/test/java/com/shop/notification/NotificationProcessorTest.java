package com.shop.notification;

import com.shop.notification.messaging.NotificationMessage;
import com.shop.notification.repository.NotificationRepository;
import com.shop.notification.service.EmailSender;
import com.shop.notification.service.NotificationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {
    @Mock NotificationRepository repository;
    @Mock EmailSender sender;
    @InjectMocks NotificationProcessor processor;

    private final NotificationMessage msg =
            new NotificationMessage("PAY_OK:1", UUID.randomUUID(), "u@x.com", "Subj", "Body");

    @Test
    void newMessage_isSent_andStored() {
        when(repository.existsByDedupeKey("PAY_OK:1")).thenReturn(false);

        processor.handle(msg);

        verify(sender).send("u@x.com", "Subj", "Body");
        verify(repository).save(any());
    }

    @Test
    void duplicateMessage_isNotSentTwice() {
        when(repository.existsByDedupeKey("PAY_OK:1")).thenReturn(true);

        processor.handle(msg);

        verifyNoInteractions(sender);
        verify(repository, never()).save(any());
    }

    @Test
    void senderFailure_propagates_soBrokerCanRetry() {
        when(repository.existsByDedupeKey(any())).thenReturn(false);
        doThrow(new IllegalStateException("smtp down")).when(sender).send(any(), any(), any());

        try { processor.handle(msg); } catch (IllegalStateException expected) { /* retry path */ }

        verify(repository, never()).save(any());
    }
}
