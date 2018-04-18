package io.insigit.jgit.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import io.insigit.jgit.RpcObjDatabase;
import io.insigit.jgit.services.RpcObjectService;
import io.insigit.jgit.utils.Converters;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.eclipse.jgit.lib.ObjectReader.OBJ_ANY;

public class GrpcClientObjectService implements RpcObjectService<Inserter> {

  public static final int BUFFER_SIZE = 8 * 1024;
  private final ObjectServiceGrpc.ObjectServiceBlockingStub stub;
  private final ObjectServiceGrpc.ObjectServiceStub asyncStub;

  public GrpcClientObjectService(RepoClient client) {
    stub = ObjectServiceGrpc.newBlockingStub(client.channel());
    asyncStub = ObjectServiceGrpc.newStub(client.channel());
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) {
    ObjectIdList result = stub.resolve(ResolveRequest.newBuilder().setAbbreviatedObjectId(
        io.insight.jgit.ObjectId.newBuilder().setId(id.name())
    ).build());
    return result.getObjectIdsList().stream().map(Converters::fromObjId).collect(toList());
  }

  @Override
  public Set<ObjectId> getShallowCommits() {
    return stub.shallowCommits(Empty.getDefaultInstance()).getObjectIdsList().stream()
        .map(Converters::fromObjId).collect(toSet());
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException {
    OpenRequest request = OpenRequest.newBuilder()
        .setObjectHint(typeHint)
        .setObjectId(Converters.toObjId(objectId)).build();
    Iterator<OpenReply> iterator = stub.open(request);
    try {
      return new GrpcClientObjectLoader(iterator);
    } catch (StatusRuntimeException e) {
      String type;
      if (typeHint == OBJ_ANY) {
        type = JGitText.get().unknownObjectType2;
      } else {
        type = Constants.typeString(typeHint);
      }
      switch (e.getStatus().getCode()) {
        case NOT_FOUND:
          throw new MissingObjectException(objectId.copy(), type);
        case INVALID_ARGUMENT:
          throw new IncorrectObjectTypeException(objectId.copy(), type);
        default:
          throw new IOException(e.getMessage(), e);
      }
    }
  }

  @Override
  public ObjectId insert(Inserter inserter, int objectType, long length, InputStream in) throws IOException {
    CompletableFuture<ObjectId> result = new CompletableFuture<>();
    StreamObserver<io.insight.jgit.ObjectId> responseObserver = new StreamObserver<io.insight.jgit.ObjectId>() {
      @Override
      public void onNext(io.insight.jgit.ObjectId value) {
        result.complete(Converters.fromObjId(value));
      }

      @Override
      public void onError(Throwable t) {
        result.completeExceptionally(t);
      }

      @Override
      public void onCompleted() {
      }
    };
    StreamObserver<ObjectInsertRequest> requestObserver = asyncStub.insert(responseObserver);
    if (length > 0) {
      byte[] buf = new byte[BUFFER_SIZE];
      int read;
      while ((read = in.read(buf)) != -1) {
        ObjectInsertRequest req = ObjectInsertRequest.newBuilder()
            .setInserter(inserter)
            .setObjectType(objectType)
            .setTotalLength(length)
            .setData(ByteString.copyFrom(buf, 0, read))
            .build();
        requestObserver.onNext(req);
      }
    } else {
      ObjectInsertRequest req = ObjectInsertRequest.newBuilder()
          .setInserter(inserter)
          .setObjectType(objectType)
          .setTotalLength(length)
          .clearData()
          .build();
      requestObserver.onNext(req);
    }
    requestObserver.onCompleted();
    try {
      return result.get();
    } catch (InterruptedException e) {
      requestObserver.onError(e);
      return null;
    } catch (ExecutionException e) {
      throw new IOException("server error", e);
    }
  }

  @Override
  public PackParser newPackParser(Inserter Inserter, RpcObjDatabase odb, InputStream in) throws IOException {
    CompletableFuture<Void> serverFuture = new CompletableFuture<>();
    StreamObserver<Empty> serverObserver = new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
      }

      @Override
      public void onError(Throwable t) {
        serverFuture.completeExceptionally(t);
      }

      @Override
      public void onCompleted() {
        serverFuture.complete(null);
      }
    };
    StreamObserver<PackParserRequest> observer = asyncStub.newParser(serverObserver);

    return new GrpcPackParser(odb, Inserter, in, observer, serverFuture);
  }

  @Override
  public ObjectInserter newInserter(RpcObjDatabase odb) {
    return new GrpcClientObjectInserter(odb, stub);
  }

  @Override
  public boolean has(AnyObjectId objectId, int typeHint) {
    OpenRequest request = OpenRequest.newBuilder()
        .setObjectHint(typeHint)
        .setObjectId(Converters.toObjId(objectId)).build();
    HasReply result = stub.has(request);
    return result.getHas();
  }
}
