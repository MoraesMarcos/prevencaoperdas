package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.FornecedorDTO;
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
}
