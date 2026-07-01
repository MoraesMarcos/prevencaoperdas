package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.FornecedorDTO;
import com.mercado.validade_api.dto.ProdutoFornecedorDTO;
import com.mercado.validade_api.dto.UltimoFornecedorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lê fornecedores do Uniplus (entidade onde fornecedor = 1), somente leitura.
 * Usa coalesce de whatsapp/celular para o número de contato.
 */
@Service
@RequiredArgsConstructor
public class FornecedorUniplusService {

    private final @Qualifier("erpJdbcTemplate") JdbcTemplate erpJdbcTemplate;

    private static final RowMapper<FornecedorDTO> MAPPER = (rs, n) -> FornecedorDTO.builder()
            .id(rs.getLong("id"))
            .nome(rs.getString("nome"))
            .cnpjcpf(rs.getString("cnpjcpf"))
            .whatsapp(rs.getString("whatsapp"))
            .email(rs.getString("email"))
            .build();

    /** Busca fornecedores por nome/razão social (para adicionar lançamentos). */
    public List<FornecedorDTO> buscar(String termo) {
        String like = "%" + (termo == null ? "" : termo.trim().toUpperCase()) + "%";
        String sql = """
                SELECT e.id,
                       COALESCE(e.nome, e.razaosocial) AS nome,
                       e.cnpjcpf,
                       COALESCE(NULLIF(e.whatsapp, ''), e.celular) AS whatsapp,
                       e.email
                FROM entidade e
                WHERE e.fornecedor = 1
                  AND (UPPER(COALESCE(e.nome, e.razaosocial)) LIKE ? OR e.cnpjcpf LIKE ?)
                ORDER BY nome
                LIMIT 30
                """;
        return erpJdbcTemplate.query(sql, MAPPER, like, like);
    }

    /** Dados de um fornecedor específico pelo id da entidade. */
    public FornecedorDTO porId(Long id) {
        String sql = """
                SELECT e.id,
                       COALESCE(e.nome, e.razaosocial) AS nome,
                       e.cnpjcpf,
                       COALESCE(NULLIF(e.whatsapp, ''), e.celular) AS whatsapp,
                       e.email
                FROM entidade e
                WHERE e.id = ?
                """;
        List<FornecedorDTO> r = erpJdbcTemplate.query(sql, MAPPER, id);
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Produtos agregados a um fornecedor (do histórico de notas de entrada),
     * cada produto uma vez com o custo unitário da entrada mais recente.
     */
    public List<ProdutoFornecedorDTO> produtosDoFornecedor(Long fornecedorId) {
        String sql = """
                SELECT DISTINCT ON (p.id)
                    p.id AS produto_id,
                    p.ean,
                    p.nome AS produto,
                    COALESCE(h.nome, 'SEM GRUPO') AS grupo,
                    nf.datainclusao AS ultima_entrada,
                    nfi.quantidade AS ult_qtd,
                    nfi.total AS ult_total,
                    ROUND(nfi.total / NULLIF(nfi.quantidade, 0), 2) AS custo_unitario
                FROM notafiscal nf
                JOIN notafiscalitem nfi ON nfi.idnotafiscal = nf.id
                JOIN produto p ON p.id = nfi.idproduto
                LEFT JOIN hierarquia h ON p.idhierarquia = h.id
                WHERE nf.identidade = ?
                  AND nf.status NOT IN (2, 3)
                  AND SUBSTRING(CAST(nfi.cfop AS VARCHAR), 1, 1) IN ('1','2','3')
                ORDER BY p.id, nf.datainclusao DESC
                """;
        return erpJdbcTemplate.query(sql, (rs, n) -> ProdutoFornecedorDTO.builder()
                .produtoId(rs.getLong("produto_id"))
                .ean(rs.getString("ean"))
                .nome(rs.getString("produto"))
                .grupo(rs.getString("grupo"))
                .ultimaEntrada(rs.getObject("ultima_entrada", java.time.LocalDateTime.class))
                .ultimaQtd(rs.getBigDecimal("ult_qtd"))
                .ultimoTotal(rs.getBigDecimal("ult_total"))
                .custoUnitario(rs.getBigDecimal("custo_unitario"))
                .build(), fornecedorId);
    }

    /** Último fornecedor que trouxe o produto (pelo EAN) + custo unitário da última entrada. */
    public UltimoFornecedorDTO ultimoFornecedorDoProduto(String ean) {
        if (ean == null || ean.isBlank()) return null;
        String sql = """
                SELECT e.id AS fornecedor_id,
                       COALESCE(e.nome, e.razaosocial) AS nome,
                       COALESCE(NULLIF(e.whatsapp, ''), e.celular) AS whatsapp,
                       ROUND(nfi.total / NULLIF(nfi.quantidade, 0), 2) AS custo_unitario
                FROM produto p
                JOIN notafiscalitem nfi ON nfi.idproduto = p.id
                JOIN notafiscal nf ON nf.id = nfi.idnotafiscal
                LEFT JOIN entidade e ON e.id = nf.identidade
                WHERE p.ean = ?
                  AND nf.status NOT IN (2, 3)
                  AND SUBSTRING(CAST(nfi.cfop AS VARCHAR), 1, 1) IN ('1','2','3')
                  AND e.id IS NOT NULL
                ORDER BY nf.datainclusao DESC
                LIMIT 1
                """;
        List<UltimoFornecedorDTO> r = erpJdbcTemplate.query(sql, (rs, n) -> UltimoFornecedorDTO.builder()
                .fornecedorId(rs.getLong("fornecedor_id"))
                .fornecedorNome(rs.getString("nome"))
                .whatsapp(rs.getString("whatsapp"))
                .custoUnitario(rs.getBigDecimal("custo_unitario"))
                .build(), ean);
        return r.isEmpty() ? null : r.get(0);
    }

    /**
     * Custo unitário do produto pelo EAN, independente do fornecedor. Cadeia de fallback:
     * 1) custo cadastrado no produto (produto.precocusto);
     * 2) custo congelado da última venda (item.sh_custo);
     * 3) custo da última entrada de qualquer fornecedor (notafiscalitem).
     * Retorna ZERO só quando o produto não existe no Uniplus (ex.: cadastro de teste).
     */
    public java.math.BigDecimal custoDoProduto(String ean) {
        if (ean == null || ean.isBlank()) return java.math.BigDecimal.ZERO;

        // 1) custo cadastrado no produto
        java.math.BigDecimal c = um("""
                SELECT precocusto FROM produto
                WHERE ean = ? AND precocusto IS NOT NULL AND precocusto > 0
                LIMIT 1
                """, ean);
        if (c != null) return c;

        // 2) custo congelado da última venda
        c = um("""
                SELECT i.sh_custo
                FROM produto p
                JOIN item i ON i.produto = p.codigo
                JOIN operacao o ON o.id = i.idoperacao
                WHERE p.ean = ? AND i.sh_custo > 0
                  AND o.empresa = '1' AND o.tipo = 1
                  AND i.cancelado = 0 AND o.cancelado = 0
                ORDER BY o.horafinal DESC
                LIMIT 1
                """, ean);
        if (c != null) return c;

        // 3) custo da última entrada (qualquer fornecedor)
        c = um("""
                SELECT ROUND(nfi.total / NULLIF(nfi.quantidade, 0), 2)
                FROM produto p
                JOIN notafiscalitem nfi ON nfi.idproduto = p.id
                JOIN notafiscal nf ON nf.id = nfi.idnotafiscal
                WHERE p.ean = ?
                  AND nf.status NOT IN (2, 3)
                  AND SUBSTRING(CAST(nfi.cfop AS VARCHAR), 1, 1) IN ('1','2','3')
                ORDER BY nf.datainclusao DESC
                LIMIT 1
                """, ean);
        return c != null ? c : java.math.BigDecimal.ZERO;
    }

    private java.math.BigDecimal um(String sql, Object... args) {
        try {
            java.math.BigDecimal v = erpJdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, args);
            return (v != null && v.signum() > 0) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Total de bonificações (CFOP 1910/1910a) recebidas de um fornecedor — crédito acumulado. */
    public java.math.BigDecimal bonificacaoTotalDoFornecedor(Long fornecedorId) {
        String sql = """
                SELECT COALESCE(SUM(nfi.total), 0) AS total
                FROM notafiscal nf
                JOIN notafiscalitem nfi ON nfi.idnotafiscal = nf.id
                WHERE nf.identidade = ?
                  AND LOWER(CAST(nfi.cfop AS TEXT)) LIKE '1910%'
                  AND nf.status NOT IN (2, 3)
                """;
        java.math.BigDecimal total = erpJdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, fornecedorId);
        return total != null ? total : java.math.BigDecimal.ZERO;
    }
}
