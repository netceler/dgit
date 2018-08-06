package io.insight.jgit.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;

import io.insight.jgit.services.KVConfigServiceMiddleware;

class CachedConfigService implements KVConfigServiceMiddleware {

    Cache<String, String> cache;

    public CachedConfigService(final int cacheSize) {
        cache = Caffeine.newBuilder().maximumSize(cacheSize).build();
    }

    @Override
    public String loadConfig(final String repositoryName, final LoadConfigInvocation invocation)
            throws IOException {
        return cache.get(repositoryName, s -> {
            CacheAdapter.checkInvocation(invocation);
            try {
                return invocation.next();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void saveConfig(final String repositoryName, final String configText,
            final SaveConfigInvocation invocation) throws IOException {
        CacheAdapter.checkInvocation(invocation);
        cache.put(repositoryName, configText);
        invocation.next();
    }
}
