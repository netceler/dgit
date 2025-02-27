import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

@Testcontainers
public class FileRepoTest extends BaseRepoTest {

    @TempDir
    File baseDir;

    @Override
    Repository createRepo() throws IOException {
        baseDir.mkdirs();
        new File(baseDir, "objects").mkdir();
        return new FileRepository(baseDir);
    }
}
