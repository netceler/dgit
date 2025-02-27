import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

@Testcontainers
public class InMemoryRepoTest extends BaseRepoTest {

    @TempDir
    File baseDir;

    @Override
    Repository createRepo() throws IOException {
        return new InMemoryRepository(new DfsRepositoryDescription("test"));
    }
    
}
