package io.insight.jgit.server.services;

import io.insight.jgit.Inserter;
import io.insigit.jgit.RpcObjDatabase;
import io.insigit.jgit.services.RpcObjectService;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public class ObjectServiceImpl implements RpcObjectService<Inserter> {
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
  public ObjectId insert(Inserter inserterId, int objectType, long length, InputStream in) throws IOException {
    return repo.getObjectDatabase().newInserter().insert(objectType, length, in);
  }

  @Override
  public PackParser newPackParser(Inserter inserterId, RpcObjDatabase odb, InputStream in) throws IOException {
    return repo.getObjectDatabase().newInserter().newPackParser(in);
  }

  @Override
  public ObjectInserter newInserter(RpcObjDatabase odb) {
    return repo.newObjectInserter();
  }

  @Override
  public boolean has(AnyObjectId objectId, int typeHint) throws IOException {
    try (final ObjectReader or = repo.getObjectDatabase().newReader()) {
      return or.has(objectId, typeHint);
    }
  }
}
