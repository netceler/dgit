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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseRepoTest {
    private RevWalk rw;
    @SuppressWarnings("rawtypes")
    private TestRepository tr;
    private Repository repo;

    abstract Repository createRepo() throws IOException;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() throws Exception {
        repo = createRepo();
        tr = new TestRepository(repo);
        rw = tr.getRevWalk();
        tr.branch("main").commit().create();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (repo != null) {
            repo.close();
        }
        if (rw != null) {
            rw.close();
        }
        // repoManager.delete(repoName);
    }

    @Test
    void insertChangeId() throws Exception {
        final RevCommit c1 = tr.commit().message("message").insertChangeId().create();
        rw.parseBody(c1);
        assertThat(Pattern.matches("^message\n\nChange-Id: I[0-9a-f]{40}\n$", c1.getFullMessage())).isTrue();

        final RevCommit c2 = tr.commit().message("").insertChangeId().create();
        rw.parseBody(c2);
        assertThat(c2.getFullMessage()).isEqualTo("\n\nChange-Id: I0000000000000000000000000000000000000000\n");
    }

    @Test
    void insertChangeIdIgnoresExisting() throws Exception {
        final String msg = "message\n" + "\n" + "Change-Id: Ideadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n";
        final RevCommit c = tr.commit().message(msg).insertChangeId().create();
        rw.parseBody(c);
        assertThat(c.getFullMessage()).isEqualTo(msg);
    }

    @Test
    void insertExplicitChangeId() throws Exception {
        final RevCommit c = tr.commit().message("message").insertChangeId(
                "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef").create();
        rw.parseBody(c);
        assertThat(c.getFullMessage()).isEqualTo("message\n\n" + "Change-Id: Ideadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n");
    }

    @Test
    void resetFromSymref() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        Ref head = repo.exactRef("HEAD");
        final RevCommit master = tr.branch("master").commit().create();
        final RevCommit branch = tr.branch("branch").commit().create();
        final RevCommit detached = tr.commit().create();

        assertThat(head.isSymbolic()).isTrue();
        assertThat(head.getTarget().getName()).isEqualTo("refs/heads/master");
        assertThat(repo.exactRef("refs/heads/master").getObjectId()).isEqualTo(master);
        assertThat(repo.exactRef("refs/heads/branch").getObjectId()).isEqualTo(branch);

        // Reset to branches preserves symref.
        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(master);
        assertThat(head.isSymbolic()).isTrue();
        assertThat(head.getTarget().getName()).isEqualTo("refs/heads/master");

        tr.reset("branch");
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(branch);
        assertThat(head.isSymbolic()).isTrue();
        assertThat(head.getTarget().getName()).isEqualTo("refs/heads/master");
        final ObjectId lastHeadBeforeDetach = head.getObjectId().copy();

        // Reset to a SHA-1 detaches.
        tr.reset(detached);
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(detached);
        assertThat(head.isSymbolic()).isFalse();

        tr.reset(detached.name());
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(detached);
        assertThat(head.isSymbolic()).isFalse();

        // Reset back to a branch remains detached.
        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(lastHeadBeforeDetach);
        assertThat(head.isSymbolic()).isFalse();
    }

    @Test
    void resetFromDetachedHead() throws Exception {
        Ref head = repo.exactRef("HEAD");
        final RevCommit master = tr.branch("master").commit().create();
        final RevCommit branch = tr.branch("branch").commit().create();
        final RevCommit detached = tr.commit().create();

        // assertNull(head);
        assertThat(repo.exactRef("refs/heads/master").getObjectId()).isEqualTo(master);
        assertThat(repo.exactRef("refs/heads/branch").getObjectId()).isEqualTo(branch);

        tr.reset("master");
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(master);
        // assertFalse(head.isSymbolic());

        tr.reset("branch");
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(branch);
        // assertFalse(head.isSymbolic());

        tr.reset(detached);
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(detached);
        // assertFalse(head.isSymbolic());

        tr.reset(detached.name());
        head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(detached);
        // assertFalse(head.isSymbolic());
    }

    @Test
    void amendRef() throws Exception {
        final RevCommit root = tr.commit().add("todelete", "to be deleted").create();
        final RevCommit orig = tr.commit().parent(root).rm("todelete").add("foo", "foo contents").add("bar",
                "bar contents").add("dir/baz", "baz contents").create();
        rw.parseBody(orig);
        tr.branch("master").update(orig);
        assertThat(blobAsString(orig, "foo")).isEqualTo("foo contents");
        assertThat(blobAsString(orig, "bar")).isEqualTo("bar contents");
        assertThat(blobAsString(orig, "dir/baz")).isEqualTo("baz contents");

        final RevCommit amended = tr.amendRef("master").tick(3).add("bar", "fixed bar contents").create();
        assertThat(repo.exactRef("refs/heads/master").getObjectId()).isEqualTo(amended);
        rw.parseBody(amended);

        assertThat(amended.getParentCount()).isEqualTo(1);
        assertThat(amended.getParent(0)).isEqualTo(root);
        assertThat(amended.getFullMessage()).isEqualTo(orig.getFullMessage());
        assertThat(amended.getAuthorIdent()).isEqualTo(orig.getAuthorIdent());

        // Committer name/email is the same, but time was incremented.
        assertThat(new PersonIdent(amended.getCommitterIdent(), new Date(0))).isEqualTo(new PersonIdent(orig.getCommitterIdent(), new Date(0)));
        assertThat(orig.getCommitTime() < amended.getCommitTime()).isTrue();

        assertThat(blobAsString(amended, "foo")).isEqualTo("foo contents");
        assertThat(blobAsString(amended, "bar")).isEqualTo("fixed bar contents");
        assertThat(blobAsString(amended, "dir/baz")).isEqualTo("baz contents");
        assertThat(TreeWalk.forPath(repo, "todelete", amended.getTree())).isNull();
    }

    @Test
    void amendHead() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        final RevCommit root = tr.commit().add("foo", "foo contents").create();
        final RevCommit orig = tr.commit().parent(root).message("original message").add("bar",
                "bar contents").create();
        tr.branch("master").update(orig);

        final RevCommit amended = tr.amendRef("HEAD").add("foo", "fixed foo contents").create();

        final Ref head = repo.exactRef(Constants.HEAD);
        assertThat(head.getObjectId()).isEqualTo(amended);
        assertThat(head.isSymbolic()).isTrue();
        assertThat(head.getTarget().getName()).isEqualTo("refs/heads/master");

        rw.parseBody(amended);
        assertThat(amended.getFullMessage()).isEqualTo("original message");
        assertThat(blobAsString(amended, "foo")).isEqualTo("fixed foo contents");
        assertThat(blobAsString(amended, "bar")).isEqualTo("bar contents");
    }

    @Test
    void amendCommit() throws Exception {
        final RevCommit root = tr.commit().add("foo", "foo contents").create();
        final RevCommit orig = tr.commit().parent(root).message("original message").add("bar",
                "bar contents").create();
        final RevCommit amended = tr.amend(orig.copy()).add("foo", "fixed foo contents").create();

        rw.parseBody(amended);
        assertThat(amended.getFullMessage()).isEqualTo("original message");
        assertThat(blobAsString(amended, "foo")).isEqualTo("fixed foo contents");
        assertThat(blobAsString(amended, "bar")).isEqualTo("bar contents");
    }

    @Test
    void commitToUnbornHead() throws Exception {
        repo.updateRef("HEAD").link("refs/heads/master");
        final RevCommit root = tr.branch("HEAD").commit().create();
        final Ref ref = repo.exactRef(Constants.HEAD);
        assertThat(ref.getObjectId()).isEqualTo(root);
        assertThat(ref.isSymbolic()).isTrue();
        assertThat(ref.getTarget().getName()).isEqualTo("refs/heads/master");
    }

    @Test
    void cherryPick() throws Exception {
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
        assertThat(headRef.getObjectId()).isEqualTo(result);
        assertThat(headRef.isSymbolic()).isTrue();
        assertThat(headRef.getLeaf().getName()).isEqualTo("refs/heads/master");

        assertThat(result.getParentCount()).isEqualTo(1);
        assertThat(result.getParent(0)).isEqualTo(head);
        assertThat(result.getAuthorIdent()).isEqualTo(toPick.getAuthorIdent());

        // Committer name/email matches default, and time was incremented.
        assertThat(new PersonIdent(result.getCommitterIdent(), new Date(0))).isEqualTo(new PersonIdent(head.getCommitterIdent(), new Date(0)));
        assertThat(toPick.getCommitTime() < result.getCommitTime()).isTrue();

        assertThat(result.getFullMessage()).isEqualTo("message to cherry-pick");
        assertThat(blobAsString(result, "foo")).isEqualTo("foo contents\n");
        assertThat(blobAsString(result, "bar")).isEqualTo("bar contents\n");
    }

    @Test
    void cherryPickWithContentMerge() throws Exception {
        final RevCommit base = tr.branch("HEAD").commit().add("foo", "foo contents\n\n").create();
        tr.branch("HEAD").commit().add("foo", "foo contents\n\nlast line\n").create();
        final RevCommit toPick = tr.commit().message("message to cherry-pick").parent(base).add("foo",
                "changed foo contents\n\n").create();
        final RevCommit result = tr.cherryPick(toPick);
        rw.parseBody(result);

        assertThat(result.getFullMessage()).isEqualTo("message to cherry-pick");
        assertThat(blobAsString(result, "foo")).isEqualTo("changed foo contents\n\nlast line\n");
    }

    @Test
    void cherryPickWithIdenticalContents() throws Exception {
        final RevCommit base = tr.branch("HEAD").commit().add("foo", "foo contents\n").create();
        final RevCommit head = tr.branch("HEAD").commit().parent(base).add("bar", "bar contents\n").create();
        final RevCommit toPick = tr.commit().parent(base).message("message to cherry-pick").add("bar",
                "bar contents\n").create();
        assertThat(toPick).isNotEqualTo(head);
        assertThat(tr.cherryPick(toPick)).isNull();
        assertThat(repo.exactRef("HEAD").getObjectId()).isEqualTo(head);
    }

    @Test
    void reattachToMaster_Race() throws Exception {
        final RevCommit commit = tr.branch("master").commit().create();
        tr.branch("master").update(commit);
        tr.branch("other").update(commit);
        repo.updateRef("HEAD").link("refs/heads/master");

        // Create a detached HEAD that is not an .
        tr.reset(commit);
        final Ref head = repo.exactRef("HEAD");
        assertThat(head.getObjectId()).isEqualTo(commit);
        assertThat(head.isSymbolic()).isFalse();

        // Try to reattach to master.
        final RefUpdate refUpdate = repo.updateRef("HEAD");

        // Make a change during reattachment.
        repo.updateRef("HEAD").link("refs/heads/other");

        assertThat(refUpdate.link("refs/heads/master")).isEqualTo(RefUpdate.Result.LOCK_FAILURE);
    }

    @Test
    void nonRacingChange() throws Exception {
        tr.branch("master").update(tr.branch("master").commit().create());
        tr.branch("other").update(tr.branch("other").commit().create());
        repo.updateRef("HEAD").link("refs/heads/master");

        // Try to update HEAD.
        final RefUpdate refUpdate = repo.updateRef("HEAD");

        // Proceed a master. This should not affect changing HEAD.
        tr.branch("master").update(tr.branch("master").commit().create());

        assertThat(refUpdate.link("refs/heads/other")).isEqualTo(RefUpdate.Result.FORCED);
    }

    private String blobAsString(final AnyObjectId treeish, final String path) throws Exception {
        final RevObject obj = tr.get(rw.parseTree(treeish), path);
        assertThat(obj.getClass()).isSameAs(RevBlob.class);
        final ObjectLoader loader = rw.getObjectReader().open(obj);
        return new String(loader.getCachedBytes(), UTF_8);
    }

}
