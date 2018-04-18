package io.insigit.jgit.grpc;

import io.insight.jgit.OpenReply;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class GrpcClientObjectLoader extends ObjectLoader {
  private final OpenReply first;
  private final Iterator<OpenReply> iterator;

  public GrpcClientObjectLoader(Iterator<OpenReply> iterator) {
    first = iterator.next();
    this.iterator = iterator;
  }

  @Override
  public int getType() {
    return first.getType();
  }

  @Override
  public boolean isLarge() {
    return first.getIsLarge();
  }

  @Override
  public long getSize() {
    return first.getSize();
  }

  @Override
  public byte[] getCachedBytes() throws LargeObjectException {
    if (!first.getIsLarge()) {
      return first.getData().toByteArray();
    }
    throw new LargeObjectException();
  }

  @Override
  public void copyTo(OutputStream out) throws MissingObjectException, IOException {
    if (isLarge()) {
      super.copyTo(out);
    } else {
      first.getData().writeTo(out);
    }
  }

  ObjectStream stream = null;

  @Override
  public ObjectStream openStream() throws MissingObjectException, IOException {
    if (isLarge()) {
      if (stream == null) {
        stream = new ObjectStream() {
          private InputStream curr = first.getData().newInput();

          @Override
          public int getType() {
            return first.getType();
          }

          @Override
          public long getSize() {
            return first.getSize();
          }

          @Override
          public int read() throws IOException {
            int read = curr.read();
            if (read == -1) {
              if (iterator.hasNext()) {
                curr = iterator.next().getData().newInput();
                return curr.read();
              } else {
                return -1;
              }
            }
            return read;
          }
        };
      }
      return stream;
    } else {
      return new ObjectStream.SmallStream(first.getType(), first.getData().toByteArray());
    }
  }
}