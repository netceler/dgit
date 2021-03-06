import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;

public class T {
    public static void main(final String[] args) throws IOException {
        final Git git = Git.open(new File("/tmp/dgit"));
        final Repository repo = git.getRepository();
        final ObjectLoader o = repo.getObjectDatabase().open(
                ObjectId.fromString("6d847541c7372bffe3309e070e75d23aeaf1ecf5"));
        System.out.println("o.getSize() = " + o.getSize());
        System.out.println("o.bytes() = " + o.getCachedBytes().length);
    }
}