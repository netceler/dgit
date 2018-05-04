package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.pack.BinaryDelta;
import org.eclipse.jgit.lib.InflaterCache;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.util.IO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class KVObjectLoader extends ObjectLoader {
  private final KVObject object;
  private final KVObjectService objectService;
  private byte[] cached;

  KVObjectLoader(KVObject object, KVObjectService objectService) {
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
      } catch (IOException e) {
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
      byte[] data = getObjectData(object.getObjectId());
      ByteArrayInputStream bi = new ByteArrayInputStream(data);
      final Inflater inflater = InflaterCache.get();
      InflaterInputStream is = new InflaterInputStream(bi, inflater) {
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

  private byte[] applyDelta(KVObject object) throws IOException {
    try {

      byte[] delta = decompress(getObjectData(object.getObjectId()), (int) object.getSize());
      if (object.getBase() == null) {
        return delta;
      }
      byte[] base = applyDelta(object.getBase());
      return BinaryDelta.apply(base, delta);
    } catch (DataFormatException e) {
      throw new IOException(e);
    }
  }

  private byte[] decompress(byte[] data, int inflatedSize) throws DataFormatException {
    Inflater inflater = InflaterCache.get();
    inflater.setInput(data);
    byte[] result = new byte[inflatedSize];
    inflater.inflate(result);
    InflaterCache.release(inflater);
    return result;
  }

  private byte[] getObjectData(String objectId) throws IOException {
    return objectService.getObjectData(ObjectId.fromString(objectId));
  }
}
