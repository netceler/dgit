package io.insight.jgit.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.insight.jgit.services.KVConfigServiceMiddleware;

import java.io.IOException;

class CachedConfigService implements KVConfigServiceMiddleware {

  Cache<String, String> cache;

  public CachedConfigService(int cacheSize) {
    cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .build();
  }

  @Override
  public String loadConfig(String repositoryName, LoadConfigInvocation invocation) throws IOException {
    return cache.get(repositoryName, s -> {
      CacheAdapter.checkInvocation(invocation);
      try {
        return invocation.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void saveConfig(String repositoryName, String configText, SaveConfigInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    cache.put(repositoryName, configText);
    invocation.next();
  }
}
