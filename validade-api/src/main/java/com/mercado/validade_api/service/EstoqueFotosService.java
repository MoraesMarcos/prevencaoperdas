package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.EstoqueFotoDTO;
import com.mercado.validade_api.entity.LoteCaptura;
import com.mercado.validade_api.entity.LoteEvidencia;
import com.mercado.validade_api.entity.Produto;
import com.mercado.validade_api.repository.LoteCapturaRepository;
import com.mercado.validade_api.repository.LoteEvidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Monta a galeria de estoque: lotes + fotos, para o front agrupar por grupo. */
@Service
@RequiredArgsConstructor
public class EstoqueFotosService {

    private static final int LIMITE_CRITICO = 30;
    private static final int LIMITE_ATENCAO = 60;

    private final LoteCapturaRepository loteRepository;
    private final LoteEvidenciaRepository evidenciaRepository;

    @Transactional(readOnly = true)
    public List<EstoqueFotoDTO> listar() {
        return loteRepository.findAllByOrderByDataVencimentoAsc().stream().map(lote -> {
            Produto produto = lote.getProduto();
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), lote.getDataVencimento());
            String status = dias < 0 ? "VENCIDO"
                    : dias <= LIMITE_CRITICO ? "CRITICO"
                    : dias <= LIMITE_ATENCAO ? "ATENCAO" : "NORMAL";

            List<String> fotos = evidenciaRepository.findByLoteId(lote.getId()).stream()
                    .map(LoteEvidencia::getFotoUrl)
                    .toList();

            return EstoqueFotoDTO.builder()
                    .loteId(lote.getId())
                    .produtoNome(produto.getNome())
                    .grupo(produto.getCategoria() != null ? produto.getCategoria() : "SEM GRUPO")
                    .numeroLote(lote.getNumeroLote())
                    .quantidadeAtual(lote.getQuantidadeAtual())
                    .dataVencimento(lote.getDataVencimento())
                    .diasParaVencer(dias)
                    .status(status)
                    .fotos(fotos)
                    .build();
        }).toList();
    }
}
