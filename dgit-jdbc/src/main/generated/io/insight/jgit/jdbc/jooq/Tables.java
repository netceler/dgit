/*
 * This file is generated by jOOQ.
*/
package io.insight.jgit.jdbc.jooq;


import io.insight.jgit.jdbc.jooq.tables.GitConfig;
import io.insight.jgit.jdbc.jooq.tables.GitObjects;
import io.insight.jgit.jdbc.jooq.tables.GitRefs;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in test
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.6"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>test.git_config</code>.
     */
    public static final GitConfig GIT_CONFIG = io.insight.jgit.jdbc.jooq.tables.GitConfig.GIT_CONFIG;

    /**
     * The table <code>test.git_objects</code>.
     */
    public static final GitObjects GIT_OBJECTS = io.insight.jgit.jdbc.jooq.tables.GitObjects.GIT_OBJECTS;

    /**
     * The table <code>test.git_refs</code>.
     */
    public static final GitRefs GIT_REFS = io.insight.jgit.jdbc.jooq.tables.GitRefs.GIT_REFS;
}
