package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uma entrada (nota fiscal) de um produto, com o fornecedor que o trouxe. */
@Data
@Builder
@AllArgsConstructor
public class FornecedorEntradaDTO {
    private LocalDateTime data;
    private String danfe;
    private Long fornecedorId;
    private String fornecedorNome;
    private String cnpjcpf;
    private String whatsapp;
    private BigDecimal quantidade;
    private BigDecimal valorTotal;
}
