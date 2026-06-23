package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BuscaProdutoResponseDTO {
    private String termo;
    private boolean encontrado;
    // Normalmente 1 resultado (bip do EAN completo). Pode vir mais de um quando
    // a busca e por "ultimos 6 digitos" e ha colisao -- o app deixa escolher.
    private List<ProdutoUniplusDTO> produtos;
}
