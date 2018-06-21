package io.insight.jgit.cache;

import io.insight.jgit.services.KVAdapterMiddleware;

public class CacheAdapter implements KVAdapterMiddleware {

  private final int configCacheSize;
  private final int refCacheSize;
  private final int objCacheSize;

  public CacheAdapter(int configCacheSize, int refCacheSize, int objCacheSize) {
    this.configCacheSize = configCacheSize;
    this.refCacheSize = refCacheSize;
    this.objCacheSize = objCacheSize;
  }

  private CachedConfigService configService;

  public CachedConfigService configService() {
    if (configService == null) {
      configService = new CachedConfigService(configCacheSize);
    }
    return configService;
  }

  private CachedRefService refService;

  static void checkInvocation(Object invocation) {
    if (invocation == null) {
      throw new IllegalStateException("missing adapter");
    }
  }

  public CachedRefService refService() {
    if (refService == null) {
      refService = new CachedRefService(refCacheSize);
    }
    return refService;
  }

  private CachedObjectService objService;

  public CachedObjectService objService() {
    if (objService == null) {
      objService = new CachedObjectService(objCacheSize);
    }
    return objService;
  }

}
