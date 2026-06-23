package com.mercado.bi_api.repository;

import com.mercado.bi_api.entity.MetaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MetaFinanceiraRepository extends JpaRepository<MetaFinanceira, UUID> {
}
