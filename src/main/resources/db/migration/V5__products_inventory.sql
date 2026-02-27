create table if not exists products (
    id          bigserial primary key,
    name        text not null,
    photo_key   text null,                 -- пока заглушка
    created_at  timestamptz not null default now()
);

create index if not exists ix_products_name_lower
    on products (lower(name));


create table if not exists inventory_balance (
    id           bigserial primary key,
    warehouse_id bigint not null references warehouses(id) on delete cascade,
    category_id  bigint not null references categories(id)  on delete cascade,
    product_id   bigint not null references products(id)    on delete restrict,
    qty          integer not null default 0 check (qty >= 0),
    updated_at   timestamptz not null default now()
);

create unique index if not exists ux_balance_category_product
    on inventory_balance (category_id, product_id);

create index if not exists ix_balance_warehouse
    on inventory_balance (warehouse_id);

create index if not exists ix_balance_product
    on inventory_balance (product_id);



create table if not exists inventory_movement (
    id           bigserial primary key,
    warehouse_id bigint not null references warehouses(id) on delete cascade,
    type         varchar(16) not null,
    note         text null,
    created_at   timestamptz not null default now()
);

create index if not exists ix_movement_warehouse_created
    on inventory_movement (warehouse_id, created_at desc);

create index if not exists ix_movement_type
    on inventory_movement (type);


create table if not exists inventory_movement_line (
    id               bigserial primary key,
    movement_id      bigint not null references inventory_movement(id) on delete cascade,
    product_id       bigint not null references products(id) on delete restrict,
    qty              integer not null check (qty > 0),
    from_category_id bigint null references categories(id) on delete restrict,
    to_category_id   bigint null references categories(id) on delete restrict
);

create index if not exists ix_move_line_movement
    on inventory_movement_line (movement_id);

create index if not exists ix_move_line_product
    on inventory_movement_line (product_id);

create index if not exists ix_move_line_from_cat
    on inventory_movement_line (from_category_id);

create index if not exists ix_move_line_to_cat
    on inventory_movement_line (to_category_id);


alter table inventory_movement
    add constraint chk_movement_type
    check (type in ('IN','OUT','MOVE','ADJUST'));


alter table inventory_movement_line
    add constraint chk_move_line_has_side
    check (from_category_id is not null or to_category_id is not null);

