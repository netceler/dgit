package io.insight.jgit.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.insight.jgit.KVRef;
import io.insight.jgit.services.KVRefServiceMiddleware;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;

public class CachedRefService implements KVRefServiceMiddleware {

  private final Cache<String, Collection<KVRef>> cache;
  public CachedRefService(int cacheSize) {
    cache = Caffeine.newBuilder()
        .maximumSize(cacheSize)
        .recordStats()
        .build();
  }

  @Override
  public Collection<KVRef> getAllRefs(String repositoryName, GetAllRefsInvocation invocation) throws IOException {

    return cache.get(repositoryName, s -> {
      try {
        return invocation.next();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public boolean compareAndPut(String repositoryName, Ref old, Ref nw, CompareAndPutInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    cache.invalidate(repositoryName);
    return invocation.next();
  }

  @Override
  public boolean compareAndRemove(String repositoryName, Ref old, CompareAndRemoveInvocation invocation) throws IOException {
    CacheAdapter.checkInvocation(invocation);
    cache.invalidate(repositoryName);
    return invocation.next();
  }

  public CacheStats cacheStats(){
    return cache.stats();
  }

}
