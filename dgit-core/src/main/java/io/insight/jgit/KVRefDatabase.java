package io.insight.jgit;

import io.insight.jgit.internal.DfsRefDatabase;
import io.insight.jgit.services.KVAdapter;
import io.insight.jgit.services.KVRefService;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.util.RefList;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class KVRefDatabase extends DfsRefDatabase {

  private final KVRefService refService;
  public String repositoryName;

  protected KVRefDatabase(KVRepository repo, KVAdapter adapter) {
    super(repo);
    repositoryName = repo.getRepositoryName();
    this.refService = adapter.refService(repositoryName);
  }

  @Override
  protected RefCache scanAllRefs() throws IOException {
    Map<String, KVRef> all = refService.getAllRefs().stream().
        collect(Collectors.toMap(KVRef::getName, o -> o));
    RefList.Builder<Ref> ids = new RefList.Builder<>();
    RefList.Builder<Ref> sym = new RefList.Builder<>();

    for (KVRef r : all.values()) {
      Ref ref = toRef(r, all);
      if (ref.isSymbolic()) {
        sym.add(ref);
      }
      ids.add(ref);
    }
    ids.sort();
    sym.sort();
    return new RefCache(ids.toRefList(), sym.toRefList());
  }

  private Ref toRef(KVRef r, Map<String, KVRef> all) {
    if (r.isSymbolic()) {
      KVRef t = all.get(r.getTargetName());
      Ref target;
      if (t != null) {
        target = toRef(t, all);
      } else {
        target = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, r.getTargetName(), null);
      }
      return new SymbolicRef(r.getName(), target);
    } else {
      Ref.Storage storage = Ref.Storage.valueOf(r.getStorageName());
      if (r.getObjectId() == null) {
        return new ObjectIdRef.Unpeeled(storage, r.getName(), null);
      } else {
        return new ObjectIdRef.PeeledNonTag(storage, r.getName(), ObjectId.fromString(r.getObjectId()));
      }
    }
  }

  @Override
  protected boolean compareAndPut(Ref old, Ref newRef) throws IOException {
    return refService.compareAndPut(old, newRef);
  }

  @Override
  protected boolean compareAndRemove(Ref old) throws IOException {
    return refService.compareAndRemove(old);
  }


}
