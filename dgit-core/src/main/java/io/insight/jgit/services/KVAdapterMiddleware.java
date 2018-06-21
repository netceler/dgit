package io.insight.jgit.services;

public interface KVAdapterMiddleware {
  KVConfigServiceMiddleware configService();


  KVRefServiceMiddleware refService();


  KVObjectServiceMiddleware objService();
}
