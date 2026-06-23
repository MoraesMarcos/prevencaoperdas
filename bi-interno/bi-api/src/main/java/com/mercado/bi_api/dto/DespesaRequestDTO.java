package com.mercado.bi_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class DespesaRequestDTO {

    @NotBlank
    private String descricao;

    @NotNull
    @Positive
    private BigDecimal valor;

    @NotNull
    private LocalDate data;

    @NotNull
    private UUID categoriaId;

    private boolean recorrente;

    @NotBlank
    private String criadoPor;
}
