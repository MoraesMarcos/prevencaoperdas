package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FornecedorDTO {
    private Long id;        // Uniplus entidade.id
    private String nome;
    private String cnpjcpf;
    private String whatsapp;
    private String email;
}
