package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;

import java.io.IOException;

public class KVObjectLoader extends ObjectLoader {
  private final KVObject object;
  private final KVObjectService objectService;

  public KVObjectLoader(KVObject object, KVObjectService objectService) {
    this.object = object;
    this.objectService = objectService;
  }

  @Override
  public int getType() {
    return object.getType();
  }

  @Override
  public long getSize() {
    return object.getSize();
  }

  @Override
  public byte[] getCachedBytes() throws LargeObjectException {
     throw new LargeObjectException();
  }

  @Override
  public boolean isLarge() {
    return true;
  }

  @Override
  public ObjectStream openStream() throws MissingObjectException, IOException {
    byte[] data = object.resolveDelta(objectService);
    return new ObjectStream.SmallStream(getType(), data);
  }
}
