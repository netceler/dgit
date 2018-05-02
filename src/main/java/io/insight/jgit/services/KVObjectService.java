package io.insight.jgit.services;

import io.insight.jgit.KVObject;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.ObjectId;

import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;

public interface KVObjectService {
  Collection<ObjectId> resolve(String name);

  KVObject insertLooseObject(int objectType, long length, InputStream in);

  /**
   *  load object including its base
   * @param objectId
   * @return
   */
  KVObject loadObject(String objectId);

  byte[] getObjectData(String objectId);

  void insertPackedObject(String objectId, int typeCode, long inflatedSize, @Nullable String base,
                          SeekableByteChannel channel, long offset, long size);
}
