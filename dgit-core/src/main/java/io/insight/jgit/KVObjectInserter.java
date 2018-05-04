package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.sha1.SHA1;

import java.io.*;
import java.text.MessageFormat;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class KVObjectInserter extends ObjectInserter {
  private KVObjectDatabase objectDatabase;
  private KVObjectService objectService;
  private Deflater deflate;


  KVObjectInserter(KVObjectDatabase objectDatabase, KVObjectService objectService) {
    this.objectDatabase = objectDatabase;
    this.objectService = objectService;
  }

  @Override
  public ObjectId insert(int objectType, long length, InputStream is) throws IOException {
    byte[] buf = buffer();
    if (length < buf.length) {
      int actLen = IO.readFully(is, buf, 0);
      ObjectId id = idFor(objectType, buf);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DeflaterOutputStream compressOs = compress(baos);
      compressOs.write(buf, 0, actLen);
      compressOs.finish();
      byte[] compressedBuf = baos.toByteArray();
      ByteArrayInputStream in = new ByteArrayInputStream(compressedBuf, 0, compressedBuf.length);
      objectService.insertLooseObject(id, objectType, length, in, compressedBuf.length);
      return id;
    } else {
      File tempFile = newTempFile();
      FileOutputStream fos = new FileOutputStream(tempFile);
      DeflaterOutputStream cOut = compress(fos);
      SHA1 md = digest();
      SHA1OutputStream dOut = new SHA1OutputStream(cOut, md);
      long len = length;
      while (len > 0) {
        int n = is.read(buf, 0, (int) Math.min(len, buf.length));
        if (n <= 0)
          throw new EOFException(MessageFormat.format(
              JGitText.get().inputDidntMatchLength, len));
        dOut.write(buf, 0, n);
        len -= n;
      }
      dOut.flush();
      cOut.finish();
      ObjectId id = md.toObjectId();
      fos.close();
      FileInputStream fin = new FileInputStream(tempFile){
        @Override
        public void close() throws IOException {
          super.close();
          FileUtils.delete(tempFile, FileUtils.RETRY);
        }
      };
      long fileSize = fin.getChannel().size();
      objectService.insertLooseObject(id, objectType, length, fin, fileSize);
      return id;
    }
  }

  private File newTempFile() throws IOException {
    return File.createTempFile("noz", null);
  }

  private DeflaterOutputStream compress(final OutputStream out) {
    if (deflate == null) {
      int compressionLevel = objectDatabase.getConfig().get(CoreConfig.KEY).getCompression();
      deflate = new Deflater(compressionLevel);
    } else
      deflate.reset();
    return new DeflaterOutputStream(out, deflate, 8192);
  }

  private static class SHA1OutputStream extends FilterOutputStream {
    private final SHA1 md;

    SHA1OutputStream(OutputStream out, SHA1 md) {
      super(out);
      this.md = md;
    }

    @Override
    public void write(int b) throws IOException {
      md.update((byte) b);
      out.write(b);
    }

    @Override
    public void write(byte[] in, int p, int n) throws IOException {
      md.update(in, p, n);
      out.write(in, p, n);
    }
  }

  @Override
  public PackParser newPackParser(InputStream in) throws IOException {
    return new KVPackParser(objectService, this.objectDatabase, in);
  }

  @Override
  public ObjectReader newReader() {
    return new KVObjectReader(objectService);
  }

  @Override
  public void flush() throws IOException {

  }

  @Override
  public void close() {

  }
}
