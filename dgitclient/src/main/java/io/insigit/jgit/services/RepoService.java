package io.insigit.jgit.services;

import org.eclipse.jgit.lib.StoredConfig;

public interface RepoService {

  StoredConfig getConfig();

  void create();
}
