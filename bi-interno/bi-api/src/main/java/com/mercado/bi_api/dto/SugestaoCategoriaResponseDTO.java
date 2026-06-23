package com.mercado.bi_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SugestaoCategoriaResponseDTO {
    private boolean encontrada;
    private UUID categoriaId;
    private String categoriaNome;
}
