package io.insight.jgit.jdbc;

import org.jooq.SQLDialect;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class SqlRepoManager extends JdbcRepoManager {

    private final DataSource ds;

    public JdbcAdapter jdbcAdapter;

    public SqlRepoManager(final String jdbcUrl, final String user, final String password) {
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        ds = new HikariDataSource(config);
        jdbcAdapter = new HsqldbJdbcAdapter();
        if (jdbcUrl.contains("oracle")) {
            jdbcAdapter = new OracledbcAdapter();
        }
        if (jdbcUrl.contains("postgres")) {
            jdbcAdapter = new PostgresJdbcAdapter();
        }
        if (jdbcUrl.contains("mysql")) {
            jdbcAdapter = new MysqlJdbcAdapter();
        }
    }

    @Override
    public JdbcAdapter adapter() {
        return jdbcAdapter;
    }

    private class OracledbcAdapter extends JdbcAdapter {
        @Override
        protected SQLDialect dialect() {
            return SQLDialect.ORACLE12C;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }

    private class PostgresJdbcAdapter extends JdbcAdapter {
        @Override
        protected SQLDialect dialect() {
            return SQLDialect.POSTGRES;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }

    private class MysqlJdbcAdapter extends JdbcAdapter {
        @Override
        protected SQLDialect dialect() {
            return SQLDialect.MYSQL;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }

    private class HsqldbJdbcAdapter extends JdbcAdapter {
        @Override
        protected SQLDialect dialect() {
            return SQLDialect.HSQLDB;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return ds.getConnection();
        }
    }
}