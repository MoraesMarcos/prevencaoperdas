package com.mercado.validade_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_barras", length = 13, nullable = false, unique = true)
    private String codigoBarras;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "marca")
    private String marca;

    @Column(name = "categoria", length = 100)
    private String categoria;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
