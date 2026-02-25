create table warehouses (
    id bigserial primary key,
    name text not null,
    address text,
    created_at timestamp not null default now()
);

create unique index ux_warehouses_name on warehouses (name);