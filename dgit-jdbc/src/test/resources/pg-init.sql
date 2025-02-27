create table "GIT_REFS"
(
    "REPO"      varchar(255),
    "NAME"      varchar(255),
    "SYMBOLIC"  smallint,
    "TARGET"    varchar(255),
    "STORAGE"   varchar(16),
    "OBJECT_ID" char(40),
    primary key ("REPO", "NAME")
);

create table "GIT_OBJECTS"
(
    "REPO"       varchar(255),
    "OBJECT_ID"  char(40),
    "TYPE"       integer,
    "DATA"       bytea,
    "BASE"       char(40),
    "SIZE"       bigint,
    "TOTAL_SIZE" bigint,
    primary key ("REPO", "OBJECT_ID")
);

create table "GIT_CONFIG"
(
    "REPO"   varchar(255),
    "CONFIG" text,
    primary key ("REPO")
);

