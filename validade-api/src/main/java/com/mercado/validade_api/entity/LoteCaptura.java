package com.mercado.validade_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lotes_captura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteCaptura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK produto_id -> produtos(id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(name = "numero_lote", length = 100, nullable = false)
    private String numeroLote;

    @Column(name = "quantidade_inicial", nullable = false)
    private Integer quantidadeInicial;

    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "nota_fiscal", length = 100)
    private String notaFiscal;

    // Coluna UUID solta (tabela de fornecedores ainda não existe)
    @Column(name = "fornecedor_id")
    private UUID fornecedorId;

    @Column(name = "criado_por", nullable = false)
    private String criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
