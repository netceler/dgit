package io.insigit.jgit.grpc;

import io.insight.jgit.*;
import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.lib.Ref;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.insigit.jgit.utils.Converters.getRef;
import static io.insigit.jgit.utils.Converters.toProtoRef;
import static java.util.stream.Collectors.toList;

public class GrpcClientRefService implements RpcRefService {
  private final RefServiceGrpc.RefServiceBlockingStub stub;

  public GrpcClientRefService(RepoClient client) {
    stub = RefServiceGrpc.newBlockingStub(client.channel());
  }


  @Override
  public Collection<Ref> getAll() {
    RefList result = stub.all(Empty.getDefaultInstance());
    Map<String, io.insight.jgit.Ref> map =
        result.getRefsList().stream().collect(Collectors.toMap(io.insight.jgit.Ref::getName,
            Function.identity()));
    return result.getRefsList().stream().map(r -> getRef(map, r)).collect(toList());
  }


  @Override
  public boolean compareAndPut(Ref oldRef, Ref newRef) {
    RefUpdateResult result = stub.compareAndPut(RefUpdateRequest.newBuilder()
        .setOldRef(toProtoRef(oldRef))
        .setNewRef(toProtoRef(newRef))
        .build());
    return result.getResult();
  }

  @Override
  public boolean compareAndRemove(Ref oldRef) {
    RefUpdateResult result = stub.compareAndRemove(RefRemoveRequest.newBuilder()
        .setOldRef(toProtoRef(oldRef))
        .build());
    return result.getResult();
  }
}
