package io.insight.jgit.services;

public interface KVAdapter {

  String loadConfig(String repositoryName);

  void saveConfig(String repositoryName, String configText);


  KVRefService refService(String repositoryName);

  KVObjectService objService(String repositoryName);
}
