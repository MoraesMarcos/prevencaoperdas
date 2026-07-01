package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.CapturaRequestDTO;
import com.mercado.validade_api.dto.CapturaResponseDTO;
import com.mercado.validade_api.dto.LoteAbertoDTO;
import com.mercado.validade_api.service.CapturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/capturas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // MVP: liberar o front. Em produção, restringir a origem.
public class CapturaController {

    private final CapturaService capturaService;

    @PostMapping
    public ResponseEntity<CapturaResponseDTO> registrar(@RequestBody CapturaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(capturaService.registrar(dto));
    }

    @GetMapping
    public List<CapturaResponseDTO> listar() {
        return capturaService.listar();
    }

    /** Próximo número de lote sequencial do produto (para preencher automaticamente no app). */
    @GetMapping("/proximo-lote")
    public Map<String, Integer> proximoLote(@RequestParam String codigoBarras) {
        return Map.of("numeroLote", capturaService.proximoNumeroLote(codigoBarras));
    }

    /** Verifica se o produto já tem um lote em aberto (não totalmente vendido). */
    @GetMapping("/lote-aberto")
    public LoteAbertoDTO loteAberto(@RequestParam String codigoBarras) {
        return capturaService.loteAbertoDoProduto(codigoBarras);
    }
}
