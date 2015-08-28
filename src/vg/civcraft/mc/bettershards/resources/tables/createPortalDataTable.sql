create table if not exists createPortalDataTable(
id varchar(255) not null,
server_name varchar(255) not null,
portal_type int not null,
partner_id varchar(255),
primary key(id));