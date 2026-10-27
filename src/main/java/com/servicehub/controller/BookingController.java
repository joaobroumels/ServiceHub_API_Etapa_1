package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@Tag(name = "Ordens de serviço", description = "Contratação por CLIENT e execução pelo prestador, com máquina de estados")
public class BookingController {
    private final BookingService bookings;
    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Contratar serviço", description = "Exige CLIENT. Cliente, status PENDING e cópia do preço atual são definidos pelo servidor")
    public ServiceRequestResponse create(@Valid @RequestBody CreateServiceRequest request) { return bookings.create(request); }

    @GetMapping
    @Operation(summary = "Listar minhas ordens", description = "Cliente e prestador veem somente suas próprias ordens. ADMIN pode consultar todas")
    public PageResponse<ServiceRequestResponse> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) { return bookings.findAll(pageable); }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar ordem", description = "Somente cliente, prestador participante ou ADMIN")
    public ServiceRequestResponse findById(@PathVariable Long id) { return bookings.findById(id); }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status da ordem", description = "PENDING → ACCEPTED/CANCELLED; ACCEPTED → IN_PROGRESS/CANCELLED; IN_PROGRESS → COMPLETED. Cliente só cancela antes do início")
    public ServiceRequestResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) { return bookings.changeStatus(id, request); }
}
