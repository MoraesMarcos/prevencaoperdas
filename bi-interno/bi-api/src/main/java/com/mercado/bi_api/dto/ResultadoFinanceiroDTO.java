package com.mercado.bi_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ResultadoFinanceiroDTO {
    private BigDecimal faturamentoLiquido;
    private BigDecimal cmvReal;
    private BigDecimal resultadoBruto;
    private BigDecimal totalDespesas;
    private BigDecimal resultadoLiquido;
    private BigDecimal percentualDespesasSobreFaturamento;
    private BigDecimal limiteIdealPercentual;
    private BigDecimal tetoPercentual;
}
