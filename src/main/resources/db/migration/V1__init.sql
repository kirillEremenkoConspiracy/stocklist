create table if not exists app_info (
    id bigserial primary key,
    name text not null,
    created_at timestamp not null default now()
);

insert into app_info(name) values ('stocklist');