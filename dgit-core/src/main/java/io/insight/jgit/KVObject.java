package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;

public class KVObject {
  private final String objectId;
  private final int type;
  private final long size;
  private long totalSize;
  private KVObject base;

  public KVObject(String objectId, int type, long size, long totalSize) {
    this.objectId = objectId;
    this.type = type;
    this.size = size;
    this.totalSize = totalSize;
  }

  public String getObjectId() {
    return objectId;
  }


  public int getType() {
    return type;
  }

  public long getSize() {
    return size;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public KVObject getBase() {
    return base;
  }

  public void setBase(KVObject base) {
    this.base = base;
  }

}
