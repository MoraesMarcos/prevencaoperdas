package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Resposta de "existe lote em aberto?" para o produto — usada antes de cadastrar
 * um novo lote, para perguntar ao operador se ele realmente quer outro lote.
 */
@Data
@Builder
@AllArgsConstructor
public class LoteAbertoDTO {
    private boolean existeLoteAberto; // true = o último lote ainda não foi totalmente vendido
    private String numeroLote;
    private Integer quantidadeRestante;
    private LocalDate dataVencimento;
}
