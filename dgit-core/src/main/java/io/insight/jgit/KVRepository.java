package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.attributes.AttributesNode;
import org.eclipse.jgit.attributes.AttributesNodeProvider;
import org.eclipse.jgit.attributes.AttributesRule;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;

public class KVRepository extends Repository {

  private final KVRepoManager manager;
  private KVRepositoryBuilder options;
  public StoredConfig config;
  private KVRefDatabase refdb;

  public KVRepository(KVRepoManager manager, KVRepositoryBuilder options) {
    super(options);
    this.manager = manager;

    this.options = options;
  }

  @Override
  public void create(boolean bare) throws IOException {
    if (exists())
      throw new IOException(MessageFormat.format(
          JGitText.get().repositoryAlreadyExists, "")); //$NON-NLS-1$

    String master = Constants.R_HEADS + Constants.MASTER;
    RefUpdate.Result result = updateRef(Constants.HEAD, true).link(master);
    if (result != RefUpdate.Result.NEW)
      throw new IOException(result.name());
  }

  private boolean exists() throws IOException {
    return manager.exists(getRepositoryName());
  }

  @Override
  public ObjectDatabase getObjectDatabase() {
    KVObjectService service = manager.adapter().objService();
    return new KVObjectDatabase(this, service);
  }

  public String getRepositoryName() {
    return this.options.getName();
  }

  @Override
  public RefDatabase getRefDatabase() {
    if(refdb==null) {
      refdb = new KVRefDatabase(this, manager.adapter());
    }
    return refdb;
  }

  @Override
  public StoredConfig getConfig() {
    if (config == null) {
      config = new StoredConfig() {
        @Override
        public void load() throws IOException, ConfigInvalidException {
          String cfgText = manager.adapter().configService().loadConfig(getRepositoryName());
          this.fromText(cfgText);
        }

        @Override
        public void save() throws IOException {
          String cfgText = this.toText();
          manager.adapter().configService().saveConfig(getRepositoryName(), cfgText);
        }
      };
    }
    return config;
  }

  @Override
  public AttributesNodeProvider createAttributesNodeProvider() {
    return new EmptyAttributesNodeProvider();
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
    throw new UnsupportedOperationException();
  }

  private static class EmptyAttributesNodeProvider implements
      AttributesNodeProvider {
    private EmptyAttributesNodeProvider.EmptyAttributesNode emptyAttributesNode = new EmptyAttributesNode();

    @Override
    public AttributesNode getInfoAttributesNode() throws IOException {
      return emptyAttributesNode;
    }

    @Override
    public AttributesNode getGlobalAttributesNode() throws IOException {
      return emptyAttributesNode;
    }

    private static class EmptyAttributesNode extends AttributesNode {

      public EmptyAttributesNode() {
        super(Collections.<AttributesRule> emptyList());
      }

      @Override
      public void parse(InputStream in) throws IOException {
        // Do nothing
      }
    }
  }
}
