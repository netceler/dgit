package io.insight.jgit;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import io.insight.jgit.services.KVObjectService;

public class KVObjectReader extends ObjectReader {

    private final KVRepository repository;

    private final KVObjectService objectService;

    KVObjectReader(final KVRepository repository, final KVObjectService objectService) {
        this.repository = repository;
        this.objectService = objectService;
    }

    @Override
    public ObjectReader newReader() {
        return new KVObjectReader(repository, objectService);
    }

    @Override
    public Collection<ObjectId> resolve(final AbbreviatedObjectId id) throws IOException {
        return objectService.resolve(repository.getRepositoryName(), id.name());
    }

    @Override
    public ObjectLoader open(final AnyObjectId objectId, final int typeHint)
            throws MissingObjectException, IncorrectObjectTypeException, IOException {
        final KVObject object = objectService.loadObject(repository.getRepositoryName(), objectId);
        if (object == null) {
            if (typeHint == OBJ_ANY) {
                throw new MissingObjectException(objectId.copy(), JGitText.get().unknownObjectType2);
            }
            throw new MissingObjectException(objectId.copy(), typeHint);
        }
        return new KVObjectLoader(repository.getRepositoryName(), object, objectService);
    }

    @Override
    public Set<ObjectId> getShallowCommits() throws IOException {
        return Collections.emptySet();
    }

    @Override
    public void close() {

    }
}
