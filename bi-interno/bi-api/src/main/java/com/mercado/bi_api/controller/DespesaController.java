package com.mercado.bi_api.controller;

import com.mercado.bi_api.dto.DespesaRequestDTO;
import com.mercado.bi_api.dto.DespesaResponseDTO;
import com.mercado.bi_api.dto.SugestaoCategoriaResponseDTO;
import com.mercado.bi_api.entity.CategoriaDespesa;
import com.mercado.bi_api.service.CategorizacaoService;
import com.mercado.bi_api.service.DespesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
public class DespesaController {

    private final DespesaService despesaService;
    private final CategorizacaoService categorizacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DespesaResponseDTO registrar(@Valid @RequestBody DespesaRequestDTO dto) {
        return despesaService.registrar(dto);
    }

    @GetMapping
    public List<DespesaResponseDTO> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return despesaService.listarPorPeriodo(inicio, fim);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        despesaService.remover(id);
    }

    // Usado pelo front enquanto a pessoa digita a descricao, para sugerir a categoria.
    @GetMapping("/sugestao-categoria")
    public SugestaoCategoriaResponseDTO sugerirCategoria(@RequestParam String descricao) {
        return categorizacaoService.sugerir(descricao)
                .map(c -> SugestaoCategoriaResponseDTO.builder()
                        .encontrada(true)
                        .categoriaId(c.getId())
                        .categoriaNome(c.getNome())
                        .build())
                .orElseGet(() -> SugestaoCategoriaResponseDTO.builder().encontrada(false).build());
    }
}
