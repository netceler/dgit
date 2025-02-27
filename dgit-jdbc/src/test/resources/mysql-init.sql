CREATE table GIT_REFS
(
    REPO      varchar(255),
    NAME      varchar(255),
    SYMBOLIC  smallint,
    TARGET    varchar(255),
    STORAGE   varchar(16),
    OBJECT_ID char(40),
    primary key (REPO, NAME)
);

CREATE table GIT_OBJECTS
(
    REPO       varchar(255),
    OBJECT_ID  char(40),
    TYPE       int,
    DATA       BLOB(2147483647),
    BASE       char(40),
    SIZE       bigint,
    TOTAL_SIZE bigint,
    primary key (REPO, OBJECT_ID)
);

CREATE table GIT_CONFIG
(
    REPO   varchar(255),
    CONFIG text,
    primary key (REPO)
);

