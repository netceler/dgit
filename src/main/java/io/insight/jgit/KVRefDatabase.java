package io.insight.jgit;

import io.insight.jgit.services.KVAdapter;
import io.insight.jgit.services.KVRefService;
import org.eclipse.jgit.internal.storage.dfs.DfsRefDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.util.RefList;

import java.io.IOException;
import java.util.Map;

public class KVRefDatabase extends DfsRefDatabase {

  private final KVRefService refService;
  public String repositoryName;

  protected KVRefDatabase(DfsRepository repo, KVAdapter adapter) {
    super(repo);
    repositoryName = this.getRepository().getDescription().getRepositoryName();
    this.refService = adapter.refService(repositoryName);
  }

  @Override
  protected RefCache scanAllRefs() throws IOException {
    Map<String, KVRef> all = refService.getAllRefs();
    RefList.Builder<Ref> ids = new RefList.Builder<>();
    RefList.Builder<Ref> sym = new RefList.Builder<>();
    for (KVRef r : all.values()) {
      Ref ref = r.toRef();
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
    KVRef old = KVRef.toKVRef(oldRef);
    KVRef nw = KVRef.toKVRef(newRef);
    return refService.compareAndPut(old, nw);
  }

  @Override
  protected boolean compareAndRemove(Ref oldRef) throws IOException {
    KVRef old = KVRef.toKVRef(oldRef);
    return refService.compareAndRemove(old);
  }


}
