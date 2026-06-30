package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.BonificacaoDTO;
import com.mercado.validade_api.dto.ContaMercadoDTO;
import com.mercado.validade_api.dto.LancamentoMercadoDTO;
import com.mercado.validade_api.entity.LancamentoMercado;
import com.mercado.validade_api.entity.MercadoTipo;
import com.mercado.validade_api.repository.LancamentoMercadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Conta corrente do mercado: crédito vem das bonificações do Uniplus (CFOP 1910/1910a),
 * gasto em avaria da loja, troca de preço, etc. (lançamentos manuais no Supabase).
 */
@Service
@RequiredArgsConstructor
public class ContaMercadoService {

    private final @Qualifier("erpJdbcTemplate") JdbcTemplate erpJdbcTemplate;
    private final LancamentoMercadoRepository repo;

    private static final String SQL_BONIFICACOES = """
            SELECT nf.datainclusao AS data, nf.numeronotafiscal AS danfe,
                   COALESCE(e.nome, e.razaosocial) AS fornecedor, p.nome AS produto,
                   nfi.quantidade AS qtd, nfi.total AS valor
            FROM notafiscal nf
            JOIN notafiscalitem nfi ON nfi.idnotafiscal = nf.id
            JOIN produto p ON p.id = nfi.idproduto
            LEFT JOIN entidade e ON e.id = nf.identidade
            WHERE LOWER(CAST(nfi.cfop AS TEXT)) LIKE '1910%'
              AND nf.status NOT IN (2, 3)
              AND nf.datainclusao >= ? AND nf.datainclusao <= ?
            ORDER BY nf.datainclusao DESC
            """;

    @Transactional(readOnly = true)
    public ContaMercadoDTO conta(LocalDate inicio, LocalDate fim) {
        LocalDateTime ini = inicio.atStartOfDay();
        LocalDateTime f = LocalDateTime.of(fim, LocalTime.of(23, 59, 59));

        List<BonificacaoDTO> bonificacoes = erpJdbcTemplate.query(SQL_BONIFICACOES, (rs, n) -> BonificacaoDTO.builder()
                .data(rs.getObject("data", LocalDateTime.class))
                .danfe(rs.getString("danfe"))
                .fornecedor(rs.getString("fornecedor"))
                .produto(rs.getString("produto"))
                .quantidade(rs.getBigDecimal("qtd"))
                .valor(rs.getBigDecimal("valor"))
                .build(), ini, f);

        BigDecimal totalBonificacoes = bonificacoes.stream()
                .map(BonificacaoDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LancamentoMercado> manuais = repo.findAllByOrderByDataDesc();
        BigDecimal creditosManuais = BigDecimal.ZERO;
        BigDecimal usos = BigDecimal.ZERO;
        for (LancamentoMercado l : manuais) {
            if (l.getTipo().getSinal() > 0) creditosManuais = creditosManuais.add(l.getValor());
            else usos = usos.add(l.getValor());
        }

        BigDecimal saldo = totalBonificacoes.add(creditosManuais).subtract(usos);

        return ContaMercadoDTO.builder()
                .saldo(saldo)
                .totalBonificacoes(totalBonificacoes)
                .totalUsos(usos)
                .bonificacoes(bonificacoes)
                .lancamentos(manuais.stream().map(this::paraDTO).toList())
                .build();
    }

    @Transactional
    public LancamentoMercadoDTO adicionar(MercadoTipo tipo, BigDecimal valor, String descricao, LocalDate data, String criadoPor) {
        if (tipo == null || valor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo e valor são obrigatórios");
        }
        LancamentoMercado l = repo.save(LancamentoMercado.builder()
                .tipo(tipo)
                .valor(valor.abs())
                .descricao(descricao)
                .data(data != null ? data : LocalDate.now())
                .criadoPor(criadoPor)
                .build());
        return paraDTO(l);
    }

    @Transactional
    public void remover(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado");
        }
        repo.deleteById(id);
    }

    private LancamentoMercadoDTO paraDTO(LancamentoMercado l) {
        return LancamentoMercadoDTO.builder()
                .id(l.getId())
                .tipo(l.getTipo().name())
                .sinal(l.getTipo().getSinal())
                .valor(l.getValor())
                .descricao(l.getDescricao())
                .data(l.getData())
                .build();
    }
}
