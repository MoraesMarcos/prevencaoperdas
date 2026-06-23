package com.mercado.bi_api.service;

import com.mercado.bi_api.dto.ResultadoFinanceiroDTO;
import com.mercado.bi_api.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    // Faixas definidas pelo dono do mercado para a saude das despesas sobre o faturamento.
    private static final BigDecimal LIMITE_IDEAL_PERCENTUAL = new BigDecimal("9.5");
    private static final BigDecimal TETO_PERCENTUAL = new BigDecimal("11");

    private final JdbcTemplate erpJdbcTemplate;
    private final DespesaRepository despesaRepository;

    private static final String SQL_BASE = """
            SELECT
                COALESCE(SUM(i.precoliquido), 0) AS faturamento_liquido,
                COALESCE(SUM(i.quantidade * i.sh_custo), 0) AS cmv_real
            FROM item i
            JOIN operacao o ON i.idoperacao = o.id
            WHERE o.horafinal >= ? AND o.horafinal <= ?
              AND o.empresa = '1'
              AND o.tipo = 1
              AND i.cancelado = 0
              AND o.cancelado = 0
            """;

    public ResultadoFinanceiroDTO calcular(LocalDate dataInicio, LocalDate dataFim, String filial) {
        LocalDateTime inicioTs = dataInicio.atStartOfDay();
        LocalDateTime fimTs = LocalDateTime.of(dataFim, LocalTime.of(23, 59, 59));

        String sql = SQL_BASE;
        Object[] params;
        if (filial != null && !filial.isBlank()) {
            sql += " AND o.filial = ?";
            params = new Object[]{inicioTs, fimTs, filial};
        } else {
            params = new Object[]{inicioTs, fimTs};
        }

        var linha = erpJdbcTemplate.queryForMap(sql, params);
        BigDecimal faturamentoLiquido = (BigDecimal) linha.getOrDefault("faturamento_liquido", BigDecimal.ZERO);
        BigDecimal cmvReal = (BigDecimal) linha.getOrDefault("cmv_real", BigDecimal.ZERO);
        BigDecimal resultadoBruto = faturamentoLiquido.subtract(cmvReal);

        BigDecimal totalDespesas = despesaRepository.findByDataBetweenOrderByDataDesc(dataInicio, dataFim)
                .stream()
                .map(d -> d.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultadoLiquido = resultadoBruto.subtract(totalDespesas);

        BigDecimal percentualDespesas = faturamentoLiquido.compareTo(BigDecimal.ZERO) > 0
                ? totalDespesas.multiply(BigDecimal.valueOf(100)).divide(faturamentoLiquido, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ResultadoFinanceiroDTO.builder()
                .faturamentoLiquido(faturamentoLiquido)
                .cmvReal(cmvReal)
                .resultadoBruto(resultadoBruto)
                .totalDespesas(totalDespesas)
                .resultadoLiquido(resultadoLiquido)
                .percentualDespesasSobreFaturamento(percentualDespesas)
                .limiteIdealPercentual(LIMITE_IDEAL_PERCENTUAL)
                .tetoPercentual(TETO_PERCENTUAL)
                .build();
    }
}
