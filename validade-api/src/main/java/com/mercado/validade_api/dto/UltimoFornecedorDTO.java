package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Último fornecedor que trouxe um produto + custo unitário (para a rebaixa automática). */
@Data
@Builder
@AllArgsConstructor
public class UltimoFornecedorDTO {
    private Long fornecedorId;
    private String fornecedorNome;
    private String whatsapp;
    private BigDecimal custoUnitario;
}
