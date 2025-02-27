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
# JGit

JGit 6.0 and newer requires at least Java 11. Older versions require at least Java 1.8.

[https://github.com/eclipse-jgit/jgit?tab=readme-ov-file#warningscaveats](https://github.com/eclipse-jgit/jgit?tab=readme-ov-file#warningscaveats)



