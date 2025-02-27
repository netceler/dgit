package io.insight.jgit.internal;

import static org.eclipse.jgit.lib.Ref.Storage.NEW;

import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SymbolicRef;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RefList;
import org.eclipse.jgit.util.RefMap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DfsRefDatabase extends RefDatabase {
    private final Repository repository;

    private final AtomicReference<RefCache> cache;

    /**
     * Initialize the reference database for a repository.
     * @param repository the repository this database instance manages
     *            references for.
     */
    protected DfsRefDatabase(final Repository repository) {
        this.repository = repository;
        this.cache = new AtomicReference<>();
    }

    /**
     * Get the repository the database holds the references of.
     * @return the repository the database holds the references of.
     */
    protected Repository getRepository() {
        return repository;
    }

    boolean exists() throws IOException {
        return 0 < read().size();
    }

    /** {@inheritDoc} */
    @Override
    public Ref exactRef(final String name) throws IOException {
        final RefCache curr = read();
        final Ref ref = curr.ids.get(name);
        return ref != null ? resolve(ref, 0, curr.ids) : null;
    }

    /** {@inheritDoc} */
    @Override
    public List<Ref> getAdditionalRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Ref> getRefs(final String prefix) throws IOException {
        final RefCache curr = read();
        final RefList<Ref> packed = RefList.emptyList();
        RefList<Ref> loose = curr.ids;
        final RefList.Builder<Ref> sym = new RefList.Builder<>(curr.sym.size());

        for (int idx = 0; idx < curr.sym.size(); idx++) {
            Ref ref = curr.sym.get(idx);
            final String name = ref.getName();
            ref = resolve(ref, 0, loose);
            if (ref != null && ref.getObjectId() != null) {
                sym.add(ref);
            } else {
                // A broken symbolic reference, we have to drop it from the
                // collections the client is about to receive. Should be a
                // rare occurrence so pay a copy penalty.
                final int toRemove = loose.find(name);
                if (0 <= toRemove) {
                    loose = loose.remove(toRemove);
                }
            }
        }

        return new RefMap(prefix, packed, loose, sym.toRefList());
    }

    private Ref resolve(final Ref ref, final int depth, final RefList<Ref> loose) throws IOException {
        if (!ref.isSymbolic()) {
            return ref;
        }

        Ref dst = ref.getTarget();

        if (MAX_SYMBOLIC_REF_DEPTH <= depth) {
            return null; // claim it doesn't exist
        }

        dst = loose.get(dst.getName());
        if (dst == null) {
            return ref;
        }

        dst = resolve(dst, depth + 1, loose);
        if (dst == null) {
            return null;
        }
        return new SymbolicRef(ref.getName(), dst);
    }

    /** {@inheritDoc} */
    @Override
    public Ref peel(final Ref ref) throws IOException {
        final Ref oldLeaf = ref.getLeaf();
        if (oldLeaf.isPeeled() || oldLeaf.getObjectId() == null) {
            return ref;
        }

        final Ref newLeaf = doPeel(oldLeaf);

        final RefCache cur = read();
        final int idx = cur.ids.find(oldLeaf.getName());
        if (0 <= idx && cur.ids.get(idx) == oldLeaf) {
            final RefList<Ref> newList = cur.ids.set(idx, newLeaf);
            cache.compareAndSet(cur, new RefCache(newList, cur));
            cachePeeledState(oldLeaf, newLeaf);
        }

        return recreate(ref, newLeaf);
    }

    Ref doPeel(final Ref leaf) throws MissingObjectException, IOException {
        try (RevWalk rw = new RevWalk(repository)) {
            final RevObject obj = rw.parseAny(leaf.getObjectId());
            if (obj instanceof RevTag) {
                return new ObjectIdRef.PeeledTag(leaf.getStorage(), leaf.getName(), leaf.getObjectId(),
                        rw.peel(obj).copy());
            } else {
                return new ObjectIdRef.PeeledNonTag(leaf.getStorage(), leaf.getName(), leaf.getObjectId());
            }
        }
    }

    static Ref recreate(final Ref old, final Ref leaf) {
        if (old.isSymbolic()) {
            final Ref dst = recreate(old.getTarget(), leaf);
            return new SymbolicRef(old.getName(), dst);
        }
        return leaf;
    }

    /** {@inheritDoc} */
    @Override
    public RefUpdate newUpdate(final String refName, final boolean detach) throws IOException {
        boolean detachingSymbolicRef = false;
        Ref ref = exactRef(refName);
        if (ref == null) {
            ref = new ObjectIdRef.Unpeeled(NEW, refName, null);
        } else {
            detachingSymbolicRef = detach && ref.isSymbolic();
        }

        final DfsRefUpdate update = new DfsRefUpdate(this, ref);
        if (detachingSymbolicRef) {
            update.setDetachingSymbolicRef();
        }
        return update;
    }

    /** {@inheritDoc} */
    @Override
    public RefRename newRename(final String fromName, final String toName) throws IOException {
        final RefUpdate src = newUpdate(fromName, true);
        final RefUpdate dst = newUpdate(toName, true);
        return new DfsRefRename(src, dst);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNameConflicting(final String refName) throws IOException {
        final RefList<Ref> all = read().ids;

        // Cannot be nested within an existing reference.
        int lastSlash = refName.lastIndexOf('/');
        while (0 < lastSlash) {
            final String needle = refName.substring(0, lastSlash);
            if (all.contains(needle)) {
                return true;
            }
            lastSlash = refName.lastIndexOf('/', lastSlash - 1);
        }

        // Cannot be the container of an existing reference.
        final String prefix = refName + '/';
        final int idx = -(all.find(prefix) + 1);
        if (idx < all.size() && all.get(idx).getName().startsWith(prefix)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void create() {
        // Nothing to do.
    }

    /** {@inheritDoc} */
    @Override
    public void refresh() {
        clearCache();
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        clearCache();
    }

    void clearCache() {
        cache.set(null);
    }

    void stored(final Ref ref) {
        RefCache oldCache, newCache;
        do {
            oldCache = cache.get();
            if (oldCache == null) {
                return;
            }
            newCache = oldCache.put(ref);
        } while (!cache.compareAndSet(oldCache, newCache));
    }

    void removed(final String refName) {
        RefCache oldCache, newCache;
        do {
            oldCache = cache.get();
            if (oldCache == null) {
                return;
            }
            newCache = oldCache.remove(refName);
        } while (!cache.compareAndSet(oldCache, newCache));
    }

    private RefCache read() throws IOException {
        RefCache c = cache.get();
        if (c == null) {
            c = scanAllRefs();
            cache.set(c);
        }
        return c;
    }

    /**
     * Read all known references in the repository.
     * @return all current references of the repository.
     * @throws java.io.IOException references cannot be accessed.
     */
    protected abstract RefCache scanAllRefs() throws IOException;

    /**
     * Compare a reference, and put if it matches.
     * <p>
     * Two reference match if and only if they satisfy the following:
     * <ul>
     * <li>If one reference is a symbolic ref, the other one should be a
     * symbolic ref.
     * <li>If both are symbolic refs, the target names should be same.
     * <li>If both are object ID refs, the object IDs should be same.
     * </ul>
     * @param oldRef old value to compare to. If the reference is expected to
     *            not exist the old value has a storage of
     *            {@link org.eclipse.jgit.lib.Ref.Storage#NEW} and an ObjectId
     *            value of {@code null}.
     * @param newRef new reference to store.
     * @return true if the put was successful; false otherwise.
     * @throws java.io.IOException the reference cannot be put due to a system
     *             error.
     */
    protected abstract boolean compareAndPut(Ref oldRef, Ref newRef) throws IOException;

    /**
     * Compare a reference, and delete if it matches.
     * @param oldRef the old reference information that was previously read.
     * @return true if the remove was successful; false otherwise.
     * @throws java.io.IOException the reference could not be removed due to a
     *             system error.
     */
    protected abstract boolean compareAndRemove(Ref oldRef) throws IOException;

    /**
     * Update the cached peeled state of a reference
     * <p>
     * The ref database invokes this method after it peels a reference that had
     * not been peeled before. This allows the storage to cache the peel state
     * of the reference, and if it is actually peelable, the target that it
     * peels to, so that on-the-fly peeling doesn't have to happen on the next
     * reference read.
     * @param oldLeaf the old reference.
     * @param newLeaf the new reference, with peel information.
     */
    protected void cachePeeledState(final Ref oldLeaf, final Ref newLeaf) {
        try {
            compareAndPut(oldLeaf, newLeaf);
        } catch (final IOException e) {
            // Ignore an exception during caching.
        }
    }

    public static class RefCache {
        final RefList<Ref> ids;

        final RefList<Ref> sym;

        /**
         * Initialize a new reference cache.
         * <p>
         * The two reference lists supplied must be sorted in correct order
         * (string compare order) by name.
         * @param ids references that carry an ObjectId, and all of {@code sym}.
         * @param sym references that are symbolic references to others.
         */
        public RefCache(final RefList<Ref> ids, final RefList<Ref> sym) {
            this.ids = ids;
            this.sym = sym;
        }

        RefCache(final RefList<Ref> ids, final RefCache old) {
            this(ids, old.sym);
        }

        /** @return number of references in this cache. */
        public int size() {
            return ids.size();
        }

        /**
         * Find a reference by name.
         * @param name full name of the reference.
         * @return the reference, if it exists, otherwise null.
         */
        public Ref get(final String name) {
            return ids.get(name);
        }

        /**
         * Obtain a modified copy of the cache with a ref stored.
         * <p>
         * This cache instance is not modified by this method.
         * @param ref reference to add or replace.
         * @return a copy of this cache, with the reference added or replaced.
         */
        public RefCache put(final Ref ref) {
            final RefList<Ref> newIds = this.ids.put(ref);
            RefList<Ref> newSym = this.sym;
            if (ref.isSymbolic()) {
                newSym = newSym.put(ref);
            } else {
                final int p = newSym.find(ref.getName());
                if (0 <= p) {
                    newSym = newSym.remove(p);
                }
            }
            return new RefCache(newIds, newSym);
        }

        /**
         * Obtain a modified copy of the cache with the ref removed.
         * <p>
         * This cache instance is not modified by this method.
         * @param refName reference to remove, if it exists.
         * @return a copy of this cache, with the reference removed.
         */
        public RefCache remove(final String refName) {
            RefList<Ref> newIds = this.ids;
            int p = newIds.find(refName);
            if (0 <= p) {
                newIds = newIds.remove(p);
            }

            RefList<Ref> newSym = this.sym;
            p = newSym.find(refName);
            if (0 <= p) {
                newSym = newSym.remove(p);
            }
            return new RefCache(newIds, newSym);
        }
    }
}
