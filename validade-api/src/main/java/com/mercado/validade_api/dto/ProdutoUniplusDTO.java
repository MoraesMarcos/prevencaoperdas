package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProdutoUniplusDTO {
    private String codigo;   // codigo interno do Uniplus (produto.codigo)
    private String ean;      // codigo de barras
    private String nome;     // produto.nome
    private String grupo;    // hierarquia.nome (no lugar de "categoria")
}
