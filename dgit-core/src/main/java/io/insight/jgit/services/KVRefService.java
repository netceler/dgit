package io.insight.jgit.services;

import io.insight.jgit.KVRef;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface KVRefService {

  Collection<KVRef> getAllRefs() throws IOException;

  boolean compareAndPut(Ref old, Ref nw) throws IOException;

  boolean compareAndRemove(Ref old) throws IOException;


}
