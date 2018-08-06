/*
 * This file is generated by jOOQ.
*/
package io.insight.jgit.jdbc.jooq.tables;


import io.insight.jgit.jdbc.jooq.Indexes;
import io.insight.jgit.jdbc.jooq.Keys;
import io.insight.jgit.jdbc.jooq.Test;
import io.insight.jgit.jdbc.jooq.tables.records.GitConfigRecord;

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
public class GitConfig extends TableImpl<GitConfigRecord> {

    private static final long serialVersionUID = -1189670840;

    /**
     * The reference instance of <code>test.git_config</code>
     */
    public static final GitConfig GIT_CONFIG = new GitConfig();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<GitConfigRecord> getRecordType() {
        return GitConfigRecord.class;
    }

    /**
     * The column <code>test.git_config.repo</code>.
     */
    public final TableField<GitConfigRecord, String> REPO = createField("repo", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>test.git_config.config</code>.
     */
    public final TableField<GitConfigRecord, String> CONFIG = createField("config", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * Create a <code>test.git_config</code> table reference
     */
    public GitConfig() {
        this(DSL.name("git_config"), null);
    }

    /**
     * Create an aliased <code>test.git_config</code> table reference
     */
    public GitConfig(String alias) {
        this(DSL.name(alias), GIT_CONFIG);
    }

    /**
     * Create an aliased <code>test.git_config</code> table reference
     */
    public GitConfig(Name alias) {
        this(alias, GIT_CONFIG);
    }

    private GitConfig(Name alias, Table<GitConfigRecord> aliased) {
        this(alias, aliased, null);
    }

    private GitConfig(Name alias, Table<GitConfigRecord> aliased, Field<?>[] parameters) {
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
        return Arrays.<Index>asList(Indexes.GIT_CONFIG_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<GitConfigRecord> getPrimaryKey() {
        return Keys.KEY_GIT_CONFIG_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<GitConfigRecord>> getKeys() {
        return Arrays.<UniqueKey<GitConfigRecord>>asList(Keys.KEY_GIT_CONFIG_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitConfig as(String alias) {
        return new GitConfig(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitConfig as(Name alias) {
        return new GitConfig(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public GitConfig rename(String name) {
        return new GitConfig(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public GitConfig rename(Name name) {
        return new GitConfig(name, null);
    }
}