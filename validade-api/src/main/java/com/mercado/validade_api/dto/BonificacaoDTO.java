package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bonificação vinda do Uniplus (CFOP 1910/1910a) — crédito do mercado. */
@Data
@Builder
@AllArgsConstructor
public class BonificacaoDTO {
    private LocalDateTime data;
    private String danfe;
    private String fornecedor;
    private String produto;
    private BigDecimal quantidade;
    private BigDecimal valor;
}
