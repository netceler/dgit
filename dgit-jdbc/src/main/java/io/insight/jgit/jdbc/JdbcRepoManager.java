package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_CONFIG;
import static io.insight.jgit.jdbc.jooq.Tables.GIT_OBJECTS;
import static io.insight.jgit.jdbc.jooq.Tables.GIT_REFS;

import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

import io.insight.jgit.KVRepoManager;

public abstract class JdbcRepoManager implements KVRepoManager {

    @Override
    public boolean exists(final String name) throws IOException {
        if (!tableExists()) {
            return false;
        }
        return adapter().withDSLContext(dsl -> dsl.select(GIT_REFS.NAME).from(GIT_REFS).where(
                GIT_REFS.REPO.eq(name)).fetchAny() != null);
    }

    private boolean tableExists() throws IOException {
        try {
            adapter().withDSLContext(dsl -> dsl.selectCount().from(GIT_REFS));
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public Repository create(final String name) throws IOException {
        final Repository repo = open(name);
        repo.create();
        return repo;
    }

    @Override
    public void delete(final String name) throws IOException {
        adapter().withDSLContext(dsl -> {
            dsl.deleteFrom(GIT_REFS).where(GIT_REFS.REPO.eq(name)).execute();
            dsl.deleteFrom(GIT_CONFIG).where(GIT_CONFIG.REPO.eq(name)).execute();
            dsl.deleteFrom(GIT_OBJECTS).where(GIT_OBJECTS.REPO.eq(name)).execute();
            return null;
        });
    }

    @Override
    public abstract JdbcAdapter adapter();
}