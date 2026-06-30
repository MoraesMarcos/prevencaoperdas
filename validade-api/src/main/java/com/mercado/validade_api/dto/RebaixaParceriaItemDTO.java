package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma linha da tela "Rebaixa Parceria": cobertura pendente de um produto,
 * agrupada (Σ (precobruto-precoliquido)×qtd das vendas promocao=2 ainda não lançadas),
 * com o fornecedor responsável já resolvido pelo Uniplus.
 */
@Data
@Builder
@AllArgsConstructor
public class RebaixaParceriaItemDTO {
    private String ean;
    private String produtoNome;
    private Long fornecedorId;       // null = sem fornecedor identificado no Uniplus
    private String fornecedorNome;
    private BigDecimal quantidade;   // unidades vendidas na parceria (pendentes)
    private BigDecimal coberturaPendente; // valor a lançar
    private LocalDateTime ultimaVenda;
}
