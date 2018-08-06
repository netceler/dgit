package io.insight.jgit;

import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.CoreConfig;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.sha1.SHA1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import io.insight.jgit.services.KVObjectService;

public class KVObjectInserter extends ObjectInserter {
    private final String repositoryName;

    private final KVObjectDatabase objectDatabase;

    private final KVObjectService objectService;

    private Deflater deflate;

    KVObjectInserter(final KVObjectDatabase objectDatabase, final KVObjectService objectService) {
        this.objectDatabase = objectDatabase;
        this.objectService = objectService;
        this.repositoryName = objectDatabase.getRepository().getRepositoryName();
    }

    @Override
    public ObjectId insert(final int objectType, final long length, final InputStream is) throws IOException {
        final byte[] buf = buffer();
        if (length < buf.length) {
            final int actLen = IO.readFully(is, buf, 0);
            final ObjectId id = idFor(objectType, buf);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DeflaterOutputStream compressOs = compress(baos);
            compressOs.write(buf, 0, actLen);
            compressOs.finish();
            final byte[] compressedBuf = baos.toByteArray();
            final ByteArrayInputStream in = new ByteArrayInputStream(compressedBuf, 0, compressedBuf.length);
            objectService.insertLooseObject(repositoryName, id, objectType, length, in, compressedBuf.length);
            return id;
        } else {
            final File tempFile = newTempFile();
            final FileOutputStream fos = new FileOutputStream(tempFile);
            final DeflaterOutputStream cOut = compress(fos);
            final SHA1 md = digest();
            @SuppressWarnings("resource")
            final SHA1OutputStream dOut = new SHA1OutputStream(cOut, md);
            long len = length;
            while (len > 0) {
                final int n = is.read(buf, 0, (int) Math.min(len, buf.length));
                if (n <= 0) {
                    throw new EOFException(MessageFormat.format(JGitText.get().inputDidntMatchLength, len));
                }
                dOut.write(buf, 0, n);
                len -= n;
            }
            dOut.flush();
            cOut.finish();
            final ObjectId id = md.toObjectId();
            fos.close();
            final FileInputStream fin = new FileInputStream(tempFile) {
                @Override
                public void close() throws IOException {
                    super.close();
                    FileUtils.delete(tempFile, FileUtils.RETRY);
                }
            };
            final long fileSize = fin.getChannel().size();
            objectService.insertLooseObject(repositoryName, id, objectType, length, fin, fileSize);
            return id;
        }
    }

    private File newTempFile() throws IOException {
        return File.createTempFile("noz", null);
    }

    private DeflaterOutputStream compress(final OutputStream out) {
        if (deflate == null) {
            final int compressionLevel = objectDatabase.getConfig().get(CoreConfig.KEY).getCompression();
            deflate = new Deflater(compressionLevel);
        } else {
            deflate.reset();
        }
        return new DeflaterOutputStream(out, deflate, 8192);
    }

    private static class SHA1OutputStream extends FilterOutputStream {
        private final SHA1 md;

        SHA1OutputStream(final OutputStream out, final SHA1 md) {
            super(out);
            this.md = md;
        }

        @Override
        public void write(final int b) throws IOException {
            md.update((byte) b);
            out.write(b);
        }

        @Override
        public void write(final byte[] in, final int p, final int n) throws IOException {
            md.update(in, p, n);
            out.write(in, p, n);
        }
    }

    @Override
    public PackParser newPackParser(final InputStream in) throws IOException {
        return new KVPackParser(objectService, this.objectDatabase, in);
    }

    @Override
    public ObjectReader newReader() {
        return new KVObjectReader(objectDatabase.getRepository(), objectService);
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() {

    }
}