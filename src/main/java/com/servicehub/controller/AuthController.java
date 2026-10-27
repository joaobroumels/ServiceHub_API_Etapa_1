package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro público e login JWT stateless")
@SecurityRequirements
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/register")
    @Operation(summary = "Cadastrar cliente ou prestador", description = "ADMIN não pode ser criado por cadastro público")
    @ApiResponse(responseCode = "201", description = "Cadastro realizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou papel administrativo proibido")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }
    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida e-mail/senha e retorna JWT Bearer com duração configurada")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas ou conta inativa")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request));
    }
}
