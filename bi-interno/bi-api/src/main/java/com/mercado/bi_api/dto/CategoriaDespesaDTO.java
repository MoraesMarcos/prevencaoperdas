package com.mercado.bi_api.dto;

import com.mercado.bi_api.entity.CentroCusto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CategoriaDespesaDTO {
    private UUID id;
    private String nome;
    private CentroCusto centroCusto;
    private String palavrasChave;
    private BigDecimal margemIdealPercentual;
}
