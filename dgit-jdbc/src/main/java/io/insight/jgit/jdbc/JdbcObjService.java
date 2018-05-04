package io.insight.jgit.jdbc;

import io.insight.jgit.KVObject;
import io.insight.jgit.jdbc.jooq.tables.records.GitObjectsRecord;
import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Collectors;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_OBJECTS;

public class JdbcObjService implements KVObjectService {
  private final JdbcAdapter jdbcAdapter;
  private final String repositoryName;

  public JdbcObjService(JdbcAdapter jdbcAdapter, String repositoryName) {
    this.jdbcAdapter = jdbcAdapter;
    this.repositoryName = repositoryName;
  }

  @Override
  public Collection<ObjectId> resolve(String name) throws IOException {
    return jdbcAdapter.withDSLContext(dsl -> dsl.select(GIT_OBJECTS.OBJECT_ID).from(GIT_OBJECTS)
        .where(GIT_OBJECTS.REPO.equal(repositoryName).and(GIT_OBJECTS.OBJECT_ID.like(name + "%")))
        .fetch(GIT_OBJECTS.OBJECT_ID).stream().map(ObjectId::fromString).collect(Collectors.toList()));
  }

  @Override
  public void insertLooseObject(AnyObjectId objectId, int objectType, long inflatedSize, InputStream in, long length)
      throws IOException {
    jdbcAdapter.withDSLContext(dsl -> {
      insertObject(objectId, objectType, inflatedSize, inflatedSize,null, in, length, dsl);
      return null;
    });
  }

  @Override
  public KVObject loadObject(AnyObjectId objectId) throws IOException {
    return jdbcAdapter.withDSLContext(dsl -> getKvObject(objectId.name(), dsl));
  }

  private KVObject getKvObject(String objectId, DSLContext dsl) {
    GitObjectsRecord rec = dsl.select(
        GIT_OBJECTS.OBJECT_ID, GIT_OBJECTS.TYPE, GIT_OBJECTS.SIZE, GIT_OBJECTS.BASE, GIT_OBJECTS.TOTAL_SIZE
    ).from(GIT_OBJECTS)
        .where(GIT_OBJECTS.REPO.eq(repositoryName).and(GIT_OBJECTS.OBJECT_ID.eq(objectId)))
        .fetchOneInto(GIT_OBJECTS);
    if (rec == null) return null;
    long totalSize = rec.getTotalSize() != null ? rec.getTotalSize() : rec.getSize();
    KVObject obj = new KVObject(rec.getObjectId(), rec.getType(), rec.getSize(), totalSize);
    if (rec.getBase() != null) {
      obj.setBase(getKvObject(rec.getBase(), dsl));
    }
    return obj;
  }

  @Override
  public byte[] getObjectData(AnyObjectId objectId) throws IOException {
    return jdbcAdapter.withDSLContext(dsl ->
        dsl.select(GIT_OBJECTS.DATA).from(GIT_OBJECTS)
            .where(GIT_OBJECTS.REPO.eq(repositoryName).and(GIT_OBJECTS.OBJECT_ID.eq(objectId.name())))
            .fetchOne(GIT_OBJECTS.DATA));
  }

  @Override
  public void insertPackedObject(AnyObjectId objectId,
                                 int objectType,
                                 long inflatedSize,
                                 long totalSize,
                                 String base,
                                 SeekableByteChannel channel,
                                 long offset,
                                 long size) throws IOException {
    jdbcAdapter.withDSLContext(dsl -> {
      dsl.transaction(configuration -> {
        channel.position(offset);
        InputStream in = Channels.newInputStream(channel);
        insertObject(objectId, objectType, inflatedSize,totalSize, base, in, size, dsl);
      });
      return null;
    });
  }

  private void insertObject(AnyObjectId objectId,
                            int objectType,
                            long inflatedSize,
                            long totalSize,
                            String base,
                            InputStream in,
                            long size,
                            DSLContext dsl) throws SQLException, IOException {
    String sql = dsl.insertInto(GIT_OBJECTS,
        GIT_OBJECTS.OBJECT_ID,
        GIT_OBJECTS.TYPE,
        GIT_OBJECTS.SIZE,
        GIT_OBJECTS.REPO,
        GIT_OBJECTS.BASE,
        GIT_OBJECTS.DATA,
        GIT_OBJECTS.TOTAL_SIZE).values(objectId.name(), objectType, inflatedSize, repositoryName, base, null, totalSize)
        .onDuplicateKeyIgnore().getSQL();
    ConnectionProvider provider = dsl.configuration().connectionProvider();
    Connection conn = provider.acquire();
    try {
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setString(1, objectId.name());
      stmt.setInt(2, objectType);
      stmt.setLong(3, inflatedSize);
      stmt.setString(4, repositoryName);
      stmt.setString(5, base);
      stmt.setBinaryStream(6, in, size);
      stmt.setLong(7, totalSize);
      stmt.executeUpdate();
    } finally {
      provider.release(conn);
    }
  }

}
