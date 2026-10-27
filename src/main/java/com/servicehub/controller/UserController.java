package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Usuários", description = "ADMIN gerencia usuários. Cada usuário consulta e edita somente o próprio perfil")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @PostMapping
    @Operation(summary = "Cadastrar usuário como ADMIN", description = "Cadastro público disponível em /api/auth/register")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse user = users.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }
    @GetMapping
    @Operation(summary = "Listar usuários ativos", description = "Somente ADMIN. Paginação com page e size (máximo 100)")
    public PageResponse<UserResponse> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return users.findAll(pageable);
    }
    @GetMapping("/me")
    @Operation(summary = "Consultar meu perfil", description = "Retorna o usuário autenticado sem senha ou hash")
    public UserResponse me() { return users.me(); }
    @GetMapping("/{id}")
    @Operation(summary = "Consultar perfil por ID", description = "Somente o titular ou ADMIN")
    public UserResponse findById(@PathVariable @Positive Long id) { return users.findById(id); }
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar perfil", description = "Somente o titular ou ADMIN. O papel é imutável; informe senha válida")
    public UserResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UserRequest request) {
        return users.update(id, request);
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar usuário", description = "Exclusão lógica. Bloqueia usuário com contratações em andamento e invalida JWT")
    @ApiResponse(responseCode = "204", description = "Conta desativada")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        users.delete(id);
        return ResponseEntity.noContent().build();
    }
}
