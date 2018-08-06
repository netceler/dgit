package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_CONFIG;

import org.jooq.Record1;

import java.io.IOException;

import io.insight.jgit.services.KVConfigService;

public class JdbcConfigService implements KVConfigService {
    private final JdbcAdapter jdbcAdapter;

    public JdbcConfigService(final JdbcAdapter jdbcAdapter) {
        this.jdbcAdapter = jdbcAdapter;
    }

    @Override
    public String loadConfig(final String repositoryName) throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> {
            final Record1<String> result = dsl.select(GIT_CONFIG.CONFIG).from(GIT_CONFIG).where(
                    GIT_CONFIG.REPO.equal(repositoryName)).fetchOne();
            return result == null ? null : result.value1();
        });
    }

    @Override
    public void saveConfig(final String repositoryName, final String configText) throws IOException {
        jdbcAdapter.withDSLContext(
                dsl -> dsl.insertInto(GIT_CONFIG, GIT_CONFIG.REPO, GIT_CONFIG.CONFIG).values(repositoryName,
                        configText).onDuplicateKeyUpdate().set(GIT_CONFIG.CONFIG, configText).execute());
    }

}
