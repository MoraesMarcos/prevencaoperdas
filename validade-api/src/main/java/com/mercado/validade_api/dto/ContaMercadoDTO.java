package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ContaMercadoDTO {
    private BigDecimal saldo;                 // bonificações + créditos manuais − usos
    private BigDecimal totalBonificacoes;     // crédito vindo do Uniplus (1910/1910a)
    private BigDecimal totalUsos;             // avarias, troca de preço, outros
    private List<BonificacaoDTO> bonificacoes;
    private List<LancamentoMercadoDTO> lancamentos;
}
