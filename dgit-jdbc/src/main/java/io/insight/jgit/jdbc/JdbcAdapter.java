package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.*;

import io.insight.jgit.services.KVAdapter;
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


  protected <R> R withTransaction(ConnectionFunc<R> func) throws IOException {
    return withConnection(conn -> {
      conn.setAutoCommit(false);
      try {
        R result = func.apply(conn);
        conn.commit();
        return result;
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    });
  }


  @Override
  public String loadConfig(String repositoryName) throws IOException {
    return withDSLContext(dsl -> {
      Record1<String> result =
          dsl.select(GIT_CONFIG.CONFIG).from(GIT_CONFIG).where(GIT_CONFIG.REPO.equal(repositoryName)).fetchOne();
      return result == null ? null : result.value1();
    });
  }

  @Override
  public void saveConfig(String repositoryName, String configText) throws IOException {
    withDSLContext(dsl ->
        dsl.insertInto(GIT_CONFIG, GIT_CONFIG.REPO, GIT_CONFIG.CONFIG).values(repositoryName, configText)
            .onDuplicateKeyUpdate()
            .set(GIT_CONFIG.CONFIG, configText).execute());
  }

  @Override
  public KVRefService refService(String repositoryName) {
    return new JdbcRefService(this, repositoryName);
  }

  @Override
  public KVObjectService objService(String repositoryName) {
    return new JdbcObjService(this, repositoryName);
  }

  @FunctionalInterface
  public interface SQLFunc<R> {
    R apply(PreparedStatement statement) throws SQLException;
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
