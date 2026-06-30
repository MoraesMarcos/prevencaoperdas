package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class LancamentoMercadoDTO {
    private UUID id;
    private String tipo;
    private int sinal;
    private BigDecimal valor;
    private String descricao;
    private LocalDate data;
}
