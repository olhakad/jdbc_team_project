create table if not exists books
(
    id           bigint auto_increment,
    title        varchar(20),
    published_at date,
    publisher_id bigint,
    primary key(id),
    foreign key (publisher_id) references publishers(id)
);

create table if not exists publishers
(
    id           bigint auto_increment,
    name        varchar(50),
    primary key(id)
);
drop table publishers;
