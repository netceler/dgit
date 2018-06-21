package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.*;

import io.insight.jgit.services.KVAdapter;
import io.insight.jgit.services.KVConfigService;
import io.insight.jgit.services.KVObjectService;
import io.insight.jgit.services.KVRefService;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class JdbcAdapter implements KVAdapter {

  abstract SQLDialect dialect();

  protected <R> R withDSLContext(ContextFunc<R> func) throws IOException {
    return withConnection(conn -> {
      DSLContext context = DSL.using(conn, dialect());
      return func.apply(context);
    });
  }

  protected <R> R withConnection(ConnectionFunc<R> func) throws IOException {
    try (Connection conn = getConnection()) {
      return func.apply(conn);
    } catch (SQLException e) {
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
