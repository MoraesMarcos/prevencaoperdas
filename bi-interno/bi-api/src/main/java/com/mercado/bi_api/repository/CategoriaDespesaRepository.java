package com.mercado.bi_api.repository;

import com.mercado.bi_api.entity.CategoriaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, UUID> {
}
