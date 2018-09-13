import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevBlob;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import io.insight.jgit.KVRepoManager;
import io.insight.jgit.cache.CachedRepoManager;
import io.insight.jgit.jdbc.SqlRepoManager;

public class JdbcRepoTest {

    private RevWalk rw;

    public String repoName = "test";

    @SuppressWarnings("rawtypes")
    private TestRepository tr;

    private Repository repo;

    private KVRepoManager repoManager;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setUp() throws Exception {

        repoManager = new SqlRepoManager("jdbc:mysql://localhost:3306/test", "root", "lambdalab-dev");
        repoManager = new CachedRepoManager(repoManager);
        if (repoManager.exists(repoName)) {
            repoManager.delete(repoName);
        }
        repoManager.create(repoName);
        repo = repoManager.open(repoName);
        // repo = new InMemoryRepository(new DfsRepositoryDescription("test"));
        // repo = new FileRepository("/tmp/test");
        tr = new TestRepository(repo);
        rw = tr.getRevWalk();
    }

    @After
    public void tearDown() throws IOException {
        repo.close();
        rw.close();
        // repoManager.delete(repoName);
    }

    @Test
    public void insertChangeId() throws Exception {
        final RevCommit c1 = tr.commit().message("message").insertChangeId().create();
        rw.parseBody(c1);
        assertTrue(Pattern.matches("^message\n\nChange-Id: I[0-9a-f]{40}\n$", c1.getFullMessage()));

        final RevCommit c2 = tr.commit().message("").insertChangeId().create();
        rw.parseBody(c2);
        assertEquals("\n\nChange-Id: I0000000000000000000000000000000000000000\n", c2.getFullMessage());
    }

    @Test
    public void insertChangeIdIgnoresExisting() throws Exception {
        final String msg = "message\n" + "\n" + "Change-Id: Ideadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n";
        final RevCommit c = tr.commit().message(msg).insertChangeId().create();
        rw.parseBody(c);
        assertEquals(msg, c.getFullMessage());
    }

    @Test
    public void insertExplicitChangeId() throws Exception {
        final RevCommit c = tr.commit().message("message").insertChangeId(
                "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef").create();
        rw.parseBody(c);
        assertEquals("message\n\n" + "Change-Id: Ideadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n",
                c.getFullMessage());
    }

    @Test
    public void resetFromSymref() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        Ref head = repo.exactRef("HEAD");
        final RevCommit master = tr.branch("master").commit().create();
        final RevCommit branch = tr.branch("branch").commit().create();
        final RevCommit detached = tr.commit().create();

        assertTrue(head.isSymbolic());
        assertEquals("refs/heads/master", head.getTarget().getName());
        assertEquals(master, repo.exactRef("refs/heads/master").getObjectId());
        assertEquals(branch, repo.exactRef("refs/heads/branch").getObjectId());

        // Reset to branches preserves symref.
        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertEquals(master, head.getObjectId());
        assertTrue(head.isSymbolic());
        assertEquals("refs/heads/master", head.getTarget().getName());

        tr.reset("branch");
        head = repo.exactRef("HEAD");
        assertEquals(branch, head.getObjectId());
        assertTrue(head.isSymbolic());
        assertEquals("refs/heads/master", head.getTarget().getName());
        final ObjectId lastHeadBeforeDetach = head.getObjectId().copy();

        // Reset to a SHA-1 detaches.
        tr.reset(detached);
        head = repo.exactRef("HEAD");
        assertEquals(detached, head.getObjectId());
        assertFalse(head.isSymbolic());

        tr.reset(detached.name());
        head = repo.exactRef("HEAD");
        assertEquals(detached, head.getObjectId());
        assertFalse(head.isSymbolic());

        // Reset back to a branch remains detached.
        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertEquals(lastHeadBeforeDetach, head.getObjectId());
        assertFalse(head.isSymbolic());
    }

    @Test
    public void resetFromDetachedHead() throws Exception {
        Ref head = repo.exactRef("HEAD");
        final RevCommit master = tr.branch("master").commit().create();
        final RevCommit branch = tr.branch("branch").commit().create();
        final RevCommit detached = tr.commit().create();

        // assertNull(head);
        assertEquals(master, repo.exactRef("refs/heads/master").getObjectId());
        assertEquals(branch, repo.exactRef("refs/heads/branch").getObjectId());

        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertEquals(master, head.getObjectId());
        // assertFalse(head.isSymbolic());

        tr.reset("branch");
        head = repo.exactRef("HEAD");
        assertEquals(branch, head.getObjectId());
        // assertFalse(head.isSymbolic());

        tr.reset(detached);
        head = repo.exactRef("HEAD");
        assertEquals(detached, head.getObjectId());
        // assertFalse(head.isSymbolic());

        tr.reset(detached.name());
        head = repo.exactRef("HEAD");
        assertEquals(detached, head.getObjectId());
        // assertFalse(head.isSymbolic());
    }

    @Test
    public void amendRef() throws Exception {
        final RevCommit root = tr.commit().add("todelete", "to be deleted").create();
        final RevCommit orig = tr.commit().parent(root).rm("todelete").add("foo", "foo contents").add("bar",
                "bar contents").add("dir/baz", "baz contents").create();
        rw.parseBody(orig);
        tr.branch("master").update(orig);
        assertEquals("foo contents", blobAsString(orig, "foo"));
        assertEquals("bar contents", blobAsString(orig, "bar"));
        assertEquals("baz contents", blobAsString(orig, "dir/baz"));

        final RevCommit amended = tr.amendRef("master").tick(3).add("bar", "fixed bar contents").create();
        assertEquals(amended, repo.exactRef("refs/heads/master").getObjectId());
        rw.parseBody(amended);

        assertEquals(1, amended.getParentCount());
        assertEquals(root, amended.getParent(0));
        assertEquals(orig.getFullMessage(), amended.getFullMessage());
        assertEquals(orig.getAuthorIdent(), amended.getAuthorIdent());

        // Committer name/email is the same, but time was incremented.
        assertEquals(new PersonIdent(orig.getCommitterIdent(), new Date(0)),
                new PersonIdent(amended.getCommitterIdent(), new Date(0)));
        assertTrue(orig.getCommitTime() < amended.getCommitTime());

        assertEquals("foo contents", blobAsString(amended, "foo"));
        assertEquals("fixed bar contents", blobAsString(amended, "bar"));
        assertEquals("baz contents", blobAsString(amended, "dir/baz"));
        assertNull(TreeWalk.forPath(repo, "todelete", amended.getTree()));
    }

    @Test
    public void amendHead() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        final RevCommit root = tr.commit().add("foo", "foo contents").create();
        final RevCommit orig = tr.commit().parent(root).message("original message").add("bar",
                "bar contents").create();
        tr.branch("master").update(orig);

        final RevCommit amended = tr.amendRef("HEAD").add("foo", "fixed foo contents").create();

        final Ref head = repo.exactRef(Constants.HEAD);
        assertEquals(amended, head.getObjectId());
        assertTrue(head.isSymbolic());
        assertEquals("refs/heads/master", head.getTarget().getName());

        rw.parseBody(amended);
        assertEquals("original message", amended.getFullMessage());
        assertEquals("fixed foo contents", blobAsString(amended, "foo"));
        assertEquals("bar contents", blobAsString(amended, "bar"));
    }

    @Test
    public void amendCommit() throws Exception {
        final RevCommit root = tr.commit().add("foo", "foo contents").create();
        final RevCommit orig = tr.commit().parent(root).message("original message").add("bar",
                "bar contents").create();
        final RevCommit amended = tr.amend(orig.copy()).add("foo", "fixed foo contents").create();

        rw.parseBody(amended);
        assertEquals("original message", amended.getFullMessage());
        assertEquals("fixed foo contents", blobAsString(amended, "foo"));
        assertEquals("bar contents", blobAsString(amended, "bar"));
    }

    @Test
    public void commitToUnbornHead() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        final RevCommit root = tr.branch("HEAD").commit().create();
        final Ref ref = repo.exactRef(Constants.HEAD);
        assertEquals(root, ref.getObjectId());
        assertTrue(ref.isSymbolic());
        assertEquals("refs/heads/master", ref.getTarget().getName());
    }

    @Test
    public void cherryPick() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        final RevCommit head = tr.branch("master").commit().add("foo", "foo contents\n").create();
        rw.parseBody(head);
        final RevCommit toPick = tr.commit().parent(tr.commit().create()) // Can't
                                                                          // cherry-pick
                                                                          // root.
                .author(new PersonIdent("Cherrypick Author", "cpa@example.com", tr.getDate(),
                        tr.getTimeZone())).author(
                                new PersonIdent("Cherrypick Committer", "cpc@example.com", tr.getDate(),
                                        tr.getTimeZone())).message("message to cherry-pick").add("bar",
                                                "bar contents\n").create();
        final RevCommit result = tr.cherryPick(toPick);
        rw.parseBody(result);

        final Ref headRef = tr.getRepository().exactRef("HEAD");
        assertEquals(result, headRef.getObjectId());
        assertTrue(headRef.isSymbolic());
        assertEquals("refs/heads/master", headRef.getLeaf().getName());

        assertEquals(1, result.getParentCount());
        assertEquals(head, result.getParent(0));
        assertEquals(toPick.getAuthorIdent(), result.getAuthorIdent());

        // Committer name/email matches default, and time was incremented.
        assertEquals(new PersonIdent(head.getCommitterIdent(), new Date(0)),
                new PersonIdent(result.getCommitterIdent(), new Date(0)));
        assertTrue(toPick.getCommitTime() < result.getCommitTime());

        assertEquals("message to cherry-pick", result.getFullMessage());
        assertEquals("foo contents\n", blobAsString(result, "foo"));
        assertEquals("bar contents\n", blobAsString(result, "bar"));
    }

    @Test
    public void cherryPickWithContentMerge() throws Exception {
        final RevCommit base = tr.branch("HEAD").commit().add("foo", "foo contents\n\n").create();
        tr.branch("HEAD").commit().add("foo", "foo contents\n\nlast line\n").create();
        final RevCommit toPick = tr.commit().message("message to cherry-pick").parent(base).add("foo",
                "changed foo contents\n\n").create();
        final RevCommit result = tr.cherryPick(toPick);
        rw.parseBody(result);

        assertEquals("message to cherry-pick", result.getFullMessage());
        assertEquals("changed foo contents\n\nlast line\n", blobAsString(result, "foo"));
    }

    @Test
    public void cherryPickWithIdenticalContents() throws Exception {
        final RevCommit base = tr.branch("HEAD").commit().add("foo", "foo contents\n").create();
        final RevCommit head = tr.branch("HEAD").commit().parent(base).add("bar", "bar contents\n").create();
        final RevCommit toPick = tr.commit().parent(base).message("message to cherry-pick").add("bar",
                "bar contents\n").create();
        assertNotEquals(head, toPick);
        assertNull(tr.cherryPick(toPick));
        assertEquals(head, repo.exactRef("HEAD").getObjectId());
    }

    @Test
    public void reattachToMaster_Race() throws Exception {
        final RevCommit commit = tr.branch("master").commit().create();
        tr.branch("master").update(commit);
        tr.branch("other").update(commit);
        repo.updateRef("HEAD").link("refs/heads/master");

        // Create a detached HEAD that is not an .
        tr.reset(commit);
        final Ref head = repo.exactRef("HEAD");
        assertEquals(commit, head.getObjectId());
        assertFalse(head.isSymbolic());

        // Try to reattach to master.
        final RefUpdate refUpdate = repo.updateRef("HEAD");

        // Make a change during reattachment.
        repo.updateRef("HEAD").link("refs/heads/other");

        assertEquals(RefUpdate.Result.LOCK_FAILURE, refUpdate.link("refs/heads/master"));
    }

    @Test
    public void nonRacingChange() throws Exception {
        tr.branch("master").update(tr.branch("master").commit().create());
        tr.branch("other").update(tr.branch("other").commit().create());
        repo.updateRef("HEAD").link("refs/heads/master");

        // Try to update HEAD.
        final RefUpdate refUpdate = repo.updateRef("HEAD");

        // Proceed a master. This should not affect changing HEAD.
        tr.branch("master").update(tr.branch("master").commit().create());

        assertEquals(RefUpdate.Result.FORCED, refUpdate.link("refs/heads/other"));
    }

    private String blobAsString(final AnyObjectId treeish, final String path) throws Exception {
        final RevObject obj = tr.get(rw.parseTree(treeish), path);
        assertSame(RevBlob.class, obj.getClass());
        final ObjectLoader loader = rw.getObjectReader().open(obj);
        return new String(loader.getCachedBytes(), UTF_8);
    }
}
