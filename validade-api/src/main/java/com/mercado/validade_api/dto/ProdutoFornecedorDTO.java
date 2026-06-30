package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Produto agregado a um fornecedor (vindo do histórico de notas), com último custo. */
@Data
@Builder
@AllArgsConstructor
public class ProdutoFornecedorDTO {
    private Long produtoId;
    private String ean;
    private String nome;
    private String grupo;
    private LocalDateTime ultimaEntrada;
    private BigDecimal ultimaQtd;
    private BigDecimal ultimoTotal;
    private BigDecimal custoUnitario; // valor sugerido para troca/rebaixa
}
