package io.insight.jgit;

import org.eclipse.jgit.lib.BaseRepositoryBuilder;

public class KVRepositoryBuilder extends BaseRepositoryBuilder<KVRepositoryBuilder, KVRepository> {
    private String name;

    public String getName() {
        return name;
    }

    public KVRepositoryBuilder setRepositoryName(final String name) {
        this.name = name;
        return self();
    }
}
