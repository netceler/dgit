package io.insigit.jgit;

import io.insigit.jgit.services.RpcObjectService;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsReader;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class RpcObjectReader extends DfsReader {
  private final RpcObjectService service;

  public RpcObjectReader(RpcObjDatabase db) {
    super(db);
    this.service = db.getObjectService();
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
  public Set<ObjectId> getShallowCommits()  {
    return service.getShallowCommits();
  }

  @Override
  public void close() {

  }

}
