package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Um lote com suas fotos, para a galeria de estoque agrupada por grupo. */
@Data
@Builder
@AllArgsConstructor
public class EstoqueFotoDTO {
    private UUID loteId;
    private String produtoNome;
    private String grupo;
    private String numeroLote;
    private Integer quantidadeAtual;
    private LocalDate dataVencimento;
    private long diasParaVencer;
    private String status; // CRITICO | ATENCAO | NORMAL | VENCIDO
    private List<String> fotos;
}
