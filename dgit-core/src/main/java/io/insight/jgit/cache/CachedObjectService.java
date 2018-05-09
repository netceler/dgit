package io.insight.jgit.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.insight.jgit.KVObject;
import io.insight.jgit.services.KVObjectServiceMiddleware;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Objects;

public class CachedObjectService implements KVObjectServiceMiddleware {
  private final Cache<ObjectKey, KVObject> objectCache;
  private final Cache<ObjectKey, byte[]> dataCache;


  public CachedObjectService(int cacheSize) {
    objectCache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .recordStats()
        .build();
    dataCache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .recordStats()
        .build();
  }

  public CacheStats objectCacheStats() {
    return objectCache.stats();
  }
  public CacheStats dataCacheStats() {
    return dataCache.stats();
  }

  @Override
  public Collection<ObjectId> resolve(String repositoryName, String name, ResolveInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    return invocation.next();
  }

  @Override
  public void insertLooseObject(String repositoryName, AnyObjectId objectId, int objectType, long inflatedSize, InputStream in, long length, InsertLooseObjectInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    invocation.next();
    KVObject obj = new KVObject(objectId.name(), objectType, inflatedSize, inflatedSize);
    objectCache.put(new ObjectKey(repositoryName, objectId), obj);
  }

  @Override
  public KVObject loadObject(String repositoryName, AnyObjectId objectId, LoadObjectInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    return objectCache.get(new ObjectKey(repositoryName, objectId), objectKey -> {
      try {
        return invocation.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }


  @Override
  public byte[] getObjectData(String repositoryName, AnyObjectId objectId, GetObjectDataInvocation invocation) throws IOException {
    ObjectKey key = new ObjectKey(repositoryName, objectId);
    return dataCache.get(key, s -> {
      try {
        return invocation.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void insertPackedObject(String repositoryName, AnyObjectId objectId, int typeCode, long inflatedSize, long totalSize, String base, SeekableByteChannel channel, long offset, long size, InsertPackedObjectInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    invocation.next();
    KVObject obj = new KVObject(objectId.name(), typeCode, inflatedSize, totalSize);
    if (base != null) {
      KVObject baseObj = objectCache.getIfPresent(new ObjectKey(repositoryName, ObjectId.fromString(base)));
      if (baseObj != null) {
        obj.setBase(baseObj);
        objectCache.put(new ObjectKey(repositoryName, objectId), obj);
      }
    } else {
      objectCache.put(new ObjectKey(repositoryName, objectId), obj);
    }
  }


  private class ObjectKey {
    private String repo;
    private AnyObjectId objectId;

    ObjectKey(String repo, AnyObjectId objectId) {
      this.repo = repo;
      this.objectId = objectId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ObjectKey objectKey = (ObjectKey) o;
      return Objects.equals(repo, objectKey.repo) &&
          Objects.equals(objectId, objectKey.objectId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(repo, objectId);
    }
  }
}
