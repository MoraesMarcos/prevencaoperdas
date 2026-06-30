package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.FornecedorDTO;
import com.mercado.validade_api.dto.ProdutoFornecedorDTO;
import com.mercado.validade_api.service.FornecedorUniplusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FornecedorController {

    private final FornecedorUniplusService fornecedorService;

    /** Busca fornecedores no Uniplus por nome/CNPJ (para adicionar lançamentos). */
    @GetMapping("/buscar")
    public List<FornecedorDTO> buscar(@RequestParam(defaultValue = "") String termo) {
        return fornecedorService.buscar(termo);
    }

    /** Produtos agregados ao fornecedor (do histórico de notas), com custo sugerido. */
    @GetMapping("/{id}/produtos")
    public List<ProdutoFornecedorDTO> produtos(@PathVariable Long id) {
        return fornecedorService.produtosDoFornecedor(id);
    }
}
