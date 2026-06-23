package com.mercado.bi_api.controller;

import com.mercado.bi_api.dto.BootstrapAdminRequestDTO;
import com.mercado.bi_api.dto.LoginRequestDTO;
import com.mercado.bi_api.dto.LoginResponseDTO;
import com.mercado.bi_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/bootstrap-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public void bootstrapAdmin(@Valid @RequestBody BootstrapAdminRequestDTO dto) {
        authService.bootstrapAdmin(dto);
    }
}
