create table if not exists books
(
    id           int primary key auto_increment,
    title        varchar(20),
    published_at date
);
drop table books;
select *  from  books;
create table if not exists publishers
(
    id           int primary key auto_increment,
    name        varchar(50)
);
drop table publishers;
