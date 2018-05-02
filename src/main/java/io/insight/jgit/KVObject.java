package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;

public class KVObject {
  private final boolean isPacked;
  private final String objectId;
  private final int type;
  private final int size;
  private KVObject base;

  public KVObject(boolean isPacked, String objectId, int type, int size) {
    this.isPacked = isPacked;
    this.objectId = objectId;
    this.type = type;
    this.size = size;
  }

  public String getObjectId() {
    return objectId;
  }

  public boolean isPacked() {
    return isPacked;
  }

  public int getType() {
    return type;
  }

  public int getSize() {
    return size;
  }

  public KVObject getBase() {
    return base;
  }

  public void setBase(KVObject base) {
    this.base = base;
  }

  public byte[] resolveDelta(KVObjectService objectService) {
    byte[] data = getObjectData(objectService);
    if (base == null) {
      return data;
    } else {
      byte[] baseData = getBase().resolveDelta(objectService);
      return BinaryDelta.apply(baseData, data);
    }
  }

  private byte[] getObjectData(KVObjectService objectService) {
    return objectService.getObjectData(objectId);
  }
}
