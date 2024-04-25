package com.depository_manage.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.ResourceTransactionManager;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Configuration
    @MapperScan(basePackages = "com.depository_manage.mapper.cpck", sqlSessionFactoryRef = "cpckSqlSessionFactory")
    public static class MyBatisConfigCpck {

        @Bean
        public SqlSessionFactory cpckSqlSessionFactory(@Qualifier("cpckDataSource") DataSource dataSource) throws Exception {
            SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
            sessionFactory.setDataSource(dataSource);
            sessionFactory.setMapperLocations(
                    new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/cpck/*.xml")
            );
            return sessionFactory.getObject();
        }


        @Bean
        public PlatformTransactionManager cpckTransactionManager(@Qualifier("cpckDataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Configuration
    @MapperScan(basePackages = "com.depository_manage.mapper.clck", sqlSessionFactoryRef = "clckSqlSessionFactory")
    public static class MyBatisConfigClck {

        @Bean
        public SqlSessionFactory clckSqlSessionFactory(@Qualifier("clckDataSource") DataSource dataSource) throws Exception {
            SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
            sessionFactory.setDataSource(dataSource);
            sessionFactory.setMapperLocations(
                    new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/clck/*.xml")
            );
            return sessionFactory.getObject();
        }

        @Bean
        public PlatformTransactionManager clckTransactionManager(@Qualifier("clckDataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
