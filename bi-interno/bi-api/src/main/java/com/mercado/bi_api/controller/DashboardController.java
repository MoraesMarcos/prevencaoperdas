package com.mercado.bi_api.controller;

import com.mercado.bi_api.dto.ResultadoFinanceiroDTO;
import com.mercado.bi_api.service.RelatorioFinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RelatorioFinanceiroService relatorioFinanceiroService;

    @GetMapping("/resultado-financeiro")
    public ResultadoFinanceiroDTO resultadoFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String filial) {
        return relatorioFinanceiroService.calcular(dataInicio, dataFim, filial);
    }
}
