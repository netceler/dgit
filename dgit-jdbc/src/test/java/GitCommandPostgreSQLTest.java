import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class GitCommandPostgreSQLTest extends GitCommandBase {
    @Container
    private final static JdbcDatabaseContainer DATABASE = new PostgreSQLContainer().withInitScript("pg-init.sql").withDatabaseName("foo").withUrlParam("loggerLevel", "ON");

    @Override
    JdbcDatabaseContainer container() {
        return DATABASE;
    }
}
