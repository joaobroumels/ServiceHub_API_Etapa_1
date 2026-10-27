package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.model.ServiceCategory;
import com.servicehub.service.ServiceOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Serviços", description = "Catálogo público e gerenciamento pelo prestador proprietário")
public class ServiceOfferingController {
    private final ServiceOfferingService service;
    public ServiceOfferingController(ServiceOfferingService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar serviços", description = "Catálogo paginado de serviços e prestadores ativos. Filtros opcionais por categoria e título")
    public PageResponse<ServiceOfferingResponse> findAll(@RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String search, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.findAll(category, search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar serviço", description = "Retorna um serviço ativo e apenas o perfil público do prestador")
    public ServiceOfferingResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar serviço", description = "Exige JWT de PROVIDER. O proprietário vem do token, nunca do JSON")
    public ServiceOfferingResponse create(@Valid @RequestBody ServiceOfferingRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar serviço", description = "Somente proprietário ou ADMIN. Não altera o preço de ordens já contratadas")
    public ServiceOfferingResponse update(@PathVariable Long id, @Valid @RequestBody ServiceOfferingRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir serviço", description = "Exclusão lógica pelo proprietário ou ADMIN. Ordens ativas impedem a exclusão")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
