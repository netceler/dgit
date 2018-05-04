package io.insight.jgit;

import io.insight.jgit.services.KVObjectService;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class KVObjectReader extends ObjectReader {

  private KVObjectService objectService;



  KVObjectReader(KVObjectService objectService) {
    this.objectService = objectService;
  }

  @Override
  public ObjectReader newReader() {
    return new KVObjectReader(objectService);
  }

  @Override
  public Collection<ObjectId> resolve(AbbreviatedObjectId id) throws IOException {
    return objectService.resolve(id.name());
  }

  @Override
  public ObjectLoader open(AnyObjectId objectId, int typeHint) throws MissingObjectException, IncorrectObjectTypeException, IOException {
    KVObject object= objectService.loadObject(objectId);
    if (object == null) {
      if (typeHint == OBJ_ANY)
        throw new MissingObjectException(objectId.copy(),
            JGitText.get().unknownObjectType2);
      throw new MissingObjectException(objectId.copy(), typeHint);
    }
    return new KVObjectLoader(object,objectService);
  }

  @Override
  public Set<ObjectId> getShallowCommits() throws IOException {
    return Collections.emptySet();
  }

  @Override
  public void close() {

  }
}
