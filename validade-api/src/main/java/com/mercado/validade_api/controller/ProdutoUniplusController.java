package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.BuscaProdutoResponseDTO;
import com.mercado.validade_api.dto.ProdutoUniplusDTO;
import com.mercado.validade_api.service.ProdutoUniplusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // MVP: liberar o app. Em produção, restringir a origem.
public class ProdutoUniplusController {

    private final ProdutoUniplusService produtoUniplusService;

    /**
     * Busca produto na ERP pelo codigo de barras.
     * Use ?ean=7891010101010 para o bip completo, ou ?ultimos=101010 para digitacao.
     */
    @GetMapping("/buscar")
    public BuscaProdutoResponseDTO buscar(@RequestParam(required = false) String ean,
                                          @RequestParam(required = false) String ultimos) {
        String termo;
        List<ProdutoUniplusDTO> produtos;

        if (ean != null && !ean.isBlank()) {
            termo = ean.trim();
            produtos = produtoUniplusService.buscarPorEan(termo);
        } else if (ultimos != null && !ultimos.isBlank()) {
            termo = ultimos.trim();
            produtos = produtoUniplusService.buscarPorSufixo(termo);
        } else {
            termo = "";
            produtos = List.of();
        }

        return BuscaProdutoResponseDTO.builder()
                .termo(termo)
                .encontrado(!produtos.isEmpty())
                .produtos(produtos)
                .build();
    }
}
