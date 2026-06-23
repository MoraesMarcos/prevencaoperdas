package com.mercado.bi_api.repository;

import com.mercado.bi_api.entity.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    List<Despesa> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);
}
