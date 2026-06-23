package com.mercado.validade_api.repository;

import com.mercado.validade_api.entity.LancamentoFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LancamentoFornecedorRepository extends JpaRepository<LancamentoFornecedor, UUID> {
    List<LancamentoFornecedor> findByFornecedorIdOrderByCriadoEmAsc(Long fornecedorId);
}
