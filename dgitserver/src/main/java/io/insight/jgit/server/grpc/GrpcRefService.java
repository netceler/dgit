package io.insight.jgit.server.grpc;

import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import io.insight.jgit.server.services.RefServiceImpl;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.util.Collection;
import static io.insigit.jgit.utils.Converters.*;
import static io.insight.jgit.server.grpc.RepoNameServerInterceptor.REPO_CTX_KEY;

public class GrpcRefService extends RefServiceGrpc.RefServiceImplBase {
  @Override
  public void all(Empty request, StreamObserver<RefList> responseObserver) {
    RefServiceImpl impl = getImpl();
    Collection<Ref> refs = impl.getAll();
    RefList.Builder refListBuilder= RefList.newBuilder();
    for (Ref ref : refs) {
      refListBuilder.addRefs(toProtoRef(ref));
    }
    responseObserver.onNext(refListBuilder.build());
    responseObserver.onCompleted();
  }

  private RefServiceImpl getImpl() {
    Repository repo = REPO_CTX_KEY.get();
    return new RefServiceImpl(repo);
  }

  @Override
  public void compareAndPut(RefUpdateRequest request, StreamObserver<RefUpdateResult> responseObserver) {
    RefServiceImpl impl = getImpl();
    Ref oldRef = getRef(request.getOldRef());
    Ref newRef = getRef(request.getNewRef());
    boolean result = impl.compareAndPut(oldRef, newRef);
    responseObserver.onNext(RefUpdateResult.newBuilder().setResult(result).build());
    responseObserver.onCompleted();
  }

  @Override
  public void compareAndRemove(RefRemoveRequest request, StreamObserver<RefUpdateResult> responseObserver) {
    RefServiceImpl impl = getImpl();
    Ref oldRef = getRef(request.getOldRef());
    boolean result = impl.compareAndRemove(oldRef);
    responseObserver.onNext(RefUpdateResult.newBuilder().setResult(result).build());
    responseObserver.onCompleted();
  }
}
