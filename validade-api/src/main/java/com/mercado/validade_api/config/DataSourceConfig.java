package com.mercado.validade_api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    /**
     * Datasource principal (Supabase): produtos e lotes de captura.
     * @Primary para que o JPA/Hibernate use este automaticamente.
     */
    @Primary
    @Bean(name = "appDataSource")
    @ConfigurationProperties(prefix = "app.datasource")
    public DataSource appDataSource() {
        return new HikariDataSource();
    }

    /**
     * Datasource da ERP Uniplus com usuario somente-leitura.
     * Acesso via JdbcTemplate puro (sem JPA) -- usado so para buscar produto pelo EAN.
     */
    @Bean(name = "erpDataSource")
    @ConfigurationProperties(prefix = "erp.datasource")
    public DataSource erpDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "erpJdbcTemplate")
    public JdbcTemplate erpJdbcTemplate(@Qualifier("erpDataSource") DataSource erpDataSource) {
        return new JdbcTemplate(erpDataSource);
    }
}
