package io.insight.jgit.server.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.insight.jgit.RemoteStreamGrpc;
import io.insight.jgit.StreamCmd;
import io.insight.jgit.StreamReply;
import io.insight.jgit.StreamRequest;
import io.insight.jgit.StreamRequest.DataChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcRemoteStream extends RemoteStreamGrpc.RemoteStreamImplBase {
  private static final Logger logger = LoggerFactory.getLogger(GrpcRemoteStream.class);

  private AtomicInteger seq = new AtomicInteger();
  private ConcurrentHashMap<Integer, InputStream> map = new ConcurrentHashMap<>();

  @Override
  public StreamObserver<StreamRequest> inputStream(StreamObserver<StreamReply> responseObserver) {
    int id = seq.getAndIncrement();
    ArrayBlockingQueue queue = new ArrayBlockingQueue<>(1);
    AtomicBoolean marksupport = new AtomicBoolean(false);

    StreamObserver<StreamRequest> observer = new StreamObserver<StreamRequest>() {
      @Override
      public void onNext(StreamRequest req) {
        switch (req.getRequestCase()) {
          case INIT:
            marksupport.set(req.getInit().getMarkSupport());
            responseObserver.onNext(StreamReply.newBuilder().setCmd(StreamCmd.INIT).setArg(id).build());
            break;
          default:
            queue.offer(req.getDataChunk());
        }
      }

      @Override
      public void onError(Throwable t) {
        logger.error("inputstream client error", t);
        map.remove(id);
        queue.offer(t);
      }

      @Override
      public void onCompleted() {
        map.remove(id);
        queue.offer(null);
      }
    };
    InputStream in = new InputStream() {
      @Override
      public int read() throws IOException {
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.READ)
            .setArg(1)
            .build());
        StreamRequest.DataChunk data = awaitResponse();
        return data.getData().toByteArray()[0];
      }

      private StreamRequest.DataChunk awaitResponse() throws IOException {
        try {
          Object d = queue.take();
          if (d == null) {
            throw new EOFException();
          } else if (d instanceof Throwable) {
            return handleException((Throwable)d);
          } else {
            return (DataChunk) d;
          }
        } catch (InterruptedException e) {
          throw new IOException(e);
        }
      }

      private DataChunk handleException(Throwable d) throws IOException {
        if (d instanceof StatusRuntimeException) {
          Status status = ((StatusRuntimeException) d).getStatus();
          if (status.getCode() == Status.Code.INTERNAL) {
            throw new IOException(status.getDescription());
          }
        }
        throw new IOException(d);
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.READ)
            .setArg(len)
            .build());
        DataChunk chunk = awaitResponse();
        if (chunk.getRet() == -1) {
          return -1;
        }
        ByteString data = chunk.getData();
        data.copyTo(b, off);
        return chunk.getData().size();
      }

      @Override
      public synchronized long skip(long n) throws IOException {
        if (n < 0) {
          return 0;
        }
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.SKIP)
            .setArg(n)
            .build());
        DataChunk chunk = awaitResponse();
        return chunk.getRet();
      }

      @Override
      public synchronized int available() throws IOException {
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.AVAIL)
            .clearArg()
            .build());
        DataChunk chunk = awaitResponse();
        return (int) chunk.getRet();
      }

      @Override
      public synchronized void mark(int readlimit) {
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.MARK)
            .setArg(readlimit)
            .build());
        try {
          awaitResponse();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean markSupported() {
        return marksupport.get();
      }

      @Override
      public synchronized void reset() throws IOException {
        responseObserver.onNext(StreamReply.newBuilder()
            .setCmd(StreamCmd.RESET)
            .clearArg()
            .build());
        awaitResponse();
      }

      @Override
      public synchronized void close() throws IOException {
        responseObserver.onCompleted();
        map.remove(id);
      }
    };
    map.put(id, in);
    return observer;
  }

  public InputStream get(int id) {
    return map.get(id);
  }

}
