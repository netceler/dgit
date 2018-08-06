package io.insight.jgit;

public class KVObject {
    private final String objectId;

    private final int type;

    private final long size;

    private final long totalSize;

    private KVObject base;

    public KVObject(final String objectId, final int type, final long size, final long totalSize) {
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

    public void setBase(final KVObject base) {
        this.base = base;
    }
}