package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class AcompanhamentoResponseDTO {
    private UUID loteId;
    private String produtoNome;
    private String codigoBarras;
    private String grupo;
    private String numeroLote;
    private Integer quantidadeAtual;
    private LocalDate dataVencimento;
    private long diasParaVencer;
    private String status; // CRITICO | ATENCAO | NORMAL | VENCIDO

    // Giro (vendas reais do Uniplus)
    private int vendido30d;
    private int vendido90d;
    private double velocidade30; // un/dia
    private double velocidade90; // un/dia

    private Long diasParaEsgotar; // null = sem giro (não esgota)
    private String recomendacao;
    private String severidade; // OK | OBSERVAR | ATENCAO | CRITICO — controla a cor do selo
    private boolean rebaixaGerada; // já existe rebaixa lançada para este lote?

    // Estoque real: inicial lançado no app vs. vendido no Uniplus desde a captura
    private int vendidoDesdeCaptura;
    private int quantidadeRestante; // max(0, quantidadeInicial - vendidoDesdeCaptura)
}
