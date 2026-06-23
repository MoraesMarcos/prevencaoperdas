package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.ContaCorrenteDetalheDTO;
import com.mercado.validade_api.dto.ContaCorrenteResumoDTO;
import com.mercado.validade_api.dto.LancamentoRequestDTO;
import com.mercado.validade_api.dto.LancamentoResponseDTO;
import com.mercado.validade_api.service.ContaCorrenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conta-corrente")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContaCorrenteController {

    private final ContaCorrenteService contaCorrenteService;

    /** Fornecedores com conta corrente (algum lançamento) + saldo. */
    @GetMapping
    public List<ContaCorrenteResumoDTO> listar() {
        return contaCorrenteService.listarFornecedores();
    }

    /** Detalhe de um fornecedor: saldo + trocas ativas + histórico. */
    @GetMapping("/{fornecedorId}")
    public ContaCorrenteDetalheDTO detalhe(@PathVariable Long fornecedorId) {
        return contaCorrenteService.detalhe(fornecedorId);
    }

    @PostMapping("/lancamento")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponseDTO adicionar(@RequestBody LancamentoRequestDTO dto) {
        return contaCorrenteService.adicionar(dto);
    }

    @PostMapping("/lancamento/{id}/finalizar")
    public void finalizar(@PathVariable UUID id) {
        contaCorrenteService.finalizar(id);
    }

    @DeleteMapping("/lancamento/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        contaCorrenteService.remover(id);
    }
}
