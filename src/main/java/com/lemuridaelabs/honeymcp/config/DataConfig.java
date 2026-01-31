package com.lemuridaelabs.honeymcp.config;

import com.lemuridaelabs.honeymcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.honeymcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.honeymcp.modules.notifications.dto.VapidKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.AfterSaveCallback;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
@EnableJdbcRepositories(basePackages = "com.lemuridaelabs.honeymcp")
class DataConfig extends AbstractJdbcConfiguration {

    @Bean
    DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setScriptEncoding("UTF-8")
                .setName("honeyMcpDb")
                .setType(EmbeddedDatabaseType.HSQL)
                .build();
    }

    @Bean
    NamedParameterJdbcOperations namedParameterJdbcOperations(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    TransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    BeforeConvertCallback<HoneyEvent> eventIdGeneratingCallback() {
        return (event) -> {
            if (event.getId() == null) {
                event.setId(UUID.randomUUID().toString());
            }
            if (event.getScore() == null) {
                event.setScore(0);
            }
            return event;
        };
    }

    @Bean
    BeforeConvertCallback<HoneyAlert> alertIdGeneratingCallback() {
        return (alert) -> {
            if (alert.getId() == null) {
                alert.setId(UUID.randomUUID().toString());
            }
            return alert;
        };
    }

    @Bean
    AfterSaveCallback<VapidKey> vapidKeyAfterSaveCallback() {
        return (vapidKey) -> {
            vapidKey.markNotNew();
            return vapidKey;
        };
    }

}