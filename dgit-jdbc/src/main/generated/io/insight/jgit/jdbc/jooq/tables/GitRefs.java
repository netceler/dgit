/*
 * This file is generated by jOOQ.
*/
package io.insight.jgit.jdbc.jooq.tables;


import io.insight.jgit.jdbc.jooq.Indexes;
import io.insight.jgit.jdbc.jooq.Keys;
import io.insight.jgit.jdbc.jooq.Test;
import io.insight.jgit.jdbc.jooq.tables.records.GitRefsRecord;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class GitRefs extends TableImpl<GitRefsRecord> {

    private static final long serialVersionUID = 359994652;

    /**
     * The reference instance of <code>test.git_refs</code>
     */
    public static final GitRefs GIT_REFS = new GitRefs();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<GitRefsRecord> getRecordType() {
        return GitRefsRecord.class;
    }

    /**
     * The column <code>test.git_refs.repo</code>.
     */
    public final TableField<GitRefsRecord, String> REPO = createField("repo", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>test.git_refs.name</code>.
     */
    public final TableField<GitRefsRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>test.git_refs.symbolic</code>.
     */
    public final TableField<GitRefsRecord, Byte> SYMBOLIC = createField("symbolic", org.jooq.impl.SQLDataType.TINYINT, this, "");

    /**
     * The column <code>test.git_refs.target</code>.
     */
    public final TableField<GitRefsRecord, String> TARGET = createField("target", org.jooq.impl.SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>test.git_refs.storage</code>.
     */
    public final TableField<GitRefsRecord, String> STORAGE = createField("storage", org.jooq.impl.SQLDataType.VARCHAR(16), this, "");

    /**
     * The column <code>test.git_refs.object_id</code>.
     */
    public final TableField<GitRefsRecord, String> OBJECT_ID = createField("object_id", org.jooq.impl.SQLDataType.CHAR(40), this, "");

    /**
     * Create a <code>test.git_refs</code> table reference
     */
    public GitRefs() {
        this(DSL.name("git_refs"), null);
    }

    /**
     * Create an aliased <code>test.git_refs</code> table reference
     */
    public GitRefs(String alias) {
        this(DSL.name(alias), GIT_REFS);
    }

    /**
     * Create an aliased <code>test.git_refs</code> table reference
     */
    public GitRefs(Name alias) {
        this(alias, GIT_REFS);
    }

    private GitRefs(Name alias, Table<GitRefsRecord> aliased) {
        this(alias, aliased, null);
    }

    private GitRefs(Name alias, Table<GitRefsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Test.TEST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.GIT_REFS_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<GitRefsRecord> getPrimaryKey() {
        return Keys.KEY_GIT_REFS_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<GitRefsRecord>> getKeys() {
        return Arrays.<UniqueKey<GitRefsRecord>>asList(Keys.KEY_GIT_REFS_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitRefs as(String alias) {
        return new GitRefs(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitRefs as(Name alias) {
        return new GitRefs(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public GitRefs rename(String name) {
        return new GitRefs(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public GitRefs rename(Name name) {
        return new GitRefs(name, null);
    }
}
