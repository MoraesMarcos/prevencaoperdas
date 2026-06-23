package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class ContaCorrenteResumoDTO {
    private Long fornecedorId;
    private String fornecedorNome;
    private String whatsapp;
    private BigDecimal saldoAtivo;   // o que ainda falta o fornecedor acertar
    private int trocasAtivas;
}
