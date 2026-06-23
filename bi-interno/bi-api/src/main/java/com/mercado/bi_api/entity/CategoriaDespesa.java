package com.mercado.bi_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bi_categorias_despesa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "centro_custo", nullable = false, length = 20)
    private CentroCusto centroCusto;

    // Palavras-chave separadas por virgula, usadas pelo motor de sugestao
    // de categoria a partir da descricao digitada da despesa (ex: "gasolina,diesel,posto").
    @Column(name = "palavras_chave", columnDefinition = "TEXT")
    private String palavrasChave;

    @Column(name = "margem_ideal_percentual", precision = 5, scale = 2)
    private BigDecimal margemIdealPercentual;
}
