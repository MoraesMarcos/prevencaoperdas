package com.mercado.validade_api.repository;

import com.mercado.validade_api.entity.LoteCaptura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoteCapturaRepository extends JpaRepository<LoteCaptura, UUID> {
    // Para o painel: lotes ordenados pelo vencimento mais próximo
    List<LoteCaptura> findAllByOrderByDataVencimentoAsc();
}
