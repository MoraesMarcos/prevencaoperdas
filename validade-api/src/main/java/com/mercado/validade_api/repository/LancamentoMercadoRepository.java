package com.mercado.validade_api.repository;

import com.mercado.validade_api.entity.LancamentoMercado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LancamentoMercadoRepository extends JpaRepository<LancamentoMercado, UUID> {
    List<LancamentoMercado> findAllByOrderByDataDesc();

    // Idempotência da rebaixa de validade lançada no mercado (evita duplicar por lote).
    boolean existsByLoteId(UUID loteId);
}
