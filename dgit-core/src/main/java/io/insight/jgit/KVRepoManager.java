package io.insight.jgit;

import io.insight.jgit.services.KVAdapter;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.util.FS;

import java.io.IOException;

public interface KVRepoManager extends RepositoryResolver<DaemonClient> {
  default Repository open(DaemonClient req, String name) throws RepositoryNotFoundException,
      ServiceMayNotContinueException {
    try {
      if (!exists(name)) {
        throw new RepositoryNotFoundException(name);
      }
    } catch (IOException e) {
      throw new ServiceMayNotContinueException(e);
    }
    return open(name);
  }

  default Repository open(String name) throws RepositoryNotFoundException {
    try {
      if(!exists(name)) {
        throw new RepositoryNotFoundException(name);
      }
    } catch (IOException e) {
      throw new RepositoryNotFoundException(name, e);
    }
    KVRepositoryBuilder options = new KVRepositoryBuilder();
    options.setRepositoryName(name);
    options.setFS(FS.detect());
    return new KVRepository(this, options);
  }

  boolean exists(String name) throws IOException;

  Repository create(String name) throws IOException;

  void delete(String name) throws IOException;

  KVAdapter adapter();
}
