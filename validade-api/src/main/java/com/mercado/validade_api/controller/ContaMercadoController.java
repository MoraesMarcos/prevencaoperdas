package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.ContaMercadoDTO;
import com.mercado.validade_api.dto.LancamentoMercadoDTO;
import com.mercado.validade_api.entity.MercadoTipo;
import com.mercado.validade_api.service.ContaMercadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conta-mercado")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContaMercadoController {

    private final ContaMercadoService contaMercadoService;

    /** Saldo do mercado + bonificações (Uniplus) + lançamentos manuais. */
    @GetMapping
    public ContaMercadoDTO conta(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        LocalDate i = inicio != null ? inicio : LocalDate.now().withDayOfYear(1);
        LocalDate f = fim != null ? fim : LocalDate.now();
        return contaMercadoService.conta(i, f);
    }

    @PostMapping("/lancamento")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoMercadoDTO adicionar(@RequestBody Map<String, Object> body) {
        MercadoTipo tipo = MercadoTipo.valueOf((String) body.get("tipo"));
        BigDecimal valor = new BigDecimal(body.get("valor").toString());
        String descricao = (String) body.get("descricao");
        LocalDate data = body.get("data") != null ? LocalDate.parse((String) body.get("data")) : null;
        return contaMercadoService.adicionar(tipo, valor, descricao, data, "Web");
    }

    @DeleteMapping("/lancamento/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID id) {
        contaMercadoService.remover(id);
    }
}
