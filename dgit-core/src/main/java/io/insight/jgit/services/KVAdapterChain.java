package io.insight.jgit.services;

public class KVAdapterChain implements KVAdapter {

    private final KVConfigServiceChain kvConfigServiceChain = new KVConfigServiceChain();

    private final KVRefServiceChain kvRefServiceChain = new KVRefServiceChain();

    private final KVObjectServiceChain kvObjectServiceChain = new KVObjectServiceChain();

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
