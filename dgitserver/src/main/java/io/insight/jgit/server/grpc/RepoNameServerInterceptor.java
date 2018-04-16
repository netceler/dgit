package io.insight.jgit.server.grpc;

import io.grpc.*;
import io.insigit.jgit.grpc.Constants;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public class RepoNameServerInterceptor implements ServerInterceptor {

  public static final Context.Key<FileRepository> REPO_CTX_KEY = Context.key("repo");
  private LocalDiskRepoManager repoManager;

  public RepoNameServerInterceptor(LocalDiskRepoManager repoManager) {
    this.repoManager = repoManager;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
    String repoName = metadata.get(Constants.REPO_NAME_METADATA_KEY);
    
    Context ctx = Context.current().withValue(REPO_CTX_KEY, repoManager.open(repoName));
    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
  }
}
