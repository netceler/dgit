package io.insight.jgit.jdbc;

import io.insight.jgit.KVRef;
import io.insight.jgit.jdbc.jooq.tables.records.GitRefsRecord;
import io.insight.jgit.services.KVRefService;
import org.eclipse.jgit.lib.Ref;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.util.Collection;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_REFS;

public class JdbcRefService implements KVRefService {
  private JdbcAdapter jdbcAdapter;
  private String repositoryName;

  public JdbcRefService(JdbcAdapter jdbcAdapter, String repositoryName) {
    this.jdbcAdapter = jdbcAdapter;
    this.repositoryName = repositoryName;
  }

  private KVRef toRef(GitRefsRecord rec) {
    if (rec == null) return null;
    KVRef ref;
    if (rec.getSymbolic() == 1) {
      ref = KVRef.createSymbolic(rec.getName(), rec.getTarget());
    } else {
      ref = KVRef.createObject(rec.getName(), rec.getObjectId(), rec.getStorage());
    }
    return ref;
  }


  @Override
  public Collection<KVRef> getAllRefs() throws IOException {
    return jdbcAdapter.withDSLContext(dsl ->
        dsl.select().from(GIT_REFS).where(GIT_REFS.REPO.equal(repositoryName))
            .fetch(r -> toRef(r.into(GIT_REFS))));
  }

  private boolean compare(Ref expect, GitRefsRecord ref) {
    if (expect == null) {
      return ref == null;
    } else if (expect.getStorage() == Ref.Storage.NEW) {
      return ref == null;
    } else if (expect.isSymbolic()) {
      return ref != null && ref.getSymbolic() == 1 && ref.getTarget().equals(expect.getTarget().getName());
    } else {
      if(ref==null) return false;
      String name = expect.getObjectId().name();
      if (ref.getObjectId() == null) {
        return expect.getObjectId() == null;
      } else {
        return ref.getObjectId().equals(expect.getObjectId().name());
      }
    }
  }

  @Override
  public boolean compareAndPut(Ref old, Ref nw) throws IOException {
    return jdbcAdapter.withDSLContext(dsl -> dsl.transactionResult(configuration -> {
      GitRefsRecord rec = DSL.using(configuration).select().from(GIT_REFS)
          .where(GIT_REFS.REPO.eq(repositoryName).and(GIT_REFS.NAME.eq(old.getName()))).fetchOneInto(GIT_REFS);
      if (compare(old, rec)) {
        boolean insert = rec == null;
        if (insert) {
          rec = DSL.using(configuration).newRecord(GIT_REFS);
        }
        rec.setName(nw.getName());
        rec.setRepo(repositoryName);
        rec.setSymbolic((byte) (nw.isSymbolic() ? 1 : 0));
        rec.setTarget(nw.getTarget() == null ? null : nw.getTarget().getName());
        rec.setObjectId(nw.getObjectId() == null ? null : nw.getObjectId().getName());
        rec.setStorage(nw.getStorage() == null ? null : nw.getStorage().name());
        int updated = insert ? rec.insert() : rec.update();
        return updated > 0;
      } else
        return false;
    }));
  }


  @Override
  public boolean compareAndRemove(Ref old) throws IOException {
    return jdbcAdapter.withDSLContext(dsl -> dsl.transactionResult(configuration -> {
      GitRefsRecord rec = DSL.using(configuration).select().from(GIT_REFS)
          .where(GIT_REFS.REPO.eq(repositoryName).and(GIT_REFS.NAME.eq(old.getName()))).fetchOneInto(GIT_REFS);
      if (compare(old, rec)) {
        return rec.delete() > 0;
      }
      return false;
    }));
  }
}
