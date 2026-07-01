package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.RebaixaGeradaDTO;
import com.mercado.validade_api.dto.UltimoFornecedorDTO;
import com.mercado.validade_api.entity.LancamentoFornecedor;
import com.mercado.validade_api.entity.LancamentoMercado;
import com.mercado.validade_api.entity.LancamentoStatus;
import com.mercado.validade_api.entity.LancamentoTipo;
import com.mercado.validade_api.entity.LoteCaptura;
import com.mercado.validade_api.entity.MercadoTipo;
import com.mercado.validade_api.entity.Produto;
import com.mercado.validade_api.repository.LancamentoFornecedorRepository;
import com.mercado.validade_api.repository.LancamentoMercadoRepository;
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
 * (de custo) na conta do fornecedor escolhido ou na conta do mercado.
 */
@Service
@RequiredArgsConstructor
public class RebaixaService {

    private final LoteCapturaRepository loteRepository;
    private final LancamentoFornecedorRepository lancamentoRepository;
    private final LancamentoMercadoRepository lancamentoMercadoRepository;
    private final FornecedorUniplusService fornecedorService;

    /** Já existe rebaixa lançada (fornecedor ou mercado) para este lote? */
    private boolean jaTemRebaixa(UUID loteId) {
        return lancamentoRepository.existsByLoteId(loteId) || lancamentoMercadoRepository.existsByLoteId(loteId);
    }

    /** Custo do produto do lote, usando a mesma cadeia de fallback do fluxo do fornecedor. */
    private BigDecimal resolverCusto(Produto produto, UltimoFornecedorDTO ultimo) {
        if (ultimo != null && ultimo.getCustoUnitario() != null && ultimo.getCustoUnitario().signum() > 0) {
            return ultimo.getCustoUnitario();
        }
        return fornecedorService.custoDoProduto(produto.getCodigoBarras());
    }

    /** Lança a rebaixa do lote na conta do MERCADO (tipo REBAIXA_VALIDADE). */
    @Transactional
    public RebaixaGeradaDTO gerarNoMercado(UUID loteId) {
        if (jaTemRebaixa(loteId)) {
            return RebaixaGeradaDTO.builder()
                    .criada(false)
                    .mensagem("Rebaixa já havia sido gerada para este lote.")
                    .build();
        }

        LoteCaptura lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado"));
        Produto produto = lote.getProduto();

        UltimoFornecedorDTO ultimo = fornecedorService.ultimoFornecedorDoProduto(produto.getCodigoBarras());
        BigDecimal custo = resolverCusto(produto, ultimo);
        BigDecimal valor = custo.multiply(BigDecimal.valueOf(lote.getQuantidadeAtual()));

        lancamentoMercadoRepository.save(LancamentoMercado.builder()
                .tipo(MercadoTipo.REBAIXA_VALIDADE)
                .valor(valor.abs())
                .loteId(loteId)
                .descricao("Rebaixa de validade — " + produto.getNome() + " — lote " + lote.getNumeroLote()
                        + " (" + lote.getQuantidadeAtual() + " un.)")
                .data(java.time.LocalDate.now())
                .criadoPor("Web")
                .build());

        return RebaixaGeradaDTO.builder()
                .criada(true)
                .produtoNome(produto.getNome())
                .valor(valor)
                .mensagem("Rebaixa de " + valor + " lançada na conta do mercado")
                .build();
    }

    @Transactional
    public RebaixaGeradaDTO gerarDoLote(UUID loteId) {
        return gerarDoLote(loteId, null);
    }

    @Transactional
    public RebaixaGeradaDTO gerarDoLote(UUID loteId, Long fornecedorIdEscolhido) {
        if (jaTemRebaixa(loteId)) {
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
        } else {
            if (ultimo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sem fornecedor no Uniplus para o produto " + produto.getNome() + " — escolha um fornecedor.");
            }
            fornId = ultimo.getFornecedorId();
            fornNome = ultimo.getFornecedorNome();
            fornWhatsapp = ultimo.getWhatsapp();
        }

        custo = resolverCusto(produto, ultimo);
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
