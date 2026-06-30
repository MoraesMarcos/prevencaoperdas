package com.mercado.validade_api.repository;

import com.mercado.validade_api.entity.LancamentoFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LancamentoFornecedorRepository extends JpaRepository<LancamentoFornecedor, UUID> {
    List<LancamentoFornecedor> findByFornecedorIdOrderByCriadoEmAsc(Long fornecedorId);

    boolean existsByLoteId(UUID loteId);

    // Para o dashboard saber, em lote, quais lotes já têm rebaixa gerada.
    @org.springframework.data.jpa.repository.Query("select l.loteId from LancamentoFornecedor l where l.loteId is not null")
    Set<UUID> findLoteIdsComRebaixa();
}
