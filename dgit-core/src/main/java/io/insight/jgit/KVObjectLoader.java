package io.insight.jgit;

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.lib.InflaterCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.storage.pack.PackConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import io.insight.jgit.services.KVObjectService;

public class KVObjectLoader extends ObjectLoader {
    private final String repositoryName;

    private final KVObject object;

    private final KVObjectService objectService;

    private byte[] cached;

    KVObjectLoader(final String repositoryName, final KVObject object, final KVObjectService objectService) {
        this.repositoryName = repositoryName;
        this.object = object;
        this.objectService = objectService;
    }

    @Override
    public int getType() {
        return object.getType();
    }

    @Override
    public long getSize() {
        return object.getTotalSize();
    }

    @Override
    public byte[] getCachedBytes() throws LargeObjectException {
        if (cached == null) {
            try {
                cached = applyDelta(object);
            } catch (final IOException e) {
                throw new LargeObjectException(e);
            }
        }
        return cached;
    }

    @Override
    public boolean isLarge() {
        return getSize() > PackConfig.DEFAULT_BIG_FILE_THRESHOLD;
    }

    @Override
    public ObjectStream openStream() throws MissingObjectException, IOException {
        if (object.getBase() == null) {
            final byte[] data = getObjectData(object.getObjectId());
            final ByteArrayInputStream bi = new ByteArrayInputStream(data);
            final Inflater inflater = InflaterCache.get();
            final InflaterInputStream is = new InflaterInputStream(bi, inflater) {
                @Override
                public void close() throws IOException {
                    super.close();
                    InflaterCache.release(inflater);
                }
            };
            return new ObjectStream.Filter(getType(), getSize(), is);
        } else {
            return new ObjectStream.SmallStream(getType(), applyDelta(object));
        }
    }

    @SuppressWarnings("hiding")
    private byte[] applyDelta(final KVObject object) throws IOException {
        try {

            final byte[] delta = decompress(getObjectData(object.getObjectId()), (int) object.getSize());
            if (object.getBase() == null) {
                return delta;
            }
            final byte[] base = applyDelta(object.getBase());
            return BinaryDelta.apply(base, delta);
        } catch (final DataFormatException e) {
            throw new IOException(e);
        }
    }

    private byte[] decompress(final byte[] data, final int inflatedSize) throws DataFormatException {
        final Inflater inflater = InflaterCache.get();
        inflater.setInput(data);
        final byte[] result = new byte[inflatedSize];
        inflater.inflate(result);
        InflaterCache.release(inflater);
        return result;
    }

    private byte[] getObjectData(final String objectId) throws IOException {
        return objectService.getObjectData(repositoryName, ObjectId.fromString(objectId));
    }
}
