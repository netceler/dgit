import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GitCommandMySQLTest extends GitCommandBase {
    private static final Logger log = LoggerFactory.getLogger(GitCommandMySQLTest.class);

    @Container
    private static final JdbcDatabaseContainer DATABASE = new MySQLContainer().withInitScript("mysql-init.sql").withDatabaseName("foo");

    @Override
    JdbcDatabaseContainer container() {
        return DATABASE;
    }

}
