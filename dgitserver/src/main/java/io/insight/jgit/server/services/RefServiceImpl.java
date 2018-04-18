package io.insight.jgit.server.services;

import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class RefServiceImpl implements RpcRefService {

  private Repository repo;

  public RefServiceImpl(Repository repo) {
    this.repo = repo;
  }

  @Override
  public Collection<Ref> getAll() {
    Map<String, Ref> allRefs = repo.getAllRefs();
    if (allRefs.isEmpty()) {
      // for a newly created repo, "HEAD" will point to a ZERO objectId which will be removed from allRefs
      // so we should add back it
      return Collections.singleton(new SymbolicRef(Constants.HEAD,
          new ObjectIdRef.Unpeeled(Ref.Storage.NEW, Constants.R_HEADS + Constants.MASTER, null)));
    }
    return allRefs.values();
  }

  @Override
  public boolean compareAndPut(Ref oldRef, Ref newRef) {
    try {
      Ref ref = repo.exactRef(oldRef.getName());
      if (equalsRef(ref, oldRef)) {
        RefUpdate update = repo.updateRef(oldRef.getName());
        RefUpdate.Result result;
        if (newRef.isSymbolic()) {
          result = update.link(newRef.getTarget().getName());
        } else {
          if(ref!=null && ref.isSymbolic())
            update.setDetachingSymbolicRef();
          update.setExpectedOldObjectId(oldRef.getObjectId());
          update.setNewObjectId(newRef.getObjectId());
          result = update.forceUpdate();
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
      return oldRef.getStorage() == Ref.Storage.NEW;
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
