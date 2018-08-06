package io.insight.jgit.jdbc;

import org.jooq.SQLDialect;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class MysqlRepoManager extends JdbcRepoManager {

    private final DataSource ds;

    public MysqlJdbcAdapter jdbcAdapter;

    public MysqlRepoManager(final String jdbcUrl, final String user, final String password) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        ds = new HikariDataSource(config);
    }

    public MysqlRepoManager(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public JdbcAdapter adapter() {
        if (jdbcAdapter == null) {
            jdbcAdapter = new MysqlJdbcAdapter();
        }
        return jdbcAdapter;
    }

    private class MysqlJdbcAdapter extends JdbcAdapter {
        @Override
        SQLDialect dialect() {
            return SQLDialect.MYSQL;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }
}
