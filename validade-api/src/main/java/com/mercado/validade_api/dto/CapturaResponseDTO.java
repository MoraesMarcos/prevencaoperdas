package com.mercado.validade_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CapturaResponseDTO {

    private UUID loteId;
    private UUID produtoId;
    private String produtoNome;
    private String codigoBarras;
    private String numeroLote;
    private Integer quantidadeAtual;
    private LocalDate dataVencimento;

    // Semáforo de validade
    private long diasParaVencer;
    private String status; // NORMAL | ATENCAO | CRITICO

    private List<String> fotos;
}
