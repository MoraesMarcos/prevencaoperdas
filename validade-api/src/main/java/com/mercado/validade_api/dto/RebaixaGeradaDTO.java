package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class RebaixaGeradaDTO {
    private boolean criada;
    private String fornecedorNome;
    private String produtoNome;
    private BigDecimal valor;
    private String mensagem;
}
