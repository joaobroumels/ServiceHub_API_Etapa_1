package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Avaliações", description = "Notas de 1 a 5 para ordens concluídas, uma avaliação por ordem")
public class ReviewController {
    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Avaliar ordem concluída", description = "Exige CLIENT proprietário da ordem COMPLETED. Avaliação duplicada retorna 409")
    public ReviewResponse create(@Valid @RequestBody ReviewRequest request) { return reviews.create(request); }

    @GetMapping("/api/services/{serviceId}/reviews")
    @Operation(summary = "Listar avaliações de um serviço", description = "Consulta pública paginada, sem dados pessoais dos clientes")
    public PageResponse<ReviewResponse> findByService(@PathVariable Long serviceId, @PageableDefault(size = 20, sort = "id") Pageable pageable) { return reviews.findByService(serviceId, pageable); }
}
