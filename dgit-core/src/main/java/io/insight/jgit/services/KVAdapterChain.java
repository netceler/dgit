package io.insight.jgit.services;

public class KVAdapterChain implements KVAdapter {

  private KVConfigServiceChain kvConfigServiceChain = new KVConfigServiceChain();
  private KVRefServiceChain kvRefServiceChain = new KVRefServiceChain();
  private KVObjectServiceChain kvObjectServiceChain  = new KVObjectServiceChain();

  @Override
  public KVConfigServiceChain configService() {
    return kvConfigServiceChain;
  }

  @Override
  public KVRefServiceChain refService() {
    return kvRefServiceChain;
  }

  @Override
  public KVObjectServiceChain objService() {
    return kvObjectServiceChain;
  }
}
