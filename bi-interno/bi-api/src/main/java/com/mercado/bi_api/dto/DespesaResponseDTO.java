package com.mercado.bi_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class DespesaResponseDTO {
    private UUID id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate data;
    private String categoriaNome;
    private String centroCusto;
    private boolean recorrente;
    private String criadoPor;
}
