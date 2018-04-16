package io.insigit.jgit.services;


import io.insigit.jgit.RpcObjDatabase;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.PackParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public interface RpcObjectService {
  Collection<ObjectId> resolve(AbbreviatedObjectId id);

  Set<ObjectId> getShallowCommits();

  ObjectLoader open(AnyObjectId objectId, int typeHint) throws IOException;

  ObjectId insert(int inserterId,int objectType, long length, InputStream in) throws IOException;

  PackParser newPackParser(RpcObjDatabase odb, InputStream in) throws IOException;

  ObjectInserter newInserter(RpcObjDatabase odb);
}
