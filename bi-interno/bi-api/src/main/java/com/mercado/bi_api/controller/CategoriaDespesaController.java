package com.mercado.bi_api.controller;

import com.mercado.bi_api.dto.CategoriaDespesaDTO;
import com.mercado.bi_api.entity.CategoriaDespesa;
import com.mercado.bi_api.repository.CategoriaDespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-despesa")
@RequiredArgsConstructor
public class CategoriaDespesaController {

    private final CategoriaDespesaRepository categoriaDespesaRepository;

    @GetMapping
    public List<CategoriaDespesaDTO> listar() {
        return categoriaDespesaRepository.findAll().stream()
                .map(c -> CategoriaDespesaDTO.builder()
                        .id(c.getId())
                        .nome(c.getNome())
                        .centroCusto(c.getCentroCusto())
                        .palavrasChave(c.getPalavrasChave())
                        .margemIdealPercentual(c.getMargemIdealPercentual())
                        .build())
                .toList();
    }
}
