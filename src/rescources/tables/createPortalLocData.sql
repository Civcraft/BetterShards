create table if not exists createPortalLocData(
x int not null,
y int not null,
z int not null,
world varchar(255) not null,
id varchar(255) not null,
primary key loc_id (x, y, z, world, id));