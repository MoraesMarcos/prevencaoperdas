package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.ContaCorrenteDetalheDTO;
import com.mercado.validade_api.dto.ContaCorrenteResumoDTO;
import com.mercado.validade_api.dto.LancamentoRequestDTO;
import com.mercado.validade_api.dto.LancamentoResponseDTO;
import com.mercado.validade_api.service.ContaCorrenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

    /** Abate um valor nos produtos em aberto (FIFO, mais antigo primeiro). */
    @PostMapping("/{fornecedorId}/abater")
    public ContaCorrenteDetalheDTO abater(@PathVariable Long fornecedorId,
                                          @RequestBody Map<String, BigDecimal> body) {
        return contaCorrenteService.abater(fornecedorId, body.get("valor"));
    }

    /** Abate as trocas usando o crédito de bonificação do fornecedor. valor null = usar tudo. */
    @PostMapping("/{fornecedorId}/abater-bonificacao")
    public ContaCorrenteDetalheDTO abaterComBonificacao(@PathVariable Long fornecedorId,
                                                        @RequestBody(required = false) Map<String, BigDecimal> body) {
        BigDecimal valor = body != null ? body.get("valor") : null;
        return contaCorrenteService.abaterComBonificacao(fornecedorId, valor);
    }

    /** Baixar histórico do fornecedor em CSV. */
    @GetMapping("/{fornecedorId}/historico.csv")
    public ResponseEntity<byte[]> historicoCsv(@PathVariable Long fornecedorId) {
        String csv = contaCorrenteService.historicoCsv(fornecedorId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historico_fornecedor_" + fornecedorId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
