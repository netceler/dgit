package io.insigit.jgit.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.insight.jgit.RepoManagerGrpc;
import io.insigit.jgit.RpcRepository;
import io.insigit.jgit.services.RepoManager;
import io.insigit.jgit.services.RepoService;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;

public class RepoClient implements AutoCloseable {
  private final ManagedChannel channel;
  private final GrpcClientRefService refService;
  private final GrpcClientObjectService objectService;
  private final RepoService repoService;
  private final String repoName;
  private RpcRepository repository;

  public RepoClient(String host, int port, RepoManager repoManager, String repoName) {
    this.repoName = repoName;
    channel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .intercept(new RepoNameInterceptor(repoName))
        .build();
    RepoManagerGrpc.RepoManagerBlockingStub stub = RepoManagerGrpc.newBlockingStub(channel);
    refService = new GrpcClientRefService(this);
    objectService = new GrpcClientObjectService(this);
    repoService = new RepoService() {
      @Override
      public StoredConfig getConfig() {
        return new GrpcRepoConfig(stub, repoName);
      }

      @Override
      public void create() {
        repoManager.create(repoName);
      }
    };
  }

  public Repository repository() throws RepositoryNotFoundException {
    if (repository == null) {
      repository = buildRepo();
    }
    return repository;
  }


  public ManagedChannel channel() {
    return channel;
  }

  public void close() {
    channel.shutdown();
  }

  private RpcRepository buildRepo() throws RepositoryNotFoundException {
    DfsRepositoryBuilder builder = new DfsRepositoryBuilder() {
      @Override
      public DfsRepository build() throws IOException {
        return new RpcRepository(this, repoService, refService, objectService) {
          @Override
          public void close() {
            super.close();
            RepoClient.this.close();
          }
        };
      }
    };
    try {
      return (RpcRepository) builder.build();
    } catch (IOException e) {
      throw new RepositoryNotFoundException(repoName, e);
    }
  }
}
