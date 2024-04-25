package com.depository_manage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean
    public JdbcTemplate cpckJdbcTemplate(@Qualifier("cpckDataSource") DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate clckJdbcTemplate(@Qualifier("clckDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
