create table if not exists createPlayerData(
uuid varchar(36) not null,
object blob not null,
entity blob,
portal_id varchar(36),
primary key (uuid));
