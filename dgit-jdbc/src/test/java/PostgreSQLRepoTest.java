import io.insight.jgit.KVRepoManager;
import io.insight.jgit.cache.CachedRepoManager;
import io.insight.jgit.jdbc.SqlRepoManager;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

@Testcontainers
public class PostgreSQLRepoTest extends BaseRepoTest {
    @Container
    private final static JdbcDatabaseContainer DATABASE = new PostgreSQLContainer().withInitScript("pg-init.sql").withDatabaseName("foo").withUrlParam("loggerLevel", "ON").withUsername("a").withPassword("a");

    private SqlRepoManager delegate;

    @Override
    Repository createRepo() throws IOException {
        final String repoName = "test";
        delegate = new SqlRepoManager(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        final CachedRepoManager repoManager = new CachedRepoManager(delegate);
        if (repoManager.exists(repoName)) {
            repoManager.delete(repoName);
        }
        repoManager.create(repoName);
        return repoManager.open(repoName);
    }

    @AfterEach
    public void tearDown() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
