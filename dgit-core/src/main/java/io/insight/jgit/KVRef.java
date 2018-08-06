package io.insight.jgit;

import org.eclipse.jgit.lib.Ref;

public class KVRef {

    private final boolean symbolic;

    private final String name;

    private final String storageName;

    private final String targetName;

    private final String objectId;

    KVRef(final boolean symbolic, final String name, final String storageName, final String targetName,
            final String objectId) {
        this.symbolic = symbolic;
        this.name = name;
        this.storageName = storageName;
        this.targetName = targetName;
        this.objectId = objectId;
    }

    public static KVRef createSymbolic(final String name, final KVRef target) {
        if (target == null) {
            return new KVRef(true, name, null, null, null);
        }
        return new KVRef(true, name, null, target.name, null);
    }

    public static KVRef createSymbolic(final String name, final String targetName) {
        return new KVRef(true, name, null, targetName, null);
    }

    public static KVRef createObject(final String name, final String objectId, final String storage) {
        return new KVRef(false, name, storage, null, objectId);
    }

    public static KVRef toSymbolicKVRef(final String name, final Ref target) {
        if (target == null) {
            return new KVRef(true, name, null, null, null);
        }
        return createSymbolic(name, toKVRef(target));
    }

    public static KVRef toKVRef(final Ref ref) {
        if (ref == null) {
            return null;
        } else if (ref.isSymbolic()) {
            return toSymbolicKVRef(ref.getName(), ref.getTarget());
        } else {
            final String objectId = ref.getObjectId() == null ? null : ref.getObjectId().name();
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

    public Ref.Storage storage() {
        return Ref.Storage.valueOf(storageName);
    }

    public String getObjectId() {
        return objectId;
    }

    public String getTargetName() {
        return targetName;
    }

}
