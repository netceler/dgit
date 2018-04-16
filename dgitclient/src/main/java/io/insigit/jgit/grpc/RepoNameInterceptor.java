package io.insigit.jgit.grpc;

import io.grpc.*;

import java.util.List;

public class RepoNameInterceptor implements ClientInterceptor {
  private String repoName;

  public RepoNameInterceptor(String repoName) {
    this.repoName = repoName;
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        headers.put(Constants.REPO_NAME_METADATA_KEY, repoName);
        super.start(responseListener, headers);
      }
    };
  }
}

