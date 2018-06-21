package io.insight.jgit.services;

import java.io.IOException;

public interface KVAdapter {

  KVConfigService configService();

  KVRefService refService();

  KVObjectService objService();
}
