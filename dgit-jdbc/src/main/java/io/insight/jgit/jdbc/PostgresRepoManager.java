package io.insight.jgit.jdbc;

import org.jooq.SQLDialect;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class PostgresRepoManager extends JdbcRepoManager {

    private final DataSource ds;

    public PostgresJdbcAdapter jdbcAdapter;

    public PostgresRepoManager(final String jdbcUrl, final String user, final String password) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        ds = new HikariDataSource(config);
    }

    public PostgresRepoManager(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public JdbcAdapter adapter() {
        if (jdbcAdapter == null) {
            jdbcAdapter = new PostgresJdbcAdapter();
        }
        return jdbcAdapter;
    }

    private class PostgresJdbcAdapter extends JdbcAdapter {
        @Override
        SQLDialect dialect() {
            return SQLDialect.POSTGRES;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }
}
