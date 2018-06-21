package io.insight.jgit.jdbc;

import io.insight.jgit.KVRepoManager;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

import static io.insight.jgit.jdbc.jooq.Tables.*;

public abstract class JdbcRepoManager implements KVRepoManager {

  @Override
  public boolean exists(String name) throws IOException {
    return adapter().withDSLContext(dsl ->
        dsl.select(GIT_REFS.NAME).from(GIT_REFS).where(GIT_REFS.REPO.eq(name)).fetchAny() != null);
  }

  @Override
  public Repository create(String name) throws IOException {
    Repository repo = open(name);
    repo.create();
    return repo;
  }

  @Override
  public void delete(String name) throws IOException {
    adapter().withDSLContext(dsl -> {
      dsl.deleteFrom(GIT_REFS).where(GIT_REFS.REPO.eq(name)).execute();
      dsl.deleteFrom(GIT_CONFIG).where(GIT_CONFIG.REPO.eq(name)).execute();
      dsl.deleteFrom(GIT_OBJECTS).where(GIT_OBJECTS.REPO.eq(name)).execute();
      return null;
    });
  }


  public abstract JdbcAdapter adapter();
 
}
