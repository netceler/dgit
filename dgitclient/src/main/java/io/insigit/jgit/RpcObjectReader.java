package io.insigit.jgit;

import io.insigit.jgit.services.RpcObjectService;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsReader;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class RpcObjectReader extends DfsReader {
  private final RpcObjectService service;
  private final RpcObjDatabase db;

  public RpcObjectReader(RpcObjDatabase db) {
    super(db);
    this.service = db.getObjectService();
    this.db = db;
  }

  @Override
  public ObjectReader newReader() {
    return db.getRepository().newObjectReader();
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) throws IOException {
    return service.resolve(id);
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint) throws MissingObjectException, IncorrectObjectTypeException, IOException {
    return service.open(objectId, typeHint);
  }

  @Override
  public boolean has(AnyObjectId objectId, int typeHint) throws IOException {
    return service.has(objectId, typeHint);
  }

  @Override
  public boolean has(AnyObjectId objectId) throws IOException {
    return has(objectId, OBJ_ANY);
  }

  @Override
  public Set<ObjectId> getShallowCommits()  {
    return service.getShallowCommits();
  }

  @Override
  public void close() {

  }

}
