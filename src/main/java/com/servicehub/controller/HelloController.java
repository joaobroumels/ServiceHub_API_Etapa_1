package com.servicehub.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping
    @Operation(summary = "Verificar API", description = "Confirma que o servidor ServiceHub está funcionando")
    public Map<String, String> hello() {
        return Map.of("message", "ServiceHub API está funcionando");
    }
}
