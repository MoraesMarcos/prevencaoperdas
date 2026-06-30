package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.RebaixaGeradaDTO;
import com.mercado.validade_api.dto.UltimoFornecedorDTO;
import com.mercado.validade_api.entity.LancamentoFornecedor;
import com.mercado.validade_api.entity.LancamentoStatus;
import com.mercado.validade_api.entity.LancamentoTipo;
import com.mercado.validade_api.entity.LoteCaptura;
import com.mercado.validade_api.entity.Produto;
import com.mercado.validade_api.repository.LancamentoFornecedorRepository;
import com.mercado.validade_api.repository.LoteCapturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rebaixa automática: a partir de um lote em atenção/crítico, lança uma REBAIXA
 * (de custo) na conta do último fornecedor que trouxe aquele produto.
 */
@Service
@RequiredArgsConstructor
public class RebaixaService {

    private final LoteCapturaRepository loteRepository;
    private final LancamentoFornecedorRepository lancamentoRepository;
    private final FornecedorUniplusService fornecedorService;

    @Transactional
    public RebaixaGeradaDTO gerarDoLote(UUID loteId) {
        return gerarDoLote(loteId, null);
    }

    @Transactional
    public RebaixaGeradaDTO gerarDoLote(UUID loteId, Long fornecedorIdEscolhido) {
        if (lancamentoRepository.existsByLoteId(loteId)) {
            return RebaixaGeradaDTO.builder()
                    .criada(false)
                    .mensagem("Rebaixa já havia sido gerada para este lote.")
                    .build();
        }

        LoteCaptura lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado"));
        Produto produto = lote.getProduto();

        // Fornecedor: o último do Uniplus (para pegar o custo) — pode ser nulo em produto sem compra.
        UltimoFornecedorDTO ultimo = fornecedorService.ultimoFornecedorDoProduto(produto.getCodigoBarras());

        Long fornId;
        String fornNome;
        String fornWhatsapp;
        BigDecimal custo;

        if (fornecedorIdEscolhido != null) {
            // Operador escolheu o fornecedor (ex.: produto sem compra no Uniplus).
            var f = fornecedorService.porId(fornecedorIdEscolhido);
            if (f == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor escolhido não encontrado.");
            fornId = f.getId();
            fornNome = f.getNome();
            fornWhatsapp = f.getWhatsapp();
            // Se o escolhido for o mesmo do último, aproveita o custo; senão, sem custo conhecido (0).
            custo = (ultimo != null && fornecedorIdEscolhido.equals(ultimo.getFornecedorId()) && ultimo.getCustoUnitario() != null)
                    ? ultimo.getCustoUnitario() : BigDecimal.ZERO;
        } else {
            if (ultimo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sem fornecedor no Uniplus para o produto " + produto.getNome() + " — escolha um fornecedor.");
            }
            fornId = ultimo.getFornecedorId();
            fornNome = ultimo.getFornecedorNome();
            fornWhatsapp = ultimo.getWhatsapp();
            custo = ultimo.getCustoUnitario() != null ? ultimo.getCustoUnitario() : BigDecimal.ZERO;
        }

        BigDecimal valor = custo.multiply(BigDecimal.valueOf(lote.getQuantidadeAtual()));

        lancamentoRepository.save(LancamentoFornecedor.builder()
                .fornecedorId(fornId)
                .fornecedorNome(fornNome)
                .whatsapp(fornWhatsapp)
                .tipo(LancamentoTipo.REBAIXA)
                .valor(valor)
                .valorAbatido(BigDecimal.ZERO)
                .ean(produto.getCodigoBarras())
                .loteId(loteId)
                .produtoNome(produto.getNome())
                .descricao("Rebaixa — lote " + lote.getNumeroLote() + " (" + lote.getQuantidadeAtual() + " un.)")
                .status(LancamentoStatus.ATIVO)
                .criadoPor("Web")
                .build());

        return RebaixaGeradaDTO.builder()
                .criada(true)
                .fornecedorNome(fornNome)
                .produtoNome(produto.getNome())
                .valor(valor)
                .mensagem("Rebaixa de " + valor + " lançada para " + fornNome)
                .build();
    }
}
