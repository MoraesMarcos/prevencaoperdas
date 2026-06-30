package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.GiroDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Calcula o giro (velocidade de venda) de um produto lendo as vendas reais
 * do Uniplus (somente leitura), pelo codigo de barras (EAN).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GiroService {

    private final @Qualifier("erpJdbcTemplate") JdbcTemplate erpJdbcTemplate;

    // Uma unica query traz 30d, 90d e desde-a-captura. A janela cobre o periodo
    // mais antigo entre 90 dias atras e a data de captura do lote (param ?2).
    private static final String SQL = """
            SELECT
                COALESCE(SUM(CASE WHEN o.horafinal >= now() - interval '30 days' THEN i.quantidade ELSE 0 END), 0) AS vendido_30d,
                COALESCE(SUM(CASE WHEN o.horafinal >= now() - interval '90 days' THEN i.quantidade ELSE 0 END), 0) AS vendido_90d,
                COALESCE(SUM(CASE WHEN o.horafinal >= CAST(? AS timestamp) THEN i.quantidade ELSE 0 END), 0) AS vendido_captura
            FROM produto p
            JOIN item i ON i.produto = p.codigo
            JOIN operacao o ON o.id = i.idoperacao
            WHERE p.ean = ?
              AND o.empresa = '1'
              AND o.tipo = 1
              AND i.cancelado = 0
              AND o.cancelado = 0
              AND o.horafinal >= LEAST(now() - interval '90 days', CAST(? AS timestamp))
            """;

    /** Mantem compatibilidade: sem data de captura, usa 90 dias atras como referencia. */
    public GiroDTO calcular(String ean) {
        return calcular(ean, java.time.LocalDate.now().minusDays(90));
    }

    /** Giro completo (30d, 90d e desde a captura) em UMA consulta ao Uniplus. */
    public GiroDTO calcular(String ean, java.time.LocalDate dataCaptura) {
        if (ean == null || ean.isBlank()) {
            return GiroDTO.builder().vendido30d(0).vendido90d(0).velocidade30(0).velocidade90(0).vendidoDesdeCaptura(0).build();
        }
        java.sql.Date dataSql = java.sql.Date.valueOf(
                dataCaptura != null ? dataCaptura : java.time.LocalDate.now().minusDays(90));
        try {
            return erpJdbcTemplate.queryForObject(SQL, (rs, n) -> {
                int v30 = rs.getInt("vendido_30d");
                int v90 = rs.getInt("vendido_90d");
                int vCap = rs.getInt("vendido_captura");
                return GiroDTO.builder()
                        .vendido30d(v30)
                        .vendido90d(v90)
                        .velocidade30(round2(v30 / 30.0))
                        .velocidade90(round2(v90 / 90.0))
                        .vendidoDesdeCaptura(vCap)
                        .build();
            }, dataSql, ean, dataSql);
        } catch (Exception e) {
            // Se o Uniplus estiver indisponivel, devolve giro zerado em vez de quebrar a tela.
            log.warn("Falha ao calcular giro EAN={}: {}", ean, e.getMessage());
            return GiroDTO.builder().vendido30d(0).vendido90d(0).velocidade30(0).velocidade90(0).vendidoDesdeCaptura(0).build();
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
