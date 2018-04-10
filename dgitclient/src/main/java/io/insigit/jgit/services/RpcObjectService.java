package io.insigit.jgit.services;


import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public interface RpcObjectService {
  Collection<ObjectId> resolve(AbbreviatedObjectId id);

  Set<ObjectId> getShallowCommits();

  ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException;

  ObjectId insert(int objectType, long length, InputStream in) throws IOException;
}
