package io.insight.jgit;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;

import io.insight.jgit.services.KVObjectService;

public class KVObjectDatabase extends ObjectDatabase {

    private final KVRepository repository;

    private final KVObjectService kvObjectService;

    public KVObjectDatabase(final KVRepository repository, final KVObjectService kvObjectService) {
        this.repository = repository;
        this.kvObjectService = kvObjectService;
    }

    @Override
    public ObjectInserter newInserter() {
        return new KVObjectInserter(this, kvObjectService);
    }

    @Override
    public ObjectReader newReader() {
        return new KVObjectReader(repository, kvObjectService);
    }

    @Override
    public void close() {

    }

    public Config getConfig() {
        return repository.getConfig();
    }

    public KVRepository getRepository() {
        return repository;
    }
}
