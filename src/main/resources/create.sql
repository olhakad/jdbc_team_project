create table if not exists book
(
    id           int ,
    title        varchar(20),
    publishedAt date
);
drop table book;
select *  from  book;
create table if not exists publisher
(
    id           int,
    name        varchar(50)
);