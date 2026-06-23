package com.mercado.validade_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lançamento no livro-caixa (conta corrente) de um fornecedor.
 * O fornecedor vem do Uniplus (entidade.id); guardamos um snapshot do nome/whatsapp.
 */
@Entity
@Table(name = "fornecedor_conta_lancamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LancamentoFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fornecedor_id", nullable = false)
    private Long fornecedorId; // Uniplus entidade.id

    @Column(name = "fornecedor_nome", nullable = false)
    private String fornecedorNome;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private LancamentoTipo tipo;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "produto_nome")
    private String produtoNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private LancamentoStatus status;

    @Column(name = "criado_por")
    private String criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
