package io.insigit.jgit;

import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.util.RefList;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class RpcRefDatabase extends DfsRefDatabase {

  protected RpcRefDatabase(RpcRepository repository) {
    super(repository);
  }

  @Override
  protected RpcRepository getRepository() {
    return (RpcRepository) super.getRepository();
  }

  public RpcRefService getRefService(){
    return getRepository().getRefService();
  }

  @Override
  protected RefCache scanAllRefs() throws IOException {
    Collection<Ref> all = getRefService().getAll();
    RefList.Builder<Ref> ids = new RefList.Builder<>();
    RefList.Builder<Ref> sym = new RefList.Builder<>();
    for (Ref ref : all) {
      if (ref.isSymbolic()) {
        sym.add(ref);
        ids.add(ref);
      } else {
        ids.add(ref);
      }
    }
    ids.sort();
    sym.sort();
    return new RefCache(ids.toRefList(), sym.toRefList());
  }

  @Override
  protected boolean compareAndPut(Ref oldRef, Ref newRef) throws IOException {
    return getRefService().compareAndPut(oldRef,newRef);
  }

  @Override
  protected boolean compareAndRemove(Ref oldRef) throws IOException {
    return getRefService().compareAndRemove(oldRef);
  }
}
