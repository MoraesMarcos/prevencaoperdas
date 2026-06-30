package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.AcompanhamentoResponseDTO;
import com.mercado.validade_api.dto.RebaixaGeradaDTO;
import com.mercado.validade_api.service.AcompanhamentoService;
import com.mercado.validade_api.service.RebaixaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/acompanhamento")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // MVP: liberar app/web. Em produção, restringir a origem.
public class AcompanhamentoController {

    private final AcompanhamentoService acompanhamentoService;
    private final RebaixaService rebaixaService;

    @GetMapping
    public List<AcompanhamentoResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean incluirNovos) {
        return acompanhamentoService.listar(incluirNovos);
    }

    /**
     * Gera a rebaixa na conta do fornecedor a partir de um lote.
     * body opcional: {fornecedorId} — quando o produto não tem fornecedor no Uniplus
     * (ou o operador quer escolher outro), passa o fornecedor escolhido.
     */
    @PostMapping("/{loteId}/gerar-rebaixa")
    public RebaixaGeradaDTO gerarRebaixa(@PathVariable UUID loteId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Long fornecedorId = body != null && body.get("fornecedorId") != null
                ? Long.valueOf(body.get("fornecedorId").toString())
                : null;
        return rebaixaService.gerarDoLote(loteId, fornecedorId);
    }
}
