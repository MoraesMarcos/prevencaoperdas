package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ContaCorrenteDetalheDTO {
    private Long fornecedorId;
    private String fornecedorNome;
    private String whatsapp;
    private String email;
    private BigDecimal saldoAtivo;
    private BigDecimal bonificacaoDisponivel;         // crédito de bonificação (1910) ainda disponível
    private List<LancamentoResponseDTO> ativas;       // lançamentos em aberto
    private List<LancamentoResponseDTO> finalizadas;  // histórico
}
