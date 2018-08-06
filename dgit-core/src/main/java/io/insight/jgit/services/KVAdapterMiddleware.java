package io.insight.jgit.services;

public interface KVAdapterMiddleware {

    <CachedConfigService> CachedConfigService configService();

    <KVRefServiceMiddleware> KVRefServiceMiddleware refService();

    <KVObjectServiceMiddleware> KVObjectServiceMiddleware objService();
}