package com.mercado.validade_api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CapturaRequestDTO {

    // Dados do produto (usados se o código de barras ainda não existir)
    private String codigoBarras;
    private String nome;
    private String marca;
    private String categoria;

    // Dados do lote
    private String numeroLote;
    private Integer quantidadeInicial;
    private LocalDate dataVencimento;
    private String notaFiscal;
    private UUID fornecedorId;
    private String criadoPor;

    // URLs das fotos (o app envia ao Supabase Storage e passa as URLs aqui)
    private List<String> fotosUrls;
}
