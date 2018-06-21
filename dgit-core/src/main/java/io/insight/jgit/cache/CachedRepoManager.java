package io.insight.jgit.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.insight.jgit.KVRepoManager;
import io.insight.jgit.KVRepository;
import io.insight.jgit.KVRepositoryBuilder;
import io.insight.jgit.services.*;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.util.Optional;

public class CachedRepoManager implements KVRepoManager {

  private KVAdapterChain chain;
  private KVRepoManager delegate;

  public CachedRepoManager(KVRepoManager delegate) {
    this.delegate = delegate;
  }

  private LoadingCache<String, Optional<Repository>> repositoryCache = Caffeine.newBuilder()
      .maximumSize(1000)
      .removalListener((String name, Optional<Repository> repo, RemovalCause cause) ->
          repo.ifPresent(Repository::close))
      .build(name -> {
        KVRepositoryBuilder options = new KVRepositoryBuilder();
        options.setRepositoryName(name);
        options.setFS(FS.detect());

        return Optional.of(new KVRepository(this, options));
      });

  @Override
  public Repository open(String name) throws RepositoryNotFoundException {
    try {
      if (!exists(name)) {
        throw new RepositoryNotFoundException(name);
      }
    } catch (IOException e) {
      throw new RepositoryNotFoundException(name, e);
    }
    Optional<Repository> repoOptional = repositoryCache.get(name);
    assert repoOptional != null;
    repoOptional.ifPresent(Repository::incrementOpen);
    return repoOptional.orElse(delegate.open(name));
  }


  @Override
  public boolean exists(String name) throws IOException {
    Optional<Repository> optional = repositoryCache.getIfPresent(name);
    if(optional != null) {
      return optional.isPresent();
    }else {
      boolean exists = delegate.exists(name);
      if(!exists) repositoryCache.put(name, Optional.empty());
      return exists;
    }
  }


  @Override
  public Repository create(String name) throws IOException {
    Repository repo = delegate.create(name);
    repositoryCache.put(name, Optional.ofNullable(repo));
    return repo;
  }


  @Override
  public void delete(String name) throws IOException {
    repositoryCache.invalidate(name);
    delegate.delete(name);
  }


  @Override
  public KVAdapterChain adapter() {
    if (chain == null) {
      chain = new KVAdapterChain();
      KVAdapter realAdapter = delegate.adapter();
      KVAdapterMiddleware cacheLayer = cacheLayer();
      chain.configService().setServices(
          cacheLayer.configService(),
          KVConfigServiceMiddleware.wrap(realAdapter.configService()));
      chain.objService().setServices(cacheLayer.objService(),
          KVObjectServiceMiddleware.wrap(realAdapter.objService())
      );
      chain.refService().setServices(cacheLayer.refService(),
          KVRefServiceMiddleware.wrap(realAdapter.refService()));
    }
    return chain;
  }

  private CacheAdapter cacheLayer;

  public CacheAdapter cacheLayer() {
    if (cacheLayer == null) {
      cacheLayer = new CacheAdapter(100, 1000, 10000);
    }
    return cacheLayer;
  }
}
