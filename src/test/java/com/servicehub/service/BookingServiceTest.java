package com.servicehub.service;

import com.servicehub.dto.CreateServiceRequest;
import com.servicehub.dto.StatusUpdateRequest;
import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.*;
import com.servicehub.repository.*;
import com.servicehub.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock ServiceRequestRepository requests;
    @Mock ServiceOfferingRepository offerings;
    @Mock UserRepository users;
    @Mock CurrentUser currentUser;
    BookingService service;
    User client;
    User provider;
    ServiceOffering offering;

    @BeforeEach
    void setUp() {
        service = new BookingService(requests, offerings, users, currentUser, new InputSanitizer());
        client = user(1L, UserType.CLIENT);
        provider = user(2L, UserType.PROVIDER);
        offering = new ServiceOffering();
        ReflectionTestUtils.setField(offering, "id", 3L);
        offering.setTitle("Limpeza");
        offering.setProvider(provider);
        offering.setPrice(new BigDecimal("150.00"));
    }

    @Test
    void shouldUseAuthenticatedClientSnapshotPriceAndSanitizeNotes() {
        when(currentUser.id()).thenReturn(client.getId());
        when(offerings.findProviderId(3L)).thenReturn(Optional.of(provider.getId()));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(client));
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(provider));
        when(offerings.findByIdForUpdate(3L)).thenReturn(Optional.of(offering));
        when(requests.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime date = LocalDateTime.now().plusDays(7);

        var response = service.create(new CreateServiceRequest(3L, date, "<b>Atendimento</b><script>alert(1)</script>"));

        ArgumentCaptor<ServiceRequest> captured = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(requests).saveAndFlush(captured.capture());
        assertThat(captured.getValue().getClient()).isSameAs(client);
        assertThat(captured.getValue().getService()).isSameAs(offering);
        assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
        assertThat(response.totalPrice()).isEqualByComparingTo("150.00");
        assertThat(response.notes()).isEqualTo("Atendimento");
        assertThat(response.scheduledAt()).isEqualTo(date);
    }

    @Test
    void shouldRejectBookingOwnServiceWithoutPersistingAnything() {
        when(currentUser.id()).thenReturn(provider.getId());
        when(offerings.findProviderId(3L)).thenReturn(Optional.of(provider.getId()));
        assertThatThrownBy(() -> service.create(new CreateServiceRequest(3L, LocalDateTime.now().plusDays(7), null)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(requests);
    }

    @Test
    void shouldBlockClientFromAcceptingOrderButAllowCancellation() {
        ServiceRequest booking = booking(RequestStatus.PENDING);
        when(currentUser.id()).thenReturn(client.getId());
        when(requests.findByIdForUpdate(4L)).thenReturn(Optional.of(booking));
        assertThatThrownBy(() -> service.changeStatus(4L, new StatusUpdateRequest(RequestStatus.ACCEPTED)))
                .isInstanceOf(AccessDeniedException.class);
        verify(requests, never()).saveAndFlush(any());

        when(requests.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.changeStatus(4L, new StatusUpdateRequest(RequestStatus.CANCELLED)).status())
                .isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void shouldRejectTransitionsFromCompletedAndReturnNotFoundForUnknownOrder() {
        when(currentUser.id()).thenReturn(provider.getId());
        when(requests.findByIdForUpdate(4L)).thenReturn(Optional.of(booking(RequestStatus.COMPLETED)));
        assertThatThrownBy(() -> service.changeStatus(4L, new StatusUpdateRequest(RequestStatus.ACCEPTED)))
                .isInstanceOf(BusinessRuleException.class);
        when(requests.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus(99L, new StatusUpdateRequest(RequestStatus.ACCEPTED)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(requests, never()).saveAndFlush(any());
    }

    private User user(long id, UserType role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUserType(role);
        user.setFullName("Pessoa " + id);
        return user;
    }

    private ServiceRequest booking(RequestStatus status) {
        ServiceRequest booking = new ServiceRequest();
        ReflectionTestUtils.setField(booking, "id", 4L);
        booking.setClient(client);
        booking.setService(offering);
        booking.setStatus(status);
        booking.setTotalPrice(offering.getPrice());
        return booking;
    }
}
