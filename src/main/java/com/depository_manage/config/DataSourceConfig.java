package com.depository_manage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Bean
    @ConfigurationProperties("spring.datasources.cpck")
    public DataSourceProperties cpckDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource cpckDataSource() {
        return cpckDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasources.clck")
    public DataSourceProperties clckDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource clckDataSource() {
        return clckDataSourceProperties().initializeDataSourceBuilder().build();
    }
}
