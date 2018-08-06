package io.insight.jgit.jdbc;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import io.insight.jgit.services.KVAdapter;
import io.insight.jgit.services.KVConfigService;
import io.insight.jgit.services.KVObjectService;
import io.insight.jgit.services.KVRefService;

public abstract class JdbcAdapter implements KVAdapter {

    abstract SQLDialect dialect();

    protected <R> R withDSLContext(final ContextFunc<R> func) throws IOException {
        return withConnection(conn -> {
            final DSLContext context = DSL.using(conn, dialect());
            return func.apply(context);
        });
    }

    protected <R> R withConnection(final ConnectionFunc<R> func) throws IOException {
        try (Connection conn = getConnection()) {
            return func.apply(conn);
        } catch (final SQLException e) {
            throw new IOException(e);
        }
    }

    protected abstract Connection getConnection() throws SQLException;

    @Override
    public KVConfigService configService() {
        return new JdbcConfigService(this);
    }

    @Override
    public KVRefService refService() {
        return new JdbcRefService(this);
    }

    @Override
    public KVObjectService objService() {
        return new JdbcObjService(this);
    }

    @FunctionalInterface
    public interface ContextFunc<R> {
        R apply(DSLContext dsl) throws SQLException, IOException;
    }

    @FunctionalInterface
    public interface ConnectionFunc<R> {
        R apply(Connection connection) throws SQLException, IOException;
    }
}