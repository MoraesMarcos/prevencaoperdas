package com.mercado.validade_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Marca, por produto (EAN), até quando as vendas de REBAIXA PARCERIA (promocao=2)
 * do Uniplus já foram lançadas em alguma conta. Evita contar a mesma venda duas vezes:
 * o "pendente" é sempre o que vendeu DEPOIS de processadoAte.
 */
@Entity
@Table(name = "rebaixa_parceria_marcador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RebaixaParceriaMarcador {

    @Id
    @Column(name = "ean", length = 20)
    private String ean;

    @Column(name = "produto_nome")
    private String produtoNome;

    // Última venda de parceria já lançada. Pendente = vendas com horafinal > processadoAte.
    @Column(name = "processado_ate", nullable = false)
    private LocalDateTime processadoAte;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
