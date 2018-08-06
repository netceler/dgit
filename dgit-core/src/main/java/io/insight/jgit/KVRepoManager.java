package io.insight.jgit;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.DaemonClient;
import org.eclipse.jgit.transport.ServiceMayNotContinueException;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.util.FS;

import java.io.IOException;

import io.insight.jgit.services.KVAdapter;

public interface KVRepoManager extends RepositoryResolver<DaemonClient> {
    @Override
    default Repository open(final DaemonClient req, final String name)
            throws RepositoryNotFoundException, ServiceMayNotContinueException {
        try {
            if (!exists(name)) {
                throw new RepositoryNotFoundException(name);
            }
        } catch (final IOException e) {
            throw new ServiceMayNotContinueException(e);
        }
        return open(name);
    }

    default Repository open(final String name) throws RepositoryNotFoundException {
        final KVRepositoryBuilder options = new KVRepositoryBuilder();
        options.setRepositoryName(name);
        options.setFS(FS.detect());
        return new KVRepository(this, options);
    }

    boolean exists(String name) throws IOException;

    Repository create(String name) throws IOException;

    void delete(String name) throws IOException;

    KVAdapter adapter();
}