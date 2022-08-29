create table if not exists book
(
    id           int primary key auto_increment,
    title        varchar(20),
    published_at date
);
drop table book;
select *  from  book;
create table if not exists publishers
(
    id           int primary key auto_increment,
    name        varchar(50)
);
drop table publishers;
