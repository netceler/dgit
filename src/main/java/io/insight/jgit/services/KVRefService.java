package io.insight.jgit.services;

import io.insight.jgit.KVRef;
import org.eclipse.jgit.lib.ObjectId;

import java.util.Collection;
import java.util.Map;

public interface KVRefService {

  Map<String, KVRef> getAllRefs();

  boolean compareAndPut(KVRef old, KVRef nw);

  boolean compareAndRemove(KVRef old);
}
