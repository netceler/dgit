package io.insight.jgit.services;

import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;

import io.insight.Middleware;
import io.insight.jgit.KVRef;

@Middleware
public interface KVRefService {

    Collection<KVRef> getAllRefs(String repositoryName) throws IOException;

    boolean compareAndPut(String repositoryName, Ref old, Ref nw) throws IOException;

    boolean compareAndRemove(String repositoryName, Ref old) throws IOException;
}