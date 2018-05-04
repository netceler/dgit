package io.insight.jgit.services;

import java.io.IOException;

public interface KVAdapter {

  String loadConfig(String repositoryName) throws IOException;

  void saveConfig(String repositoryName, String configText) throws IOException;


  KVRefService refService(String repositoryName);

  KVObjectService objService(String repositoryName);
}
