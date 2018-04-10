package io.insigit.jgit.grpc;

import io.grpc.Context;
import io.insight.jgit.*;
import io.insigit.jgit.RpcRepository;
import io.insigit.jgit.services.RpcRefService;
import org.eclipse.jgit.lib.Ref;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static io.insigit.jgit.utils.Converters.*;

public class RefServiceImpl implements RpcRefService {
  private final RefServiceGrpc.RefServiceBlockingStub stub;
  private RpcRepository repository;
  private Client client;

  public RefServiceImpl(Client client, RpcRepository repository) {
    this.client = client;
    stub = RefServiceGrpc.newBlockingStub(client.getChannel());
    this.repository = repository;
  }

  public <T> T withRepoNameRun(Callable<T> c)   {
    try {
      return Context.current().withValue(Constants.REPO_NAME_CTX_KEY,
          this.repository.getDescription().getRepositoryName()).call(c);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Collection<Ref> getAll() {
    return withRepoNameRun(() -> {
      RefList result = stub.all(Empty.getDefaultInstance());
      Map<String, io.insight.jgit.Ref> map =
          result.getRefsList().stream().collect(Collectors.toMap(io.insight.jgit.Ref::getName,
              Function.identity()));
      return result.getRefsList().stream().map(r -> getRef(map, r)).collect(toList());
    });
  }



  @Override
  public boolean compareAndPut(Ref oldRef, Ref newRef) {
    return withRepoNameRun(() -> {
      RefUpdateResult result = stub.compareAndPut(RefUpdateRequest.newBuilder()
          .setOldRef(toProtoRef(oldRef))
          .setNewRef(toProtoRef(newRef))
          .build());
      return result.getResult();
    });
  }

  @Override
  public boolean compareAndRemove(Ref oldRef) {
    return withRepoNameRun(() -> {
      RefUpdateResult result = stub.compareAndRemove(RefRemoveRequest.newBuilder()
          .setOldRef(toProtoRef(oldRef))
          .build());
      return result.getResult();
    });
  }
}
