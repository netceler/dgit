package io.insight.jgit.server.services;

import io.insigit.jgit.services.RpcObjectService;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public class ObjectServiceImpl implements RpcObjectService {
  private Repository repo;

  public ObjectServiceImpl(Repository repository) {
    repo = repository;
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) {
    return null;
  }

  @Override
  public Set<ObjectId> getShallowCommits() {
    try {
      return repo.getObjectDatabase().newReader().getShallowCommits();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException {
    return repo.getObjectDatabase().open(objectId, typeHint);
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream in) throws IOException {
    return repo.getObjectDatabase().newInserter().insert(objectType, length, in);
  }
}
