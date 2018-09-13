package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_OBJECTS;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.jooq.DSLContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Collectors;

import io.insight.jgit.KVObject;
import io.insight.jgit.jdbc.jooq.tables.records.GitObjectsRecord;
import io.insight.jgit.services.KVObjectService;

public class JdbcObjService implements KVObjectService {
    private final JdbcAdapter jdbcAdapter;

    public JdbcObjService(final JdbcAdapter jdbcAdapter) {
        this.jdbcAdapter = jdbcAdapter;
    }

    @Override
    public Collection<ObjectId> resolve(final String repositoryName, final String name) throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> dsl.select(GIT_OBJECTS.OBJECT_ID).from(GIT_OBJECTS).where(
                GIT_OBJECTS.REPO.equal(repositoryName).and(GIT_OBJECTS.OBJECT_ID.like(name + "%"))).fetch(
                        GIT_OBJECTS.OBJECT_ID).stream().map(ObjectId::fromString).collect(
                                Collectors.toList()));
    }

    @Override
    public void insertLooseObject(final String repositoryName, final AnyObjectId objectId,
            final int objectType, final long inflatedSize, final InputStream in, final long length)
            throws IOException {
        jdbcAdapter.withDSLContext(dsl -> {
            insertObject(repositoryName, objectId, objectType, inflatedSize, inflatedSize, null, in, length,
                    dsl);
            return null;
        });
    }

    @Override
    public KVObject loadObject(final String repositoryName, final AnyObjectId objectId) throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> getKvObject(repositoryName, objectId.name(), dsl));
    }

    private KVObject getKvObject(final String repositoryName, final String objectId, final DSLContext dsl) {
        final GitObjectsRecord rec = dsl.select(GIT_OBJECTS.OBJECT_ID, GIT_OBJECTS.TYPE, GIT_OBJECTS.SIZE,
                GIT_OBJECTS.BASE, GIT_OBJECTS.TOTAL_SIZE).from(GIT_OBJECTS).where(
                        GIT_OBJECTS.REPO.eq(repositoryName).and(
                                GIT_OBJECTS.OBJECT_ID.eq(objectId))).fetchAnyInto(GIT_OBJECTS);
        if (rec == null) {
            return null;
        }
        final long totalSize = rec.getTotalSize() != null ? rec.getTotalSize() : rec.getSize();
        final KVObject obj = new KVObject(rec.getObjectId(), rec.getType(), rec.getSize(), totalSize);
        if (rec.getBase() != null) {
            obj.setBase(getKvObject(repositoryName, rec.getBase(), dsl));
        }
        return obj;
    }

    @Override
    public byte[] getObjectData(final String repositoryName, final AnyObjectId objectId) throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> dsl.select(GIT_OBJECTS.DATA).from(GIT_OBJECTS).where(
                GIT_OBJECTS.REPO.eq(repositoryName).and(GIT_OBJECTS.OBJECT_ID.eq(objectId.name()))).fetchAny(
                        GIT_OBJECTS.DATA));
    }

    @Override
    public void insertPackedObject(final String repositoryName, final AnyObjectId objectId,
            final int objectType, final long inflatedSize, final long totalSize, final String base,
            final SeekableByteChannel channel, final long offset, final long size) throws IOException {
        jdbcAdapter.withDSLContext(dsl -> {
            dsl.transaction(configuration -> {
                channel.position(offset);
                final InputStream in = Channels.newInputStream(channel);
                insertObject(repositoryName, objectId, objectType, inflatedSize, totalSize, base, in, size,
                        dsl);
            });
            return null;
        });
    }

    private void insertObject(final String repositoryName, final AnyObjectId objectId, final int objectType,
            final long inflatedSize, final long totalSize, final String base, final InputStream in,
            final long size, final DSLContext dsl) throws SQLException, IOException {
        final byte[] targetArray = new byte[in.available()];
        in.read(targetArray);
        dsl.insertInto(GIT_OBJECTS, GIT_OBJECTS.OBJECT_ID, GIT_OBJECTS.TYPE, GIT_OBJECTS.SIZE,
                GIT_OBJECTS.REPO, GIT_OBJECTS.BASE, GIT_OBJECTS.DATA, GIT_OBJECTS.TOTAL_SIZE).values(
                        objectId.name(), objectType, inflatedSize, repositoryName, base, targetArray,
                        totalSize).onDuplicateKeyIgnore().execute();
    }
}