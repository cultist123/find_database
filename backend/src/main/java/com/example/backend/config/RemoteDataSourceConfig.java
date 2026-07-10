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
// 远程数据源配置（主数据源，查询 ts_device_data_format）
// ===========================================


@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.backend.repository.remote",
        entityManagerFactoryRef = "remoteEntityManagerFactory",
        transactionManagerRef = "remoteTransactionManager"
)


public class RemoteDataSourceConfig {

    //从 application.properties 中读取 spring.datasource.remote.* 配置
    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.remote")
    public DataSourceProperties remoteDataSourceProperties() {
        return new DataSourceProperties();
    }

    //创建远程数据源连接池，标记为 @Primary 表示默认数据源
    @Primary
    @Bean
    public DataSource remoteDataSource() {
        return remoteDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    //创建远程 EntityManagerFactory，负责把 Entity 类映射到远程数据库的表
    //basePackages 指定只扫描 entity.remote 包下的 Entity
    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean remoteEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("remoteDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.backend.entity.remote")
                .persistenceUnit("remote")
                .properties(jpaProperties("none", "org.hibernate.dialect.PostgreSQLDialect"))
                .build();
    }

    //创建远程事务管理器，Spring 用它控制远程数据库的事务（提交/回滚）
    @Primary
    @Bean
    public PlatformTransactionManager remoteTransactionManager(
            @Qualifier("remoteEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    //通用的 JPA 属性设置方法，接收 ddl-auto 策略和数据库方言
    private Map<String, Object> jpaProperties(String ddlAuto, String dialect) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.dialect", dialect);
        return props;
    }
}