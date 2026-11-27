package com.swizima.stafftrackapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ensures the target PostgreSQL database (e.g. {@code swizima_stafftrack}) exists before
 * HikariCP / JPA initializes, preventing container crash-loops on servers where PostgreSQL
 * was already initialized before {@code stafftrackapi} was added.
 */
@Slf4j
@Component
public class DatabaseAutoCreator implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private static final Pattern PG_URL_PATTERN =
            Pattern.compile("^(jdbc:postgresql://[^/]+)/([a-zA-Z_][a-zA-Z0-9_]*)(\\?.*)?$");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (environment == null) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url");
        String user = environment.getProperty("spring.datasource.username", "postgres");
        String pass = environment.getProperty("spring.datasource.password", "postgres");
        if (url != null && !url.isBlank()) {
            ensureDatabaseExists(url, user, pass);
        }
    }

    private void ensureDatabaseExists(String jdbcUrl, String username, String password) {
        Matcher m = PG_URL_PATTERN.matcher(jdbcUrl.trim());
        if (!m.matches()) {
            return;
        }
        String serverPrefix = m.group(1);
        String targetDb = m.group(2);
        String querySuffix = m.group(3) != null ? m.group(3) : "";
        String adminUrl = serverPrefix + "/postgres" + querySuffix;

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password);
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, targetDb);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE DATABASE \"" + targetDb + "\"");
                        log.info("Auto-created missing PostgreSQL database '{}'", targetDb);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Database auto-creation check skipped for '{}': {}", targetDb, ex.getMessage());
        }
    }
}
