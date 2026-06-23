package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.ProdutoUniplusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Busca produto na ERP Uniplus (somente leitura) pelo codigo de barras.
 * O EAN pode estar em produto.ean (principal) ou produtoean.ean (alternativos).
 * Grupo = hierarquia.nome.
 */
@Service
@RequiredArgsConstructor
public class ProdutoUniplusService {

    private static final int LIMITE = 10;

    private final @Qualifier("erpJdbcTemplate") JdbcTemplate erpJdbcTemplate;

    private static final RowMapper<ProdutoUniplusDTO> MAPPER = (rs, n) -> ProdutoUniplusDTO.builder()
            .codigo(rs.getString("codigo"))
            .ean(rs.getString("ean"))
            .nome(rs.getString("nome"))
            .grupo(rs.getString("grupo"))
            .build();

    /** Busca pelo EAN completo (bip do scanner). */
    public List<ProdutoUniplusDTO> buscarPorEan(String ean) {
        String sql = """
                SELECT DISTINCT p.codigo, p.ean, p.nome, COALESCE(h.nome, 'SEM GRUPO') AS grupo
                FROM produto p
                LEFT JOIN hierarquia h ON p.idhierarquia = h.id
                LEFT JOIN produtoean pe ON pe.idproduto = p.id
                WHERE p.ean = ? OR pe.ean = ?
                LIMIT ?
                """;
        return erpJdbcTemplate.query(sql, MAPPER, ean, ean, LIMITE);
    }

    /**
     * Busca pelos ultimos digitos (digitacao manual, ex: 6 digitos).
     * Usa UNION (em vez de OR sobre o JOIN) para evitar plano ruim no banco
     * de producao com o curinga inicial do LIKE.
     */
    public List<ProdutoUniplusDTO> buscarPorSufixo(String sufixo) {
        String like = "%" + sufixo;
        String sql = """
                SELECT codigo, ean, nome, grupo FROM (
                    SELECT p.codigo, p.ean, p.nome, COALESCE(h.nome, 'SEM GRUPO') AS grupo
                    FROM produto p
                    LEFT JOIN hierarquia h ON p.idhierarquia = h.id
                    WHERE p.ean LIKE ?
                    UNION
                    SELECT p.codigo, p.ean, p.nome, COALESCE(h.nome, 'SEM GRUPO') AS grupo
                    FROM produtoean pe
                    JOIN produto p ON p.id = pe.idproduto
                    LEFT JOIN hierarquia h ON p.idhierarquia = h.id
                    WHERE pe.ean LIKE ?
                ) x
                LIMIT ?
                """;
        return erpJdbcTemplate.query(sql, MAPPER, like, like, LIMITE);
    }
}
