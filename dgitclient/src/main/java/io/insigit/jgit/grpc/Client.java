package io.insigit.jgit.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {
  private final ManagedChannel channel;

  public Client(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host,port)
        .usePlaintext()
        .intercept(new RepoNameInterceptor())
        .build();
  }


  public ManagedChannel getChannel() {
    return channel;
  }
}
