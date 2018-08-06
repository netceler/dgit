package io.insight.jgit.jdbc;

import static io.insight.jgit.jdbc.jooq.Tables.GIT_REFS;

import org.eclipse.jgit.lib.Ref;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.util.Collection;

import io.insight.jgit.KVRef;
import io.insight.jgit.jdbc.jooq.tables.records.GitRefsRecord;
import io.insight.jgit.services.KVRefService;

public class JdbcRefService implements KVRefService {
    private final JdbcAdapter jdbcAdapter;

    public JdbcRefService(final JdbcAdapter jdbcAdapter) {
        this.jdbcAdapter = jdbcAdapter;
    }

    private KVRef toRef(final GitRefsRecord rec) {
        if (rec == null) {
            return null;
        }
        KVRef ref;
        if (rec.getSymbolic() == 1) {
            ref = KVRef.createSymbolic(rec.getName(), rec.getTarget());
        } else {
            ref = KVRef.createObject(rec.getName(), rec.getObjectId(), rec.getStorage());
        }
        return ref;
    }

    @Override
    public Collection<KVRef> getAllRefs(final String repositoryName) throws IOException {
        return jdbcAdapter.withDSLContext(
                dsl -> dsl.select().from(GIT_REFS).where(GIT_REFS.REPO.equal(repositoryName)).fetch(
                        r -> toRef(r.into(GIT_REFS))));
    }

    private boolean compare(final Ref expect, final GitRefsRecord ref) {
        if (expect == null) {
            return ref == null;
        } else if (expect.getStorage() == Ref.Storage.NEW) {
            return ref == null;
        } else if (expect.isSymbolic()) {
            return ref != null && ref.getSymbolic() == 1
                    && ref.getTarget().equals(expect.getTarget().getName());
        } else {
            if (ref == null) {
                return false;
            }
            if (ref.getObjectId() == null) {
                return expect.getObjectId() == null;
            } else {
                return ref.getObjectId().equals(expect.getObjectId().name());
            }
        }
    }

    @Override
    public boolean compareAndPut(final String repositoryName, final Ref old, final Ref nw)
            throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> dsl.transactionResult(configuration -> {
            GitRefsRecord rec = DSL.using(configuration).select().from(GIT_REFS).where(
                    GIT_REFS.REPO.eq(repositoryName).and(GIT_REFS.NAME.eq(old.getName()))).fetchOneInto(
                            GIT_REFS);
            if (compare(old, rec)) {
                final boolean insert = rec == null;
                if (rec == null) {
                    rec = DSL.using(configuration).newRecord(GIT_REFS);
                }
                rec.setName(nw.getName());
                rec.setRepo(repositoryName);
                rec.setSymbolic((byte) (nw.isSymbolic() ? 1 : 0));
                rec.setTarget(nw.getTarget() == null ? null : nw.getTarget().getName());
                rec.setObjectId(nw.getObjectId() == null ? null : nw.getObjectId().getName());
                rec.setStorage(nw.getStorage() == null ? null : nw.getStorage().name());
                final int updated = insert ? rec.insert() : rec.update();
                return updated > 0;
            } else {
                return false;
            }
        }));
    }

    @Override
    public boolean compareAndRemove(final String repositoryName, final Ref old) throws IOException {
        return jdbcAdapter.withDSLContext(dsl -> dsl.transactionResult(configuration -> {
            final GitRefsRecord rec = DSL.using(configuration).select().from(GIT_REFS).where(
                    GIT_REFS.REPO.eq(repositoryName).and(GIT_REFS.NAME.eq(old.getName()))).fetchOneInto(
                            GIT_REFS);
            if (compare(old, rec)) {
                return rec.delete() > 0;
            }
            return false;
        }));
    }
}