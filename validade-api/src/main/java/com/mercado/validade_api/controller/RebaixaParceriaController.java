package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.RebaixaParceriaItemDTO;
import com.mercado.validade_api.service.RebaixaParceriaService;
import com.mercado.validade_api.service.RebaixaParceriaService.Responsavel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rebaixa-parceria")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RebaixaParceriaController {

    private final RebaixaParceriaService service;

    /** Cobertura de rebaixa parceria pendente, agrupada por produto. */
    @GetMapping
    public List<RebaixaParceriaItemDTO> listar() {
        return service.listarPendentes();
    }

    /** Lança a cobertura de um produto. body: {ean, responsavel: FORNECEDOR|MERCADO, fornecedorId?} */
    @PostMapping("/lancar")
    public Map<String, String> lancar(@RequestBody Map<String, Object> body) {
        String ean = (String) body.get("ean");
        Responsavel responsavel = Responsavel.valueOf(
                body.getOrDefault("responsavel", "FORNECEDOR").toString());
        Long fornecedorId = body.get("fornecedorId") != null
                ? Long.valueOf(body.get("fornecedorId").toString())
                : null;
        return Map.of("mensagem", service.lancar(ean, responsavel, fornecedorId, "Web"));
    }
}
