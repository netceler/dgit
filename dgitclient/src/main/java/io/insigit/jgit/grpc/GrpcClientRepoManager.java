package io.insigit.jgit.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.insight.jgit.RepoManagerGrpc;
import io.insight.jgit.RepoName;
import io.insight.jgit.RepoReply;
import io.insigit.jgit.services.RepoManager;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

public class GrpcClientRepoManager implements RepoManager {

  private final RepoManagerGrpc.RepoManagerBlockingStub stub;
  private String host;
  private int port;

  public GrpcClientRepoManager(String host, int port) {
    this.host = host;
    this.port = port;
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build();
    this.stub = RepoManagerGrpc.newBlockingStub(channel);
  }


  @Override
  public Repository open(String name) throws RepositoryNotFoundException {
    RepoReply result = stub.openRepo(nameRequest(name));
    if (!result.getRepoExists()) {
      throw new RepositoryNotFoundException(name);
    }
    RepoClient client = new RepoClient(host, port, GrpcClientRepoManager.this, name);
    return client.repository();
  }

  private RepoName nameRequest(String name) {
    return RepoName.newBuilder().setName(name).build();
  }

  @Override
  public boolean exists(String name) {
    return stub.openRepo(nameRequest(name)).getRepoExists();
  }

  @Override
  public Repository create(String name) {
    stub.createRepo(nameRequest(name));
    try {
      return open(name);
    } catch (RepositoryNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(String name) {
    stub.deleteRepo(nameRequest(name));
  }
}
