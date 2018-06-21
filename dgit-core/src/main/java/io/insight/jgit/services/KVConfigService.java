package io.insight.jgit.services;


import io.insight.Middleware;

import java.io.IOException;

@Middleware
public interface KVConfigService {
  String loadConfig(String repositoryName) throws IOException;

  void saveConfig(String repositoryName, String configText) throws IOException;

}
