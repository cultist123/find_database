package com.example.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

// ===========================================
// 本地数据源配置（记录统计结果到本地 PostgreSQL）
// ===========================================

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.backend.repository.local",
        entityManagerFactoryRef = "localEntityManagerFactory",
        transactionManagerRef = "localTransactionManager"
)
public class LocalDataSourceConfig {

    //从 application.properties 中读取 spring.datasource.local.* 配置
    @Bean
    @ConfigurationProperties("spring.datasource.local")
    public DataSourceProperties localDataSourceProperties() {
        return new DataSourceProperties();
    }

    //创建本地数据源连接池
    @Bean
    public DataSource localDataSource() {
        return localDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    //创建本地 EntityManagerFactory，只扫描 entity.local 包下的 Entity
    //ddl-auto=update：自动根据 Entity 类创建/更新本地表结构
    @Bean
    public LocalContainerEntityManagerFactoryBean localEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("localDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.backend.entity.local")
                .persistenceUnit("local")
                .properties(jpaProperties("update", "org.hibernate.dialect.PostgreSQLDialect"))
                .build();
    }

    //创建本地事务管理器
    @Bean
    public PlatformTransactionManager localTransactionManager(
            @Qualifier("localEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private Map<String, Object> jpaProperties(String ddlAuto, String dialect) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", dialect);
        return props;
    }
}