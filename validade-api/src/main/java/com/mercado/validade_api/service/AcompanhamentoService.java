package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.AcompanhamentoResponseDTO;
import com.mercado.validade_api.dto.GiroDTO;
import com.mercado.validade_api.entity.LoteCaptura;
import com.mercado.validade_api.entity.Produto;
import com.mercado.validade_api.repository.LancamentoFornecedorRepository;
import com.mercado.validade_api.repository.LoteCapturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Monta o acompanhamento: cada lote capturado (Supabase) cruzado com o giro real
 * de vendas (Uniplus), gerando o semáforo de validade e a recomendação de ação.
 */
@Service
@RequiredArgsConstructor
public class AcompanhamentoService {

    private static final int LIMITE_CRITICO = 30;
    private static final int LIMITE_ATENCAO = 60;
    // O lote só entra no acompanhamento após 30 dias de cadastrado (fase de histórico de giro).
    private static final int DIAS_FASE_HISTORICO = 30;

    private final LoteCapturaRepository loteRepository;
    private final GiroService giroService;
    private final LancamentoFornecedorRepository lancamentoRepository;

    @Transactional(readOnly = true)
    public List<AcompanhamentoResponseDTO> listar(boolean incluirNovos) {
        List<AcompanhamentoResponseDTO> resultado = new ArrayList<>();
        LocalDateTime corteHistorico = LocalDateTime.now().minusDays(DIAS_FASE_HISTORICO);
        Set<UUID> lotesComRebaixa = lancamentoRepository.findLoteIdsComRebaixa();

        for (LoteCaptura lote : loteRepository.findAllByOrderByDataVencimentoAsc()) {
            // Regra: só aparece depois de 30 dias cadastrado (a menos que incluirNovos=true para teste).
            if (!incluirNovos && lote.getCriadoEm() != null && lote.getCriadoEm().isAfter(corteHistorico)) {
                continue;
            }
            Produto produto = lote.getProduto();
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), lote.getDataVencimento());
            String status = dias < 0 ? "VENCIDO"
                    : dias <= LIMITE_CRITICO ? "CRITICO"
                    : dias <= LIMITE_ATENCAO ? "ATENCAO" : "NORMAL";

            // Vendido no Uniplus desde o dia que o lote foi capturado no app
            java.time.LocalDate dataCaptura = lote.getCriadoEm() != null
                    ? lote.getCriadoEm().toLocalDate()
                    : lote.getDataVencimento().minusYears(1); // fallback improvavel

            // UMA consulta ao Uniplus: 30d, 90d e desde a captura
            GiroDTO giro = giroService.calcular(produto.getCodigoBarras(), dataCaptura);
            int vendidoDesdeCaptura = giro.getVendidoDesdeCaptura();
            int quantidadeRestante = Math.max(0, lote.getQuantidadeInicial() - vendidoDesdeCaptura);

            // Velocidade primaria: 30 dias (mais recente); se nao houver, usa 90 dias.
            double velocidadeDia = giro.getVelocidade30() > 0 ? giro.getVelocidade30() : giro.getVelocidade90();
            Long diasParaEsgotar = velocidadeDia > 0
                    ? (long) Math.ceil(quantidadeRestante / velocidadeDia)
                    : null;

            // Lote totalmente vendido: o giro do Uniplus ja consumiu toda a quantidade inicial.
            boolean vendido = quantidadeRestante <= 0;
            Recomendacao rec = vendido
                    ? new Recomendacao("Vendido — baixar do estoque", "VENDIDO")
                    : recomendar(status, dias, diasParaEsgotar);
            String statusFinal = vendido ? "VENDIDO" : status;

            resultado.add(AcompanhamentoResponseDTO.builder()
                    .loteId(lote.getId())
                    .produtoNome(produto.getNome())
                    .codigoBarras(produto.getCodigoBarras())
                    .grupo(produto.getCategoria())
                    .numeroLote(lote.getNumeroLote())
                    .quantidadeAtual(lote.getQuantidadeAtual())
                    .dataVencimento(lote.getDataVencimento())
                    .diasParaVencer(dias)
                    .status(statusFinal)
                    .vendido30d(giro.getVendido30d())
                    .vendido90d(giro.getVendido90d())
                    .velocidade30(giro.getVelocidade30())
                    .velocidade90(giro.getVelocidade90())
                    .diasParaEsgotar(diasParaEsgotar)
                    .recomendacao(rec.texto())
                    .severidade(rec.severidade())
                    .rebaixaGerada(lotesComRebaixa.contains(lote.getId()))
                    .vendidoDesdeCaptura(vendidoDesdeCaptura)
                    .quantidadeRestante(quantidadeRestante)
                    .build());
        }
        return resultado;
    }

    private record Recomendacao(String texto, String severidade) {}

    private Recomendacao recomendar(String status, long diasParaVencer, Long diasParaEsgotar) {
        if ("VENCIDO".equals(status)) {
            return new Recomendacao("VENCIDO — retirar da prateleira", "CRITICO");
        }
        if (diasParaEsgotar == null) {
            // Sem nenhuma venda nos ultimos 90 dias. A acao depende de quao perto do vencimento.
            return switch (status) {
                case "CRITICO" -> new Recomendacao("Sem giro e vencendo — devolver ao fornecedor", "CRITICO");
                case "ATENCAO" -> new Recomendacao("Sem giro — devolver ou promover", "ATENCAO");
                default -> new Recomendacao("Sem giro — observar", "OBSERVAR");
            };
        }
        if (diasParaEsgotar <= diasParaVencer) {
            return new Recomendacao("Giro suficiente — OK", "OK");
        }
        // Vai sobrar produto no vencimento
        return switch (status) {
            case "CRITICO" -> new Recomendacao("Vence antes de vender — devolver ou promover JÁ", "CRITICO");
            case "ATENCAO" -> new Recomendacao("Vai sobrar no vencimento — promover", "ATENCAO");
            default -> new Recomendacao("Giro lento para a validade — acompanhar", "OBSERVAR");
        };
    }
}
