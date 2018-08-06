package io.insight.jgit.cache;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collection;
import java.util.Objects;

import io.insight.jgit.KVObject;
import io.insight.jgit.services.KVObjectServiceMiddleware;

public class CachedObjectService implements KVObjectServiceMiddleware {
    private final Cache<ObjectKey, KVObject> objectCache;

    private final Cache<ObjectKey, byte[]> dataCache;

    public CachedObjectService(final int cacheSize) {
        objectCache = Caffeine.newBuilder().maximumSize(cacheSize).recordStats().build();
        dataCache = Caffeine.newBuilder().maximumSize(cacheSize).recordStats().build();
    }

    public CacheStats objectCacheStats() {
        return objectCache.stats();
    }

    public CacheStats dataCacheStats() {
        return dataCache.stats();
    }

    @Override
    public Collection<ObjectId> resolve(final String repositoryName, final String name,
            final ResolveInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        return invocation.next();
    }

    @Override
    public void insertLooseObject(final String repositoryName, final AnyObjectId objectId,
            final int objectType, final long inflatedSize, final InputStream in, final long length,
            final InsertLooseObjectInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        invocation.next();
        final KVObject obj = new KVObject(objectId.name(), objectType, inflatedSize, inflatedSize);
        objectCache.put(new ObjectKey(repositoryName, objectId), obj);
    }

    @Override
    public KVObject loadObject(final String repositoryName, final AnyObjectId objectId,
            final LoadObjectInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        return objectCache.get(new ObjectKey(repositoryName, objectId), objectKey -> {
            try {
                return invocation.next();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public byte[] getObjectData(final String repositoryName, final AnyObjectId objectId,
            final GetObjectDataInvocation invocation) throws IOException {
        final ObjectKey key = new ObjectKey(repositoryName, objectId);
        return dataCache.get(key, s -> {
            try {
                return invocation.next();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void insertPackedObject(final String repositoryName, final AnyObjectId objectId,
            final int typeCode, final long inflatedSize, final long totalSize, final String base,
            final SeekableByteChannel channel, final long offset, final long size,
            final InsertPackedObjectInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        invocation.next();
        final KVObject obj = new KVObject(objectId.name(), typeCode, inflatedSize, totalSize);
        if (base != null) {
            final KVObject baseObj = objectCache.getIfPresent(
                    new ObjectKey(repositoryName, ObjectId.fromString(base)));
            if (baseObj != null) {
                obj.setBase(baseObj);
                objectCache.put(new ObjectKey(repositoryName, objectId), obj);
            }
        } else {
            objectCache.put(new ObjectKey(repositoryName, objectId), obj);
        }
    }

    private class ObjectKey {
        private final String repo;

        private final AnyObjectId objectId;

        ObjectKey(final String repo, final AnyObjectId objectId) {
            this.repo = repo;
            this.objectId = objectId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ObjectKey objectKey = (ObjectKey) o;
            return Objects.equals(repo, objectKey.repo) && Objects.equals(objectId, objectKey.objectId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repo, objectId);
        }
    }
}
