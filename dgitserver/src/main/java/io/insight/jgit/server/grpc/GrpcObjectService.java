package io.insight.jgit.server.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.*;
import io.insight.jgit.server.services.ObjectServiceImpl;
import io.insight.jgit.server.utils.ByteBufferBackedInputStream;
import io.insight.jgit.server.utils.FileCachedByteBuffer;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.PackParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.insight.jgit.server.grpc.RepoNameServerInterceptor.REPO_CTX_KEY;
import static io.insigit.jgit.grpc.GrpcClientObjectService.BUFFER_SIZE;

public class GrpcObjectService extends ObjectServiceGrpc.ObjectServiceImplBase {
  private AtomicInteger inserterIdSeq = new AtomicInteger(0);
  private ConcurrentHashMap<Integer, ObjectInserter> inserters = new ConcurrentHashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(GrpcObjectService.class);

  private ObjectServiceImpl getImpl() {
    Repository repo = REPO_CTX_KEY.get();
    return new ObjectServiceImpl(repo);
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ObjectIdList> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    Collection<ObjectId> result = impl.resolve(AbbreviatedObjectId.fromString(request.getAbbreviatedObjectId().getId()));
    if (logger.isDebugEnabled())
      logger.debug("resolve {} -> {}", request.toString(), result);
    ObjectIdList.Builder builder = toObjectIdList(result);
    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  private ObjectIdList.Builder toObjectIdList(Collection<ObjectId> result) {
    ObjectIdList.Builder builder = ObjectIdList.newBuilder();
    for (ObjectId objectId : result) {
      builder.addObjectIds(io.insight.jgit.ObjectId.newBuilder().setId(objectId.getName()));
    }
    return builder;
  }

  @Override
  public void shallowCommits(Empty request, StreamObserver<ObjectIdList> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    Set<ObjectId> result = impl.getShallowCommits();
    if (logger.isDebugEnabled())
      logger.debug("shallowCommits {} -> {}", request.toString(), result);
    responseObserver.onNext(toObjectIdList(result).build());
    responseObserver.onCompleted();
  }

  @Override
  public void open(OpenRequest request, StreamObserver<OpenReply> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    try {
      ObjectLoader ldr = impl.open(ObjectId.fromString(request.getObjectId().getId()), request.getObjectHint());
      OpenReply.Builder builder = OpenReply.newBuilder()
          .setIsLarge(ldr.isLarge())
          .setType(ldr.getType())
          .setSize(ldr.getSize());

      if (ldr.isLarge()) {
        ObjectStream stream = ldr.openStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int read;
        while ((read = stream.read(buf)) != -1) {
          responseObserver.onNext(
              builder.setData(ByteString.copyFrom(buf, 0, read))
                  .build());
        }
      } else {
        responseObserver.onNext(builder
            .setData(ByteString.copyFrom(ldr.getCachedBytes()))
            .build());
      }
      if (logger.isDebugEnabled())
        logger.debug("open {} -> {}", request.toString(), builder.toString());
      responseObserver.onCompleted();
    } catch (MissingObjectException e){
      responseObserver.onError(Status.NOT_FOUND
          .withDescription(e.getMessage())
          .withCause(e.getCause())
          .augmentDescription(e.getObjectId().name())
          .asRuntimeException()
      );
    } catch (IncorrectObjectTypeException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT
          .withDescription(e.getMessage())
          .withCause(e.getCause())
          .asRuntimeException()
      );
    } catch (IOException e) {
      responseObserver.onError(Status.INTERNAL
          .withDescription(e.getMessage())
          .withCause(e.getCause())
          .asRuntimeException()
      );
    }
  }

  @Override
  public void has(OpenRequest request, StreamObserver<HasReply> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    ObjectId objectId = ObjectId.fromString(request.getObjectId().getId());
    try {
      boolean result = impl.has(objectId, request.getObjectHint());
      if (logger.isDebugEnabled())
        logger.debug("has {} -> {}", request.toString(), result);
      responseObserver.onNext(HasReply.newBuilder().setHas(result).build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public StreamObserver<ObjectInsertRequest> insert(StreamObserver<io.insight.jgit.ObjectId> responseObserver) {

    return new StreamObserver<ObjectInsertRequest>() {
      public int inserterId;
      FileCachedByteBuffer.ClosableByteBuf buf;
      int length;
      int objectType;

      @Override
      public void onNext(ObjectInsertRequest req) {
        if (buf == null) {
          try {
            this.inserterId = req.getInserter().getId();
            this.objectType = req.getObjectType();
            this.length = (int) req.getTotalLength();
            buf = FileCachedByteBuffer.createBuffer(length);
          } catch (IOException e) {
            responseObserver.onError(e);
          }
        }
        if (!req.getData().isEmpty())
          buf.readFrom(req.getData());
      }

      @Override
      public void onError(Throwable t) {
        responseObserver.onError(t);
      }

      @Override
      public void onCompleted() {
        if (buf != null) {
          InputStream in = new ByteBufferBackedInputStream(buf.nioBuffer());
          try {
            ObjectInserter inserter = inserters.get(inserterId);
            if (inserter != null) {
              ObjectId result = inserter.insert(objectType, length, in);
              if (logger.isDebugEnabled())
                logger.debug("inserter:{} insert object type:{} len:{} -> {}",inserterId, objectType, length, result);
              responseObserver.onNext(io.insight.jgit.ObjectId.newBuilder().setId(
                  result.getName()).build());
            } else {
              responseObserver.onError(new Exception("inserter not found: " + inserterId));
            }
          } catch (IOException e) {
            responseObserver.onError(e);
          } finally {
            try {
              buf.close();
            } catch (Exception e) {
              responseObserver.onError(e);
            }
          }
        }
        responseObserver.onCompleted();
      }
    };
  }


  @Override
  public void newInserter(Empty request, StreamObserver<Inserter> responseObserver) {
    ObjectServiceImpl impl = getImpl();
    ObjectInserter inserter = impl.newInserter(null);
    int insertId = inserterIdSeq.getAndIncrement();
    inserters.put(insertId, inserter);
    if (logger.isDebugEnabled())
      logger.debug("new inserter id:{}", insertId);
    responseObserver.onNext(Inserter.newBuilder().setId(insertId).build());
    responseObserver.onCompleted();

  }

  @Override
  public void flushInserter(Inserter request, StreamObserver<Empty> responseObserver) {
    int id = request.getId();
    ObjectInserter inserter = inserters.get(id);
    if (inserter != null) {
      try {
        inserter.flush();
        if (logger.isDebugEnabled())
          logger.debug("flush inserter id:{}", id);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
      } catch (IOException e) {
        responseObserver.onError(e);
      }
    } else {
      responseObserver.onError(new Exception("inserter not found: " + id));
    }
  }

  @Override
  public void closeInserter(Inserter request, StreamObserver<Empty> responseObserver) {
    int id = request.getId();
    ObjectInserter inserter = inserters.get(id);
    if (inserter != null) {
      inserter.close();
      inserters.remove(id);
      if (logger.isDebugEnabled())
        logger.debug("close inserter id:{}", id);
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    } else {
      responseObserver.onError(new Exception("inserter not found: " + id));
    }
  }

  @Override
  public StreamObserver<PackParserRequest> newParser(StreamObserver<Empty> responseObserver) {
    return new StreamObserver<PackParserRequest>() {
      public int inserterId;
      ObjectInserter inserter;
      FileCachedByteBuffer.ClosableByteBuf buf;

      @Override
      public void onNext(PackParserRequest req) {
        try {
          inserterId = req.getInserter().getId();
          inserter = inserters.get(inserterId);
          if (inserter == null) {
            responseObserver.onError(
                Status.INTERNAL.withDescription("inserter not found: " + inserterId).asRuntimeException());
          } else {
            if (buf == null) {
              buf = FileCachedByteBuffer.createBuffer();
              if (logger.isDebugEnabled())
                logger.debug("inserter:{} newParser", inserterId);
            }
            buf.readFrom(req.getData());
          }
        } catch (Exception e) {
          responseObserver.onError(e);
        }
      }

      @Override
      public void onError(Throwable t) {

      }

      @Override
      public void onCompleted() {
        InputStream in = new ByteBufferBackedInputStream(buf.nioBuffer());
        try {
          PackParser parser = inserter.newPackParser(in);
          parser.parse(NullProgressMonitor.INSTANCE);
          responseObserver.onNext(Empty.getDefaultInstance());
          responseObserver.onCompleted();
          if (logger.isDebugEnabled())
            logger.debug("inserter:{} parse done.", inserterId);
        } catch (IOException e) {
          responseObserver.onError(e);
        } finally {
          try {
            buf.close();
          } catch (Exception e) {
            responseObserver.onError(e);
          }
        }
      }
    };
  }
}
