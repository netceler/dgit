package io.insight.jgit.server.utils;


import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FileCachedByteBuffer {

  public static int MEM_CACHE_THRESHOLD = 512 * 1024;
  public static int MAX_SIZE = 1024 * 1024 * 1024;

  public static ClosableByteBuf createBuffer(int length) throws IOException {

    if (length <= MEM_CACHE_THRESHOLD) {
      return new ClosableByteBuf(Unpooled.buffer(length),() -> {});
    } else {
      final File tempFile = File.createTempFile("cache_file", null);
      RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
      MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
      ByteBuf buf = Unpooled.wrappedBuffer(bb);
      buf.setIndex(0, 0);
      return new ClosableByteBuf(buf, () -> {
        raf.close();
        tempFile.delete();
      });
    }
  }

  public static ClosableByteBuf createBuffer() throws IOException {
    return createBuffer(MAX_SIZE);
  }

  public static class ClosableByteBuf  implements AutoCloseable {
    private ByteBuf buf;
    private AutoCloseable closeable;

    ClosableByteBuf(ByteBuf buf, AutoCloseable closeable) {
      this.buf = buf;
      this.closeable = closeable;
    }

    public ByteBuf buf() {
      return buf;
    }

    public ByteBuffer nioBuffer(){
      return buf.nioBuffer();
    }

    @Override
    public void close() throws Exception {
      closeable.close();
    }

    public void readFrom(ByteString data) {
      ByteBuffer bb = buf.nioBuffer(buf.writerIndex(), buf.writableBytes());
      data.copyTo(bb);
      buf.writerIndex(buf.writerIndex() + data.size());
    }
  }


}
