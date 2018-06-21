package io.insight.jgit;


import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.SymbolicRef;

public class KVRef {

  private final boolean symbolic;
  private final String name;
  private final String storageName;
  private final String targetName;
  private final String objectId;

  KVRef(boolean symbolic, String name, String storageName, String targetName, String objectId) {
    this.symbolic = symbolic;
    this.name = name;
    this.storageName = storageName;
    this.targetName = targetName;
    this.objectId = objectId;
  }

  public static KVRef createSymbolic(String name, KVRef target) {
    if (target == null) {
      return new KVRef(true, name, null, null, null);
    }
    return new KVRef(true, name, null, target.name, null);
  }

  public static KVRef createSymbolic(String name, String targetName) {
    return new KVRef(true, name, null, targetName, null);
  }

  public static KVRef createObject(String name, String objectId, String storage) {
    return new KVRef(false, name, storage, null, objectId);
  }

  public static KVRef toSymbolicKVRef(String name, Ref target) {
    if (target == null) {
      return new KVRef(true, name, null, null, null);
    }
    return createSymbolic(name, toKVRef(target));
  }



  public static KVRef toKVRef(Ref ref) {
    if (ref == null) {
      return null;
    } else if (ref.isSymbolic()) {
      return toSymbolicKVRef(ref.getName(), ref.getTarget());
    } else {
      String objectId = ref.getObjectId() ==null ? null : ref.getObjectId().name();
      return createObject(ref.getName(), objectId, ref.getStorage().name());
    }
  }

  public String getName() {
    return name;
  }

  public boolean isSymbolic() {
    return symbolic;
  }

  public String getStorageName() {
    return storageName;
  }

  public Ref.Storage storage(){
    return Ref.Storage.valueOf(storageName);
  }

  public String getObjectId() {
    return objectId;
  }


  public String getTargetName() {
    return targetName;
  }

}
