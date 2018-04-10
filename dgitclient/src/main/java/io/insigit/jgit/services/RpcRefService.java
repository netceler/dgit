package io.insigit.jgit.services;

import org.eclipse.jgit.lib.Ref;

import java.util.Collection;

public interface RpcRefService {
  Collection<Ref> getAll();

  boolean compareAndPut(Ref oldRef, Ref newRef);

  boolean compareAndRemove(Ref oldRef);
}
