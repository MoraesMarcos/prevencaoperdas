package com.mercado.validade_api.service;

import com.mercado.validade_api.dto.*;
import com.mercado.validade_api.entity.LancamentoFornecedor;
import com.mercado.validade_api.entity.LancamentoStatus;
import com.mercado.validade_api.entity.LancamentoTipo;
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
                .bonificacaoDisponivel(bonificacaoDisponivel(fornecedorId))
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
                .valorAbatido(BigDecimal.ZERO)
                .ean(dto.getEan())
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

    // Residual em centavos (<= R$0,10) é absorvido ao fechar um produto, para não
    // deixar centavos pendurados — a "regra dos centavos" pedida pelo usuário.
    private static final BigDecimal LIMIAR_CENTAVOS = new BigDecimal("0.10");

    /**
     * Abate um valor (pagamento/negociação) nos produtos em aberto do fornecedor,
     * FIFO (mais antigo primeiro). Fecha cada produto até o valor acabar; resíduos
     * de poucos centavos são absorvidos para fechar o produto limpo.
     */
    @Transactional
    public ContaCorrenteDetalheDTO abater(Long fornecedorId, BigDecimal valorPagamento) {
        aplicarAbatimentoFifo(fornecedorId, valorPagamento);
        return detalhe(fornecedorId);
    }

    /**
     * Abate trocas/rebaixas usando o crédito de bonificação do próprio fornecedor.
     * O valor é limitado ao crédito disponível (bonificações Uniplus − já usado).
     */
    @Transactional
    public ContaCorrenteDetalheDTO abaterComBonificacao(Long fornecedorId, BigDecimal valorSolicitado) {
        BigDecimal disponivel = bonificacaoDisponivel(fornecedorId);
        BigDecimal usar = valorSolicitado == null ? disponivel : valorSolicitado.min(disponivel);
        if (usar.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sem crédito de bonificação disponível.");
        }

        BigDecimal aplicado = aplicarAbatimentoFifo(fornecedorId, usar);

        if (aplicado.signum() > 0) {
            // Registro finalizado do uso da bonificação (não afeta o saldo, só rastreia o consumo).
            String nome = repo.findByFornecedorIdOrderByCriadoEmAsc(fornecedorId).stream()
                    .map(LancamentoFornecedor::getFornecedorNome).findFirst().orElse("Fornecedor");
            repo.save(LancamentoFornecedor.builder()
                    .fornecedorId(fornecedorId)
                    .fornecedorNome(nome)
                    .tipo(LancamentoTipo.BONIFICACAO)
                    .valor(aplicado)
                    .valorAbatido(aplicado)
                    .descricao("Abatido com crédito de bonificação")
                    .status(LancamentoStatus.FINALIZADO)
                    .criadoPor("Sistema (bonificação)")
                    .build());
        }
        return detalhe(fornecedorId);
    }

    /** Crédito de bonificação ainda disponível = total Uniplus − bonificação já usada. */
    @Transactional(readOnly = true)
    public BigDecimal bonificacaoDisponivel(Long fornecedorId) {
        BigDecimal total = fornecedorService.bonificacaoTotalDoFornecedor(fornecedorId);
        BigDecimal usada = repo.findByFornecedorIdOrderByCriadoEmAsc(fornecedorId).stream()
                .filter(l -> l.getTipo() == LancamentoTipo.BONIFICACAO)
                .map(LancamentoFornecedor::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disp = total.subtract(usada);
        return disp.signum() > 0 ? disp : BigDecimal.ZERO;
    }

    /** Aplica um valor FIFO (mais antigo primeiro) nas trocas em aberto; retorna o quanto foi efetivamente abatido. */
    private BigDecimal aplicarAbatimentoFifo(Long fornecedorId, BigDecimal valor) {
        BigDecimal restante = valor;
        BigDecimal aplicado = BigDecimal.ZERO;

        List<LancamentoFornecedor> ativosPositivos = repo.findByFornecedorIdOrderByCriadoEmAsc(fornecedorId).stream()
                .filter(l -> l.getStatus() == LancamentoStatus.ATIVO && l.getTipo().getSinal() > 0)
                .toList();

        for (LancamentoFornecedor l : ativosPositivos) {
            if (restante.signum() <= 0) break;

            BigDecimal abatido = l.getValorAbatido() != null ? l.getValorAbatido() : BigDecimal.ZERO;
            BigDecimal falta = l.getValor().subtract(abatido);
            if (falta.signum() <= 0) continue;

            if (restante.compareTo(falta) >= 0) {
                l.setValorAbatido(l.getValor());
                l.setStatus(LancamentoStatus.FINALIZADO);
                restante = restante.subtract(falta);
                aplicado = aplicado.add(falta);
            } else {
                BigDecimal residuo = falta.subtract(restante);
                if (residuo.compareTo(LIMIAR_CENTAVOS) <= 0) {
                    l.setValorAbatido(l.getValor());
                    l.setStatus(LancamentoStatus.FINALIZADO);
                    aplicado = aplicado.add(falta);
                } else {
                    l.setValorAbatido(abatido.add(restante));
                    aplicado = aplicado.add(restante);
                }
                restante = BigDecimal.ZERO;
            }
            repo.save(l);
        }
        return aplicado;
    }

    private BigDecimal saldoAtivo(List<LancamentoFornecedor> ls) {
        BigDecimal saldo = BigDecimal.ZERO;
        for (LancamentoFornecedor l : ls) {
            if (l.getStatus() == LancamentoStatus.ATIVO) {
                BigDecimal abatido = l.getValorAbatido() != null ? l.getValorAbatido() : BigDecimal.ZERO;
                BigDecimal restante = l.getValor().subtract(abatido);
                saldo = saldo.add(restante.multiply(BigDecimal.valueOf(l.getTipo().getSinal())));
            }
        }
        return saldo;
    }

    private LancamentoResponseDTO paraDTO(LancamentoFornecedor l) {
        BigDecimal abatido = l.getValorAbatido() != null ? l.getValorAbatido() : BigDecimal.ZERO;
        return LancamentoResponseDTO.builder()
                .id(l.getId())
                .tipo(l.getTipo().name())
                .valor(l.getValor())
                .valorAbatido(abatido)
                .restante(l.getValor().subtract(abatido))
                .sinal(l.getTipo().getSinal())
                .ean(l.getEan())
                .descricao(l.getDescricao())
                .produtoNome(l.getProdutoNome())
                .status(l.getStatus().name())
                .criadoEm(l.getCriadoEm())
                .build();
    }

    /** Gera o histórico do fornecedor em CSV (produtos, tipo, valor, status). */
    @Transactional(readOnly = true)
    public String historicoCsv(Long fornecedorId) {
        List<LancamentoFornecedor> ls = repo.findByFornecedorIdOrderByCriadoEmAsc(fornecedorId);
        StringBuilder sb = new StringBuilder();
        sb.append("Data;Tipo;Produto;EAN;Valor;Abatido;Restante;Status\n");
        for (LancamentoFornecedor l : ls) {
            BigDecimal abatido = l.getValorAbatido() != null ? l.getValorAbatido() : BigDecimal.ZERO;
            sb.append(l.getCriadoEm() != null ? l.getCriadoEm().toLocalDate() : "").append(';')
              .append(l.getTipo().name()).append(';')
              .append(csv(l.getProdutoNome() != null ? l.getProdutoNome() : l.getDescricao())).append(';')
              .append(csv(l.getEan())).append(';')
              .append(l.getValor()).append(';')
              .append(abatido).append(';')
              .append(l.getValor().subtract(abatido)).append(';')
              .append(l.getStatus().name()).append('\n');
        }
        return sb.toString();
    }

    private String csv(String v) {
        if (v == null) return "";
        return v.replace(';', ',').replace('\n', ' ');
    }
}
