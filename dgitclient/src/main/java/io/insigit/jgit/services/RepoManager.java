package io.insigit.jgit.services;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;

import java.io.IOException;

public interface RepoManager extends RepositoryResolver<Object> {
  default Repository open(Object req, String name) throws RepositoryNotFoundException,
      ServiceMayNotContinueException {
    if (!exists(name)) {
      throw new RepositoryNotFoundException(name);
    }
    return open(name);
  }

  Repository open(String name) throws RepositoryNotFoundException;

  boolean exists(String name);

  Repository create(String name);

  void delete(String name) throws IOException;

}
