package io.insight.jgit.jdbc;

import io.insight.jgit.services.KVConfigService;
import org.jooq.Record1;

import java.io.IOException;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_CONFIG;

public class JdbcConfigService implements KVConfigService {
  private JdbcAdapter jdbcAdapter;

  public JdbcConfigService(JdbcAdapter jdbcAdapter) {
    this.jdbcAdapter = jdbcAdapter;
  }


  @Override
  public String loadConfig(String repositoryName) throws IOException {
    return jdbcAdapter.withDSLContext(dsl -> {
      Record1<String> result =
          dsl.select(GIT_CONFIG.CONFIG).from(GIT_CONFIG).where(GIT_CONFIG.REPO.equal(repositoryName)).fetchOne();
      return result == null ? null : result.value1();
    });
  }

  @Override
  public void saveConfig(String repositoryName, String configText) throws IOException {
    jdbcAdapter.withDSLContext(dsl ->
        dsl.insertInto(GIT_CONFIG, GIT_CONFIG.REPO, GIT_CONFIG.CONFIG).values(repositoryName, configText)
            .onDuplicateKeyUpdate()
            .set(GIT_CONFIG.CONFIG, configText).execute());
  }

}
