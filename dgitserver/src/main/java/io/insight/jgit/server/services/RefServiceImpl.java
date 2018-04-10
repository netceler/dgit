package io.insight.jgit.server.services;

import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.Collection;

public class RefServiceImpl implements RpcRefService {

  private Repository repo;

  public RefServiceImpl(Repository repo) {
    this.repo = repo;
  }

  @Override
  public Collection<Ref> getAll() {
    return repo.getAllRefs().values();
  }

  @Override
  public boolean compareAndPut(Ref oldRef, Ref newRef) {
    try {
      Ref ref = repo.exactRef(oldRef.getName());
      if (equalsRef(ref, oldRef)) {
        RefUpdate update = repo.updateRef(ref.getName());
        RefUpdate.Result result;
        if (newRef.isSymbolic()) {
          result = update.link(newRef.getTarget().getName());
        } else {
          update.setExpectedOldObjectId(oldRef.getObjectId());
          update.setNewObjectId(newRef.getObjectId());
          result = update.update();
        }
        return handleRefResult(result);
      } else {
        return false;
      }
    } catch (IOException e) {
      return false;
    }
  }

  private boolean handleRefResult(RefUpdate.Result result) {
    switch (result) {
      case NOT_ATTEMPTED:
      case NO_CHANGE:
      case NEW:
      case FORCED:
      case RENAMED:
      case FAST_FORWARD:
        return true;
      default:
        return false;
    }
  }

  private boolean equalsRef(Ref ref, Ref oldRef) {
    if (ref == null) {
      return oldRef.getStorage() == Ref.Storage.NEW && oldRef.getTarget() == null;
    } else {
      if (ref.isSymbolic()) {
        return oldRef.isSymbolic() && ref.getTarget().getName().equals(oldRef.getTarget().getName());
      } else {
        return !oldRef.isSymbolic() && ref.getObjectId().equals(oldRef.getObjectId());
      }
    }
  }

  @Override
  public boolean compareAndRemove(Ref oldRef) {
    try {
      Ref ref = repo.exactRef(oldRef.getName());
      if (equalsRef(ref, oldRef)) {
        RefUpdate.Result result = repo.updateRef(ref.getName()).delete();
        return handleRefResult(result);
      }
    } catch (IOException e) {
      return false;
    }
    return false;
  }
}
