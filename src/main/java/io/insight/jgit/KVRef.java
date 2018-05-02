package io.insight.jgit;


import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.SymbolicRef;

public class KVRef {

  private final boolean symbolic;
  private String name;
  private final String storage;
  private final KVRef target;
  private final String objectId;

  public KVRef(boolean symbolic, String name,
               @Nullable KVRef target,
               @Nullable String storage,
               @Nullable String objectId) {
    this.symbolic = symbolic;
    this.name = name;
    this.storage = storage;
    this.target = target;
    this.objectId = objectId;
  }

  public static KVRef toSymbolicKVRef(String name, Ref target) {
    if (target == null) {
      return new KVRef(true, name, null, Ref.Storage.NEW.name(), null);
    }
    return new KVRef(true, name, toKVRef(target), null, null);
  }

  public static KVRef toObjectKVRef(String name, String storage, String objectId) {
    return new KVRef(false, name, null, storage, objectId);
  }

  public static KVRef toKVRef(Ref ref) {
    if (ref == null) {
      return null;
    } else if (ref.isSymbolic()) {
      return toSymbolicKVRef(ref.getName(), ref.getTarget());
    } else {
      return toObjectKVRef(ref.getName(), ref.getStorage().name(), ref.getObjectId().name());
    }
  }

  public boolean isSymbolic() {
    return symbolic;
  }

  public String getStorage() {
    return storage;
  }

  public KVRef getTarget() {
    return target;
  }

  public String getObjectId() {
    return objectId;
  }

  public Ref toRef() {
    if (symbolic) {
      if (target == null) {
        ObjectIdRef.Unpeeled t = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, null, null);
        return new SymbolicRef(name, t);
      } else {
        return new SymbolicRef(name, target.toRef());
      }
    } else {
      Ref.Storage storage = Ref.Storage.valueOf(this.storage);
      if (objectId == null) {
        return new ObjectIdRef.Unpeeled(storage, name, null);
      } else {
        return new ObjectIdRef.PeeledNonTag(storage, name, ObjectId.fromString(objectId));
      }
    }
  }
}
