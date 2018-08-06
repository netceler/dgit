package io.insight.jgit.services;

import java.io.IOException;

import io.insight.Middleware;

@Middleware
public interface KVConfigService {
    String loadConfig(String repositoryName) throws IOException;

    void saveConfig(String repositoryName, String configText) throws IOException;
}