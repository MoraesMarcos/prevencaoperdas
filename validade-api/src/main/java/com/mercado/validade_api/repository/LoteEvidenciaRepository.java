package com.mercado.validade_api.repository;

import com.mercado.validade_api.entity.LoteEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoteEvidenciaRepository extends JpaRepository<LoteEvidencia, UUID> {
    List<LoteEvidencia> findByLoteId(UUID loteId);
}
