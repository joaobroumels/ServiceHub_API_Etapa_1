package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.ProviderService;
import com.servicehub.service.ServiceOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Prestadores", description = "Perfis públicos sem e-mail, telefone ou senha")
public class ProviderController {
    private final ProviderService providers;
    private final ServiceOfferingService services;
    public ProviderController(ProviderService providers, ServiceOfferingService services) { this.providers = providers; this.services = services; }

    @GetMapping
    @Operation(summary = "Listar prestadores", description = "Retorna página de prestadores ativos com id, nome, bio e avatar")
    public PageResponse<ProviderResponse> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) { return providers.findAll(pageable); }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prestador", description = "Retorna somente os dados públicos de um prestador ativo")
    public ProviderResponse findById(@PathVariable Long id) { return providers.findById(id); }

    @GetMapping("/{id}/services")
    @Operation(summary = "Listar serviços de um prestador", description = "Consulta paginada do catálogo público do prestador indicado")
    public PageResponse<ServiceOfferingResponse> services(@PathVariable Long id, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        providers.findById(id);
        return services.findByProvider(id, pageable);
    }
}
