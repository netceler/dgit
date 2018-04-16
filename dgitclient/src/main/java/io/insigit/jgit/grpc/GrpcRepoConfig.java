package io.insigit.jgit.grpc;

import io.insight.jgit.RepoConfig;
import io.insight.jgit.RepoManagerGrpc;
import io.insight.jgit.RepoName;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;

public class GrpcRepoConfig extends StoredConfig {
  private final RepoManagerGrpc.RepoManagerBlockingStub stub;
  private final String name;

  public GrpcRepoConfig(RepoManagerGrpc.RepoManagerBlockingStub stub, String name) {
    this.stub = stub;
    this.name = name;
  }

  @Override
  public void load() throws IOException, ConfigInvalidException {
    String text = stub.loadConfig(RepoName.newBuilder().setName(name).build()).getConfig();
    this.fromText(text);
  }

  @Override
  public void save() throws IOException {
    stub.saveConfig(RepoConfig.newBuilder().setRepoName(name).setConfig(this.toText()).build());
  }
}
