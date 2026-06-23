package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.*;
import com.mercado.validade_api.entity.LancamentoFornecedor;
import com.mercado.validade_api.entity.LancamentoStatus;
import com.mercado.validade_api.repository.LancamentoFornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ContaCorrenteService {

    private final LancamentoFornecedorRepository repo;
    private final FornecedorUniplusService fornecedorService;

    /** Lista os fornecedores que têm conta corrente (algum lançamento), com saldo. */
    @Transactional(readOnly = true)
    public List<ContaCorrenteResumoDTO> listarFornecedores() {
        Map<Long, List<LancamentoFornecedor>> porFornecedor = new LinkedHashMap<>();
        for (LancamentoFornecedor l : repo.findAll()) {
            porFornecedor.computeIfAbsent(l.getFornecedorId(), k -> new ArrayList<>()).add(l);
        }

        List<ContaCorrenteResumoDTO> resultado = new ArrayList<>();
        for (var entrada : porFornecedor.entrySet()) {
            List<LancamentoFornecedor> ls = entrada.getValue();
            BigDecimal saldo = saldoAtivo(ls);
            int trocasAtivas = (int) ls.stream()
                    .filter(l -> l.getStatus() == LancamentoStatus.ATIVO && l.getTipo().getSinal() > 0)
                    .count();
            var primeiro = ls.get(0);
            resultado.add(ContaCorrenteResumoDTO.builder()
                    .fornecedorId(entrada.getKey())
                    .fornecedorNome(primeiro.getFornecedorNome())
                    .whatsapp(primeiro.getWhatsapp())
                    .saldoAtivo(saldo)
                    .trocasAtivas(trocasAtivas)
                    .build());
        }
        resultado.sort((a, b) -> b.getSaldoAtivo().compareTo(a.getSaldoAtivo()));
        return resultado;
    }

    /** Detalhe de um fornecedor: dados + saldo + lançamentos ativos e finalizados. */
    @Transactional(readOnly = true)
    public ContaCorrenteDetalheDTO detalhe(Long fornecedorId) {
        List<LancamentoFornecedor> ls = repo.findByFornecedorIdOrderByCriadoEmAsc(fornecedorId);
        FornecedorDTO f = fornecedorService.porId(fornecedorId);

        String nome = !ls.isEmpty() ? ls.get(0).getFornecedorNome() : (f != null ? f.getNome() : "Fornecedor");
        String whats = !ls.isEmpty() ? ls.get(0).getWhatsapp() : (f != null ? f.getWhatsapp() : null);

        return ContaCorrenteDetalheDTO.builder()
                .fornecedorId(fornecedorId)
                .fornecedorNome(nome)
                .whatsapp(whats)
                .email(f != null ? f.getEmail() : null)
                .saldoAtivo(saldoAtivo(ls))
                .ativas(ls.stream().filter(l -> l.getStatus() == LancamentoStatus.ATIVO).map(this::paraDTO).toList())
                .finalizadas(ls.stream().filter(l -> l.getStatus() == LancamentoStatus.FINALIZADO).map(this::paraDTO).toList())
                .build();
    }

    @Transactional
    public LancamentoResponseDTO adicionar(LancamentoRequestDTO dto) {
        if (dto.getFornecedorId() == null || dto.getTipo() == null || dto.getValor() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fornecedorId, tipo e valor são obrigatórios");
        }
        FornecedorDTO f = fornecedorService.porId(dto.getFornecedorId());
        if (f == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor não encontrado no Uniplus");
        }

        LancamentoFornecedor l = repo.save(LancamentoFornecedor.builder()
                .fornecedorId(f.getId())
                .fornecedorNome(f.getNome())
                .whatsapp(f.getWhatsapp())
                .tipo(dto.getTipo())
                .valor(dto.getValor().abs())
                .descricao(dto.getDescricao())
                .produtoNome(dto.getProdutoNome())
                .status(LancamentoStatus.ATIVO)
                .criadoPor(dto.getCriadoPor())
                .build());

        return paraDTO(l);
    }

    @Transactional
    public void finalizar(UUID lancamentoId) {
        LancamentoFornecedor l = repo.findById(lancamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado"));
        l.setStatus(LancamentoStatus.FINALIZADO);
        repo.save(l);
    }

    @Transactional
    public void remover(UUID lancamentoId) {
        if (!repo.existsById(lancamentoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado");
        }
        repo.deleteById(lancamentoId);
    }

    private BigDecimal saldoAtivo(List<LancamentoFornecedor> ls) {
        BigDecimal saldo = BigDecimal.ZERO;
        for (LancamentoFornecedor l : ls) {
            if (l.getStatus() == LancamentoStatus.ATIVO) {
                saldo = saldo.add(l.getValor().multiply(BigDecimal.valueOf(l.getTipo().getSinal())));
            }
        }
        return saldo;
    }

    private LancamentoResponseDTO paraDTO(LancamentoFornecedor l) {
        return LancamentoResponseDTO.builder()
                .id(l.getId())
                .tipo(l.getTipo().name())
                .valor(l.getValor())
                .sinal(l.getTipo().getSinal())
                .descricao(l.getDescricao())
                .produtoNome(l.getProdutoNome())
                .status(l.getStatus().name())
                .criadoEm(l.getCriadoEm())
                .build();
    }
}
