package io.insight.jgit.cache;

import org.eclipse.jgit.lib.Ref;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.IOException;
import java.util.Collection;

import io.insight.jgit.KVRef;
import io.insight.jgit.services.KVRefServiceMiddleware;

public class CachedRefService implements KVRefServiceMiddleware {

    private final Cache<String, Collection<KVRef>> cache;

    public CachedRefService(final int cacheSize) {
        cache = Caffeine.newBuilder().maximumSize(cacheSize).recordStats().build();
    }

    @Override
    public Collection<KVRef> getAllRefs(final String repositoryName, final GetAllRefsInvocation invocation)
            throws IOException {

        return cache.get(repositoryName, s -> {
            try {
                return invocation.next();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean compareAndPut(final String repositoryName, final Ref old, final Ref nw,
            final CompareAndPutInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        cache.invalidate(repositoryName);
        return invocation.next();
    }

    @Override
    public boolean compareAndRemove(final String repositoryName, final Ref old,
            final CompareAndRemoveInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        cache.invalidate(repositoryName);
        return invocation.next();
    }

    public CacheStats cacheStats() {
        return cache.stats();
    }

}
