package io.insight.jgit.server.utils;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.SlicedByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FileCachedByteBuffer  {

  public static int MEM_CACHE_THRESHOLD = 512 * 1024;

  public static ClosableByteBuf createBuffer(int length) throws IOException {

    if (length <= MEM_CACHE_THRESHOLD){
      return new ClosableByteBuf(Unpooled.buffer(length)){
        @Override
        public void close() throws Exception { }
      };
    } else {
      final File tempFile = File.createTempFile("cache_file", null);
      RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
      MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, length);
      ByteBuf buf = Unpooled.wrappedBuffer(bb);
      return new ClosableByteBuf(buf) {
        @Override
        public void close() throws Exception {
          raf.close();
          tempFile.delete();
        }
      };
    }
  }

  public static abstract class ClosableByteBuf extends SlicedByteBuf implements AutoCloseable {
    ClosableByteBuf(ByteBuf buffer) {
      super(buffer, 0, buffer.capacity());
    }

    @Override
    public ReferenceCounted touch() {
      return this.unwrap().touch();
    }

    @Override
    public ReferenceCounted touch(Object o) {
      return this.unwrap().touch(o);
    }
  }


}
