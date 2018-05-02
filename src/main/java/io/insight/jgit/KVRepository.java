package io.insight.jgit;

import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.lib.*;

import java.io.IOException;

public class KVRepository extends Repository {

  private final KVRepoManager manager;
  private DfsRepository delegateRepo;
  public StoredConfig config;

  protected KVRepository(KVRepoManager manager, DfsRepositoryBuilder options) {
    super(options);
    this.manager = manager;
    delegateRepo = new DfsRepository(options) {

      KVRefDatabase kvRefDatabase;

      @Override
      public DfsObjDatabase getObjectDatabase() {
        throw new UnsupportedOperationException();
      }

      @Override
      public RefDatabase getRefDatabase() {
        if (kvRefDatabase == null) {
          kvRefDatabase = new KVRefDatabase(delegateRepo, manager.adapter());
        }
        return kvRefDatabase;
      }
    };
  }

  @Override
  public void create(boolean bare) throws IOException {
    manager.create(delegateRepo.getDescription().getRepositoryName());
  }

  @Override
  public ObjectDatabase getObjectDatabase() {
    return new KVObjectDatabase(
        manager.adapter().objService(this.delegateRepo.getDescription().getRepositoryName()));
  }

  @Override
  public RefDatabase getRefDatabase() {
    return delegateRepo.getRefDatabase();
  }

  @Override
  public StoredConfig getConfig() {
    if (config == null) {
      config = new StoredConfig() {
        @Override
        public void load() throws IOException, ConfigInvalidException {
          String cfgText = manager.adapter().loadConfig(delegateRepo.getDescription().getRepositoryName());
          this.fromText(cfgText);
        }

        @Override
        public void save() throws IOException {
          String cfgText = this.toText();
          manager.adapter().saveConfig(delegateRepo.getDescription().getRepositoryName(), cfgText);
        }
      };
    }
    return config;
  }

  @Override
  public AttributesNodeProvider createAttributesNodeProvider() {
    return delegateRepo.createAttributesNodeProvider();
  }

  @Override
  public void scanForRepoChanges() throws IOException {
    getRefDatabase().refresh();
  }

  @Override
  public void notifyIndexChanged() {
  }

  @Override
  public ReflogReader getReflogReader(String refName) throws IOException {
    return delegateRepo.getReflogReader(refName);
  }

}
