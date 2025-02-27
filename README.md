# Prerequisites

[https://github.com/netceler/java-middleware](https://github.com/netceler/java-middleware)

```bash
gh repo clone netceler/java-middleware
cd java-middleware
mvn install
```

# Deploy

Some tests are broken... :-(

```bash
mvn deploy -Dmaven.test.failure.ignore=true
```

