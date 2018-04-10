package io.insigit.jgit.grpc;

import io.grpc.Context;
import io.grpc.Metadata;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

public class Constants {
  public static final Context.Key<String> REPO_NAME_CTX_KEY = Context.key("repoName");
  public static final Metadata.Key<String> REPO_NAME_METADATA_KEY = Metadata.Key.of("traceId", ASCII_STRING_MARSHALLER);
}
