package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.CapturaRequestDTO;
import com.mercado.validade_api.dto.CapturaResponseDTO;
import com.mercado.validade_api.entity.LoteCaptura;
import com.mercado.validade_api.entity.LoteEvidencia;
import com.mercado.validade_api.entity.Produto;
import com.mercado.validade_api.repository.LoteCapturaRepository;
import com.mercado.validade_api.repository.LoteEvidenciaRepository;
import com.mercado.validade_api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CapturaService {

    // Limites do semáforo de validade (em dias)
    private static final int LIMITE_CRITICO = 30;
    private static final int LIMITE_ATENCAO = 60;

    private final ProdutoRepository produtoRepository;
    private final LoteCapturaRepository loteRepository;
    private final LoteEvidenciaRepository evidenciaRepository;

    @Transactional
    public CapturaResponseDTO registrar(CapturaRequestDTO dto) {
        validar(dto);

        // 1) Acha o produto pelo código de barras ou cria um novo
        Produto produto = produtoRepository.findByCodigoBarras(dto.getCodigoBarras())
                .orElseGet(() -> produtoRepository.save(Produto.builder()
                        .codigoBarras(dto.getCodigoBarras())
                        .nome(dto.getNome())
                        .marca(dto.getMarca())
                        .categoria(dto.getCategoria())
                        .build()));

        // 2) Registra o lote
        LoteCaptura lote = loteRepository.save(LoteCaptura.builder()
                .produto(produto)
                .numeroLote(dto.getNumeroLote())
                .quantidadeInicial(dto.getQuantidadeInicial())
                .quantidadeAtual(dto.getQuantidadeInicial())
                .dataVencimento(dto.getDataVencimento())
                .notaFiscal(dto.getNotaFiscal())
                .fornecedorId(dto.getFornecedorId())
                .criadoPor(dto.getCriadoPor())
                .build());

        // 3) Salva as evidências (fotos)
        List<String> fotos = new ArrayList<>();
        if (dto.getFotosUrls() != null) {
            for (String url : dto.getFotosUrls()) {
                if (url == null || url.isBlank()) continue;
                evidenciaRepository.save(LoteEvidencia.builder()
                        .lote(lote)
                        .fotoUrl(url)
                        .build());
                fotos.add(url);
            }
        }

        return montarResposta(lote, fotos);
    }

    @Transactional(readOnly = true)
    public List<CapturaResponseDTO> listar() {
        List<CapturaResponseDTO> resultado = new ArrayList<>();
        for (LoteCaptura lote : loteRepository.findAllByOrderByDataVencimentoAsc()) {
            List<String> fotos = evidenciaRepository.findByLoteId(lote.getId())
                    .stream().map(LoteEvidencia::getFotoUrl).toList();
            resultado.add(montarResposta(lote, fotos));
        }
        return resultado;
    }

    private void validar(CapturaRequestDTO dto) {
        if (dto.getCodigoBarras() == null || dto.getCodigoBarras().isBlank())
            erro("codigoBarras é obrigatório");
        if (dto.getNumeroLote() == null || dto.getNumeroLote().isBlank())
            erro("numeroLote é obrigatório");
        if (dto.getQuantidadeInicial() == null || dto.getQuantidadeInicial() <= 0)
            erro("quantidadeInicial deve ser maior que zero");
        if (dto.getDataVencimento() == null)
            erro("dataVencimento é obrigatória");
        if (dto.getCriadoPor() == null || dto.getCriadoPor().isBlank())
            erro("criadoPor é obrigatório");
    }

    private void erro(String msg) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private CapturaResponseDTO montarResposta(LoteCaptura lote, List<String> fotos) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), lote.getDataVencimento());
        String status = dias <= LIMITE_CRITICO ? "CRITICO"
                : dias <= LIMITE_ATENCAO ? "ATENCAO" : "NORMAL";

        return CapturaResponseDTO.builder()
                .loteId(lote.getId())
                .produtoId(lote.getProduto().getId())
                .produtoNome(lote.getProduto().getNome())
                .codigoBarras(lote.getProduto().getCodigoBarras())
                .numeroLote(lote.getNumeroLote())
                .quantidadeAtual(lote.getQuantidadeAtual())
                .dataVencimento(lote.getDataVencimento())
                .diasParaVencer(dias)
                .status(status)
                .fotos(fotos)
                .build();
    }
}
