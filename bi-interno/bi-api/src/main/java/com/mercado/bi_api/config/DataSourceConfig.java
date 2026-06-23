package com.mercado.bi_api.config;

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
     * Datasource principal (proprio do bi-interno: despesas, categorias, usuarios, metas).
     * Marcado @Primary para que o Spring Boot use este para JPA/Hibernate automaticamente.
     */
    @Primary
    @Bean(name = "appDataSource")
    @ConfigurationProperties(prefix = "app.datasource")
    public DataSource appDataSource() {
        return new HikariDataSource();
    }

    /**
     * Datasource da ERP (Uniplus) com usuario somente-leitura.
     * Acesso via JdbcTemplate puro (sem JPA) -- nunca usar para escrita.
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
