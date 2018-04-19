package io.insight.jgit.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import java.io.File;
import java.io.IOException;

public class GrpcServer {

  private static final int MAX_OPEN_REPOS = 1000;
  private final Server server;
  private LocalDiskRepoManager repoManager;

  public GrpcServer(int port, File baseDir) {
    repoManager = new LocalDiskRepoManager(baseDir, MAX_OPEN_REPOS);
    RepoNameServerInterceptor interceptor=new RepoNameServerInterceptor(repoManager);
    GrpcRefService grpcRefService = new GrpcRefService();
    GrpcRemoteStream grpcRemoteStream= new GrpcRemoteStream();
    GrpcObjectService grpcObjectService = new GrpcObjectService(grpcRemoteStream);
    GrpcRepoManager grpcRepoManager = new GrpcRepoManager(repoManager);
    server = ServerBuilder.forPort(port)
        .addService(grpcRepoManager)
        .addService(grpcRemoteStream)
        .addService(ServerInterceptors.intercept(grpcRefService, interceptor))
        .addService(ServerInterceptors.intercept(grpcObjectService, interceptor))
        .build();
    
  }
  public void start() throws IOException {
    server.start();
  }

  public void  join() throws InterruptedException {
    server.awaitTermination();
  }

  public void stop(){
    server.shutdownNow();
  }
}
