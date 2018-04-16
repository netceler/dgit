package io.insight.jgit.server.grpc;

import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileBasedConfig;

import java.io.IOException;

public class GrpcRepoManager extends RepoManagerGrpc.RepoManagerImplBase {
  private LocalDiskRepoManager localRepoManager;

  public GrpcRepoManager(LocalDiskRepoManager repoManager) {
    localRepoManager = repoManager;
  }

  @Override
  public void loadConfig(RepoName request, StreamObserver<RepoConfig> responseObserver) {
    String config = localRepoManager.open(request.getName()).getConfig().toText();
    responseObserver.onNext(RepoConfig.newBuilder().setConfig(config).build());
    responseObserver.onCompleted();
  }

  @Override
  public void saveConfig(RepoConfig request, StreamObserver<Empty> responseObserver) {
    FileRepository repository = localRepoManager.open(request.getRepoName());
    try {
      FileBasedConfig config = repository.getConfig();
      config.fromText(request.getConfig());
      config.save();
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (ConfigInvalidException | IOException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void createRepo(RepoName request, StreamObserver<Empty> responseObserver) {
    localRepoManager.create(request.getName());
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void openRepo(RepoName request, StreamObserver<RepoReply> responseObserver) {
    String name = request.getName();
    boolean exists = localRepoManager.exists(name);
    if (exists) {
      localRepoManager.open(name);
    }
    responseObserver.onNext(RepoReply.newBuilder().setRepoExists(exists).build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteRepo(RepoName request, StreamObserver<RepoReply> responseObserver) {
    String name = request.getName();
    boolean exists = localRepoManager.exists(name);
    if (exists)
      localRepoManager.delete(request.getName());
    responseObserver.onNext(RepoReply.newBuilder().setRepoExists(exists).build());
    responseObserver.onCompleted();
  }
}
