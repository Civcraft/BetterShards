create table if not exists createEntityData(
id int not null auto_increment,
object blob not null,
portal_id varchar(36),
primary key (id));