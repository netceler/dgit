package io.insight.jgit;

import org.eclipse.jgit.internal.storage.file.PackLock;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.transport.PackedObjectInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.zip.CRC32;

import io.insight.jgit.services.KVObjectService;

public class KVPackParser extends PackParser {

    private final KVObjectService objectService;

    private final String repositoryName;

    private final HashMap<Long, ObjectInfo> objectsByPos = new HashMap<>();

    private final CRC32 crc;

    private RandomAccessFile out;

    KVPackParser(final KVObjectService objectService, final KVObjectDatabase objectDatabase,
            final InputStream in) {
        super(objectDatabase, in);
        this.objectService = objectService;
        this.crc = new CRC32();
        this.repositoryName = objectDatabase.getRepository().getRepositoryName();
    }

    @Override
    public PackLock parse(final ProgressMonitor receiving, final ProgressMonitor resolving)
            throws IOException {
        final File tmpPack = File.createTempFile("incoming_", ".pack");
        out = new RandomAccessFile(tmpPack, "rw");
        super.parse(receiving, resolving);

        for (final ObjectInfo info : objectsByPos.values()) {
            objectService.insertPackedObject(repositoryName, ObjectId.fromString(info.objectId), info.type,
                    info.inflatedSize, info.totalSize, info.getBaseId(), out.getChannel(),
                    info.pos + info.headerSize, info.size);
        }
        out.close();
        if (!tmpPack.delete() && tmpPack.exists()) {
            tmpPack.deleteOnExit();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStoreStream(final byte[] raw, final int pos, final int len) throws IOException {
        out.write(raw, pos, len);
    }

    @Override
    protected void onObjectHeader(final Source src, final byte[] raw, final int pos, final int len)
            throws IOException {
        crc.update(raw, pos, len);
        if (last != null) {
            last.headerSize = len;
        }
    }

    @Override
    protected void onObjectData(final Source src, final byte[] raw, final int pos, final int len)
            throws IOException {
        crc.update(raw, pos, len);
    }

    @Override
    protected void onEndThinPack() throws IOException {
    }

    @Override
    protected UnresolvedDelta onEndDelta() throws IOException {
        final UnresolvedDelta delta = new UnresolvedDelta();
        delta.setCRC((int) crc.getValue());
        return delta;
    }

    @Override
    protected void onPackHeader(final long objCnt) throws IOException {
    }

    @Override
    protected void onPackFooter(final byte[] hash) throws IOException {
        if (last != null) {
            last.size = out.getFilePointer() - last.pos;
            last = null;
        }
    }

    @Override
    protected ObjectTypeAndSize seekDatabase(final UnresolvedDelta delta, final ObjectTypeAndSize info)
            throws IOException {
        out.seek(delta.getOffset());
        crc.reset();
        return readObjectHeader(info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ObjectTypeAndSize seekDatabase(final PackedObjectInfo obj, final ObjectTypeAndSize info)
            throws IOException {
        out.seek(obj.getOffset());
        crc.reset();
        return readObjectHeader(info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int readDatabase(final byte[] dst, final int pos, final int cnt) throws IOException {
        return out.read(dst, pos, cnt);
    }

    @Override
    protected boolean checkCRC(final int oldCRC) {
        return oldCRC == (int) crc.getValue();
    }

    @Override
    protected void onBeginWholeObject(final long streamPosition, final int type, final long inflatedSize)
            throws IOException {
        crc.reset();
        newObject(streamPosition, type, inflatedSize);
    }

    private ObjectInfo last;

    private ObjectInfo newObject(final long streamPosition, final int type, final long inflatedSize) {
        if (last != null) {
            last.size = streamPosition - last.pos;
        }
        last = new ObjectInfo();
        last.pos = streamPosition;
        last.type = type;
        last.inflatedSize = inflatedSize;
        objectsByPos.put(streamPosition, last);
        return last;
    }

    @Override
    protected void onEndWholeObject(final PackedObjectInfo info) throws IOException {
        info.setCRC((int) crc.getValue());
        if (info.getType() == Constants.OBJ_BLOB) {
            final ObjectInfo i = objectsByPos.get(info.getOffset());
            i.objectId = info.getName();
            i.type = info.getType();
            i.totalSize = i.inflatedSize;
        }
    }

    @Override
    protected void onInflatedObjectData(final PackedObjectInfo info, final int typeCode, final byte[] data)
            throws IOException {
        final ObjectInfo i = objectsByPos.get(info.getOffset());
        i.objectId = info.getName();
        i.type = typeCode;
        i.totalSize = data.length;
    }

    @Override
    protected boolean onAppendBase(final int typeCode, final byte[] data, final PackedObjectInfo info)
            throws IOException {
        return false;
    }

    @Override
    protected void onBeginOfsDelta(final long deltaStreamPosition, final long baseStreamPosition,
            final long inflatedSize) throws IOException {
        crc.reset();
        final ObjectInfo o = newObject(deltaStreamPosition, Constants.OBJ_OFS_DELTA, inflatedSize);
        o.basePos = baseStreamPosition;
    }

    @Override
    protected void onBeginRefDelta(final long deltaStreamPosition, final AnyObjectId baseId,
            final long inflatedSize) throws IOException {
        crc.reset();
        final ObjectInfo o = newObject(deltaStreamPosition, Constants.OBJ_REF_DELTA, inflatedSize);
        o.baseId = baseId.name();
    }

    class ObjectInfo {
        Long basePos;

        String baseId;

        String objectId;

        int headerSize;

        long pos;

        int type;

        long inflatedSize;

        long size;

        long totalSize;

        String getBaseId() {
            if (baseId == null) {
                if (basePos == null) {
                    return null;
                }
                return objectsByPos.get(basePos).objectId;
            }
            return baseId;
        }
    }
}
