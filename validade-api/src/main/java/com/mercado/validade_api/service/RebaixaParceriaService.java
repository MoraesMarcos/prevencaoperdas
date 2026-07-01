package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.RebaixaParceriaItemDTO;
import com.mercado.validade_api.dto.UltimoFornecedorDTO;
import com.mercado.validade_api.entity.*;
import com.mercado.validade_api.repository.LancamentoFornecedorRepository;
import com.mercado.validade_api.repository.LancamentoMercadoRepository;
import com.mercado.validade_api.repository.RebaixaParceriaMarcadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REBAIXA PARCERIA (promocao = 2 no Uniplus): a cada venda, o desconto
 * (precobruto - precoliquido) × qtd é uma cobertura que alguém precisa bancar.
 * Aqui o operador decide, por produto, se quem paga é o FORNECEDOR (conta do
 * fornecedor) ou o MERCADO (desconta da bonificação). Idempotente via marcador.
 */
@Service
@RequiredArgsConstructor
public class RebaixaParceriaService {

    // Janela máxima de backlog considerada quando o produto nunca foi processado.
    private static final int DIAS_BACKLOG = 180;

    private final @Qualifier("erpJdbcTemplate") JdbcTemplate erpJdbcTemplate;
    private final RebaixaParceriaMarcadorRepository marcadorRepository;
    private final LancamentoFornecedorRepository lancamentoFornecedorRepository;
    private final LancamentoMercadoRepository lancamentoMercadoRepository;
    private final FornecedorUniplusService fornecedorService;

    // TUDO em 1 query: cobertura agrupada por produto + fornecedor (LATERAL), na janela de backlog.
    // COALESCE(precobruto, precoliquido) trata linhas com precobruto nulo (mesma regra da auditoria de descontos).
    private static final String SQL_AGRUPADO = """
            SELECT s.ean, s.nome, s.cobertura, s.qtd, s.ultima,
                   f.fornecedor_id, f.fornecedor_nome, f.whatsapp
            FROM (
                SELECT p.ean AS ean, MAX(p.nome) AS nome,
                       SUM((COALESCE(i.precobruto, i.precoliquido) - i.precoliquido) * i.quantidade) AS cobertura,
                       SUM(i.quantidade) AS qtd,
                       MAX(o.horafinal) AS ultima
                FROM item i
                JOIN operacao o ON o.id = i.idoperacao
                JOIN produto p  ON p.codigo = i.produto
                WHERE i.promocao = '2'
                  AND o.empresa = '1' AND o.tipo = 1
                  AND i.cancelado = 0 AND o.cancelado = 0
                  AND COALESCE(i.precobruto, i.precoliquido) > i.precoliquido
                  AND o.horafinal >= now() - make_interval(days => ?)
                GROUP BY p.ean
                HAVING SUM((COALESCE(i.precobruto, i.precoliquido) - i.precoliquido) * i.quantidade) > 0
            ) s
            LEFT JOIN LATERAL (
                SELECT e.id AS fornecedor_id,
                       COALESCE(e.nome, e.razaosocial) AS fornecedor_nome,
                       COALESCE(NULLIF(e.whatsapp, ''), e.celular) AS whatsapp
                FROM produto p2
                JOIN notafiscalitem nfi ON nfi.idproduto = p2.id
                JOIN notafiscal nf ON nf.id = nfi.idnotafiscal
                JOIN entidade e ON e.id = nf.identidade
                WHERE p2.ean = s.ean
                  AND nf.status NOT IN (2, 3)
                  AND SUBSTRING(CAST(nfi.cfop AS VARCHAR), 1, 1) IN ('1','2','3')
                ORDER BY nf.datainclusao DESC
                LIMIT 1
            ) f ON true
            ORDER BY s.ultima DESC
            """;

    // Cobertura pendente de um EAN (vendas de parceria depois de 'desde').
    private static final String SQL_PENDENTE = """
            SELECT COALESCE(SUM((COALESCE(i.precobruto, i.precoliquido) - i.precoliquido) * i.quantidade), 0) AS cobertura,
                   COALESCE(SUM(i.quantidade), 0) AS qtd,
                   MAX(o.horafinal) AS ultima
            FROM item i
            JOIN operacao o ON o.id = i.idoperacao
            JOIN produto p  ON p.codigo = i.produto
            WHERE p.ean = ?
              AND i.promocao = '2'
              AND o.empresa = '1' AND o.tipo = 1
              AND i.cancelado = 0 AND o.cancelado = 0
              AND COALESCE(i.precobruto, i.precoliquido) > i.precoliquido
              AND o.horafinal > CAST(? AS timestamp)
            """;

    // Linhas cruas (sem agregação) para auditoria visual — mesmo formato da tela de auditoria de descontos.
    private static final String SQL_LINHAS = """
            SELECT o.horafinal AS data_hora,
                   COALESCE(i.precobruto, i.precoliquido) AS preco_bruto,
                   i.precoliquido AS preco_liquido,
                   i.quantidade,
                   (COALESCE(i.precobruto, i.precoliquido) - i.precoliquido) AS desconto_unitario,
                   (COALESCE(i.precobruto, i.precoliquido) - i.precoliquido) * i.quantidade AS desconto_linha
            FROM item i
            JOIN operacao o ON o.id = i.idoperacao
            JOIN produto p  ON p.codigo = i.produto
            WHERE p.ean = ?
              AND i.promocao = '2'
              AND o.empresa = '1' AND o.tipo = 1
              AND i.cancelado = 0 AND o.cancelado = 0
              AND COALESCE(i.precobruto, i.precoliquido) > i.precoliquido
              AND o.horafinal > CAST(? AS timestamp)
            ORDER BY o.horafinal DESC
            """;

    /** Lista as vendas individuais (sem agregação) que compõem a cobertura pendente do EAN — auditoria. */
    @Transactional(readOnly = true)
    public List<com.mercado.validade_api.dto.RebaixaParceriaLinhaDTO> listarLinhas(String ean) {
        LocalDateTime desde = marcadorRepository.findById(ean)
                .map(RebaixaParceriaMarcador::getProcessadoAte)
                .orElse(LocalDateTime.now().minusDays(DIAS_BACKLOG));

        return erpJdbcTemplate.query(SQL_LINHAS, (rs, n) -> com.mercado.validade_api.dto.RebaixaParceriaLinhaDTO.builder()
                .dataHora(rs.getObject("data_hora", LocalDateTime.class))
                .precoBruto(rs.getBigDecimal("preco_bruto"))
                .precoLiquido(rs.getBigDecimal("preco_liquido"))
                .quantidade(rs.getBigDecimal("quantidade"))
                .descontoUnitario(rs.getBigDecimal("desconto_unitario"))
                .descontoLinha(rs.getBigDecimal("desconto_linha"))
                .build(), ean, java.sql.Timestamp.valueOf(desde));
    }

    private record Pendente(BigDecimal cobertura, BigDecimal qtd, LocalDateTime ultima) {}

    private Pendente pendente(String ean, LocalDateTime desde) {
        return erpJdbcTemplate.queryForObject(SQL_PENDENTE, (rs, n) -> new Pendente(
                rs.getBigDecimal("cobertura"),
                rs.getBigDecimal("qtd"),
                rs.getObject("ultima", LocalDateTime.class)
        ), ean, java.sql.Timestamp.valueOf(desde));
    }

    private record LinhaAgrupada(String ean, String nome, BigDecimal cobertura, BigDecimal qtd,
                                 LocalDateTime ultima, Long fornecedorId, String fornecedorNome) {}

    /** Lista, por produto, a cobertura de parceria ainda não lançada. */
    @Transactional(readOnly = true)
    public List<RebaixaParceriaItemDTO> listarPendentes() {
        // 1 query no ERP: cobertura agrupada por produto + fornecedor.
        List<LinhaAgrupada> linhas = erpJdbcTemplate.query(SQL_AGRUPADO, (rs, n) -> new LinhaAgrupada(
                rs.getString("ean"),
                rs.getString("nome"),
                rs.getBigDecimal("cobertura"),
                rs.getBigDecimal("qtd"),
                rs.getObject("ultima", LocalDateTime.class),
                (Long) rs.getObject("fornecedor_id"),
                rs.getString("fornecedor_nome")
        ), DIAS_BACKLOG);

        // 1 query no Supabase: todos os marcadores de uma vez.
        java.util.Map<String, LocalDateTime> marcadores = new java.util.HashMap<>();
        for (RebaixaParceriaMarcador m : marcadorRepository.findAll()) {
            marcadores.put(m.getEan(), m.getProcessadoAte());
        }

        List<RebaixaParceriaItemDTO> resultado = new ArrayList<>();
        for (LinhaAgrupada l : linhas) {
            if (l.ean() == null || l.ean().isBlank()) continue;

            BigDecimal cobertura = l.cobertura();
            BigDecimal qtd = l.qtd();
            LocalDateTime ultima = l.ultima();

            LocalDateTime processadoAte = marcadores.get(l.ean());
            if (processadoAte != null) {
                // Já houve lançamento antes. Se nada vendeu depois, não tem pendente.
                if (ultima != null && !ultima.isAfter(processadoAte)) continue;
                // Vendeu depois do último lançamento: refina só este produto (poucos casos).
                Pendente p = pendente(l.ean(), processadoAte);
                if (p.cobertura() == null || p.cobertura().signum() <= 0) continue;
                cobertura = p.cobertura();
                qtd = p.qtd();
                ultima = p.ultima();
            }

            resultado.add(RebaixaParceriaItemDTO.builder()
                    .ean(l.ean())
                    .produtoNome(l.nome())
                    .fornecedorId(l.fornecedorId())
                    .fornecedorNome(l.fornecedorId() != null ? l.fornecedorNome() : "— sem fornecedor —")
                    .quantidade(qtd)
                    .coberturaPendente(cobertura)
                    .ultimaVenda(ultima)
                    .build());
        }
        return resultado;
    }

    public enum Responsavel { FORNECEDOR, MERCADO }

    /** Lança a cobertura pendente de um produto na conta do FORNECEDOR ou do MERCADO. */
    @Transactional
    public String lancar(String ean, Responsavel responsavel, Long fornecedorIdEscolhido, String criadoPor) {
        if (ean == null || ean.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ean é obrigatório");

        LocalDateTime desde = marcadorRepository.findById(ean)
                .map(RebaixaParceriaMarcador::getProcessadoAte)
                .orElse(LocalDateTime.now().minusDays(DIAS_BACKLOG));

        Pendente p = pendente(ean, desde);
        if (p.cobertura() == null || p.cobertura().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nada pendente para este produto.");

        BigDecimal valor = p.cobertura();
        BigDecimal qtd = p.qtd() != null ? p.qtd() : BigDecimal.ZERO;
        String desc = "Rebaixa parceria — " + qtd.stripTrailingZeros().toPlainString() + " un. (EAN " + ean + ")";

        String destino;
        if (responsavel == Responsavel.FORNECEDOR) {
            // Fornecedor: o operador pode ter escolhido um; senão usa o último do Uniplus.
            Long fornId;
            String fornNome;
            String fornWhatsapp;
            if (fornecedorIdEscolhido != null) {
                var f = fornecedorService.porId(fornecedorIdEscolhido);
                if (f == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor escolhido não encontrado.");
                fornId = f.getId();
                fornNome = f.getNome();
                fornWhatsapp = f.getWhatsapp();
            } else {
                UltimoFornecedorDTO forn = fornecedorService.ultimoFornecedorDoProduto(ean);
                if (forn == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Sem fornecedor no Uniplus para este produto — escolha um fornecedor.");
                fornId = forn.getFornecedorId();
                fornNome = forn.getFornecedorNome();
                fornWhatsapp = forn.getWhatsapp();
            }
            lancamentoFornecedorRepository.save(LancamentoFornecedor.builder()
                    .fornecedorId(fornId)
                    .fornecedorNome(fornNome)
                    .whatsapp(fornWhatsapp)
                    .tipo(LancamentoTipo.REBAIXA)
                    .valor(valor)
                    .valorAbatido(BigDecimal.ZERO)
                    .ean(ean)
                    .produtoNome(produtoNomeDoUniplus(ean))
                    .descricao(desc)
                    .status(LancamentoStatus.ATIVO)
                    .criadoPor(criadoPor)
                    .build());
            destino = "conta do fornecedor " + fornNome;
        } else {
            lancamentoMercadoRepository.save(LancamentoMercado.builder()
                    .tipo(MercadoTipo.REBAIXA_PARCERIA)
                    .valor(valor.abs())
                    .descricao(desc + " — " + produtoNomeDoUniplus(ean))
                    .data(java.time.LocalDate.now())
                    .criadoPor(criadoPor)
                    .build());
            destino = "conta do mercado (descontado da bonificação)";
        }

        // Avança o marcador para a última venda lançada (idempotência).
        LocalDateTime ate = p.ultima() != null ? p.ultima() : LocalDateTime.now();
        marcadorRepository.save(RebaixaParceriaMarcador.builder()
                .ean(ean)
                .produtoNome(produtoNomeDoUniplus(ean))
                .processadoAte(ate)
                .build());

        return "Lançado R$ " + valor + " na " + destino + ".";
    }

    private String produtoNomeDoUniplus(String ean) {
        try {
            return erpJdbcTemplate.queryForObject(
                    "SELECT nome FROM produto WHERE ean = ? LIMIT 1", String.class, ean);
        } catch (Exception e) {
            return ean;
        }
    }
}
