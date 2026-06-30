package com.mercado.validade_api.dto;

import com.mercado.validade_api.entity.LancamentoTipo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LancamentoRequestDTO {
    private Long fornecedorId;     // Uniplus entidade.id
    private LancamentoTipo tipo;   // TROCA | REBAIXA | AVARIA | NEGOCIACAO | PAGAMENTO
    private BigDecimal valor;
    private String ean;
    private String descricao;
    private String produtoNome;
    private String criadoPor;
}
