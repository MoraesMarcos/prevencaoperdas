package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class LancamentoResponseDTO {
    private UUID id;
    private String tipo;
    private BigDecimal valor;        // sempre positivo
    private BigDecimal valorAbatido; // quanto já foi pago/abatido
    private BigDecimal restante;     // valor - valorAbatido
    private int sinal;               // +1 aumenta a dívida, -1 reduz
    private String ean;
    private String descricao;
    private String produtoNome;
    private String status;
    private LocalDateTime criadoEm;
}
