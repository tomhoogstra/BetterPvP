create table if not exists ${tablePrefix}clans
(
    id        int         auto_increment,
    Name      varchar(32) not null,
    Created   TIMESTAMP   null default CURRENT_TIMESTAMP,
    Home      varchar(64) null,
    Admin     tinyint     null default 0,
    Safe      tinyint     null default 0,
    Energy    int         null default 0,
    Points    int         null default 0,
    Cooldown  bigint      null default 0,
    Level     int         null default 1,
    LastLogin TIMESTAMP   null default CURRENT_TIMESTAMP,
    constraint clans_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_Name_uindex
    on ${tablePrefix}clans (Name);

create table if not exists ${tablePrefix}clan_territory
(
    id        int         auto_increment not null,
    Clan      int         not null,
    Chunk     varchar(64) not null,
    constraint clan_territory_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_territory_Clan_Chunk_uindex
    on ${tablePrefix}clan_territory (Clan, Chunk);

create table if not exists ${tablePrefix}clan_members
(
    id        int         auto_increment not null,
    Clan      int         not null,
    Member    varchar(64) not null,
    `Rank`    varchar(64) not null default 'RECRUIT',
    constraint clan_members_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_members_Clan_Member_uindex
    on ${tablePrefix}clan_members (Clan, Member);

create table if not exists ${tablePrefix}clan_alliances
(
    id        int         auto_increment not null,
    Clan      int         not null,
    AllyClan  int         not null,
    Trusted   tinyint     default 0,
    constraint clan_alliances_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_alliances_Clan_AllyClan_uindex
    on ${tablePrefix}clan_alliances (Clan, AllyClan);

create table if not exists ${tablePrefix}clan_enemies
(
    id        int         auto_increment not null,
    Clan      int         not null,
    EnemyClan  int        not null,
    Dominance  tinyint    default 0,
    constraint clan_enemies_pk
        primary key (id)
);

create unique index ${tablePrefix}clans_enemies_Clan_EnemyClan_uindex
    on ${tablePrefix}clan_enemies (Clan, EnemyClan);


create table if not exists ${tablePrefix}dominance_scale
(
    ClanSize  int not null,
    Dominance int not null,
    constraint ${tablePrefix}dominance_scale_pk
        primary key (ClanSize)
);

INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (1, 4);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (2, 4);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (3, 5);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (4, 5);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (5, 6);
INSERT IGNORE INTO ${tablePrefix}dominance_scale VALUES (6, 6);