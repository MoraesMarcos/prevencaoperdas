package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma venda individual (linha crua, sem agregação) que compõe a cobertura
 * pendente de um produto na Rebaixa Parceria — para auditoria visual.
 */
@Data
@Builder
@AllArgsConstructor
public class RebaixaParceriaLinhaDTO {
    private LocalDateTime dataHora;
    private BigDecimal precoBruto;
    private BigDecimal precoLiquido;
    private BigDecimal quantidade;
    private BigDecimal descontoUnitario;
    private BigDecimal descontoLinha; // desconto_unitario * quantidade
}
