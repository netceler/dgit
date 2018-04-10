package io.insigit.jgit.grpc;

import io.grpc.*;

import java.util.List;

public class RepoNameInterceptor implements ClientInterceptor {
  public RepoNameInterceptor() {
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(method, callOptions)) {
      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        if (Constants.REPO_NAME_CTX_KEY.get() != null) {
          headers.put(Constants.REPO_NAME_METADATA_KEY, Constants.REPO_NAME_CTX_KEY.get());
        }
        super.start(responseListener, headers);
      }
    };
  }
}

