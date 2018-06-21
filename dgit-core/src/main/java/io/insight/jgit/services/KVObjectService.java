package io.insight.jgit.services;

import io.insight.Middleware;
import io.insight.jgit.KVObject;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;

@Middleware
public interface KVObjectService {
  Collection<ObjectId> resolve(String repositoryName, String name) throws IOException;

  void insertLooseObject(String repositoryName, AnyObjectId objectId, int objectType, long inflatedSize, InputStream in, long length) throws IOException;

  /**
   *  load object including its base
   * @param objectId
   * @return
   */
  KVObject loadObject(String repositoryName, AnyObjectId objectId) throws IOException;

  byte[] getObjectData(String repositoryName, AnyObjectId objectId) throws IOException;

  void insertPackedObject(String repositoryName, AnyObjectId objectId, int typeCode, long inflatedSize, long totalSize, @Nullable String base,
                          SeekableByteChannel channel, long offset, long size) throws IOException;
}
