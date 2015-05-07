create table if not exists (
uuid varchar(36) not null,
object blob not null,
entity bloc,
portal_id varchar(36),
primary key (uuid));
