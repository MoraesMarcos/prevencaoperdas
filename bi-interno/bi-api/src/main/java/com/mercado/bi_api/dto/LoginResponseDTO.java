package com.mercado.bi_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String nome;
    private String email;
    private String papel;
}
