package com.mercado.validade_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Lançamento manual da conta corrente do mercado (crédito ou uso de bonificação). */
@Entity
@Table(name = "mercado_conta_lancamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LancamentoMercado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private MercadoTipo tipo;

    @Column(name = "valor", nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao")
    private String descricao;

    // Lote de captura que originou este lançamento (rebaixa de validade). Evita duplicar.
    @Column(name = "lote_id")
    private UUID loteId;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "criado_por")
    private String criadoPor;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
