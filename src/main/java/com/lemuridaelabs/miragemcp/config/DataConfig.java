package com.lemuridaelabs.miragemcp.config;

import com.lemuridaelabs.miragemcp.modules.alerts.dto.HoneyAlert;
import com.lemuridaelabs.miragemcp.modules.events.dto.HoneyEvent;
import com.lemuridaelabs.miragemcp.modules.notifications.dto.VapidKey;
import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.event.AfterSaveCallback;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Database configuration for Spring Data JDBC.
 *
 * <p>Configures an HSQLDB database for storing honeypot events, alerts,
 * push subscriptions, and VAPID keys. Supports both in-memory and file-based
 * persistence modes.</p>
 *
 * <p>By default, uses an in-memory database. Set the {@code DATABASE_PATH}
 * environment variable to a directory path to enable persistent storage.
 * The database files will be created in that directory.</p>
 *
 * @since 1.0
 */
@Configuration
@EnableJdbcRepositories(basePackages = "com.lemuridaelabs.miragemcp")
class DataConfig extends AbstractJdbcConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataConfig.class);

    @Value("${miragemcp.database.path:}")
    private String databasePath;

    /**
     * Provides a data source for database connections, either file-based or in-memory, depending on the
     * configuration of the application.
     * <p>
     * If a non-blank file-based database path is configured, it creates and configures a file-based
     * data source. Otherwise, it defaults to an in-memory data source.
     *
     * @return the configured DataSource instance, either file-based or in-memory
     */
    @Bean
    DataSource dataSource() {
        if (databasePath != null && !databasePath.isBlank()) {
            return createFileBasedDataSource();
        }
        return createInMemoryDataSource();
    }

    /**
     * Creates an in-memory {@link DataSource} using HSQL database.
     * Useful for lightweight testing or setups where persistent storage is not required.
     *
     * @return a configured in-memory {@link DataSource} instance
     */
    private DataSource createInMemoryDataSource() {
        log.info("Using in-memory database");
        return new EmbeddedDatabaseBuilder()
                .setScriptEncoding("UTF-8")
                .setName("miragemcpDb")
                .setType(EmbeddedDatabaseType.HSQL)
                .build();
    }

    /**
     * Creates and configures a file-based {@link DataSource} using HSQLDB.
     * The database is configured to be persisted to the file system at the specified path.
     * Additionally, a schema initialization script is executed on the database with error handling.
     *
     * @return a configured {@link DataSource} for file-based persistence
     */
    private DataSource createFileBasedDataSource() {
        Path dbPath = Path.of(databasePath, "miragemcp").toAbsolutePath();
        log.info("Using file-based database at: {}", dbPath);

        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL("jdbc:hsqldb:file:" + dbPath + ";hsqldb.write_delay=false");
        dataSource.setUser("SA");
        dataSource.setPassword("");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("/sql/schema.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);

        return dataSource;
    }

    /**
     * Provides a {@link NamedParameterJdbcOperations} bean for executing SQL queries with
     * named parameters through the configured {@link DataSource}.
     *
     * @param dataSource the {@link DataSource} to be used by the {@link NamedParameterJdbcTemplate}.
     *                   The data source is typically used for acquiring database connections.
     * @return a fully initialized {@link NamedParameterJdbcOperations} instance configured
     * with the provided {@link DataSource}.
     */
    @Bean
    NamedParameterJdbcOperations namedParameterJdbcOperations(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Configures and provides a transaction manager bean for managing database transactions.
     *
     * @param dataSource the DataSource to be used by the transaction manager
     * @return a configured instance of {@link TransactionManager} implemented as a {@link DataSourceTransactionManager}
     */
    @Bean
    TransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * Provides a callback to generate default values for a {@link HoneyEvent} before it is converted and saved.
     *
     * <p>This method ensures that if the event does not already have an ID, a new unique identifier
     * is assigned using {@link UUID#randomUUID()}. Additionally, if the event's score is null, it
     * defaults the score to 0.</p>
     *
     * @return a {@link BeforeConvertCallback} for {@link HoneyEvent} to populate missing ID and score values
     */
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

    /**
     * Callback method for generating a unique identifier for HoneyAlert entities before they are
     * converted and saved to the database.
     * <p>
     * Generates a new UUID and assigns it as the ID for a HoneyAlert entity if the ID is not already set.
     *
     * @return a BeforeConvertCallback for HoneyAlert entities, ensuring IDs are generated for new alerts.
     */
    @Bean
    BeforeConvertCallback<HoneyAlert> alertIdGeneratingCallback() {
        return (alert) -> {
            if (alert.getId() == null) {
                alert.setId(UUID.randomUUID().toString());
            }
            return alert;
        };
    }

    /**
     * Callback executed after saving a {@link VapidKey} entity.
     * <p>
     * The callback updates the state of the saved {@link VapidKey} instance by marking it as not new,
     * signaling that the entity has been persisted.
     *
     * @return an {@link AfterSaveCallback} that processes the {@link VapidKey} instance after it is saved
     */
    @Bean
    AfterSaveCallback<VapidKey> vapidKeyAfterSaveCallback() {
        return (vapidKey) -> {
            vapidKey.markNotNew();
            return vapidKey;
        };
    }

}