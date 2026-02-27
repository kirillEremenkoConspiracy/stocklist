create table if not exists categories (
  id bigserial primary key,
  warehouse_id bigint not null references warehouses(id) on delete cascade,
  parent_id bigint null references categories(id) on delete cascade,
  name varchar(120) not null,
  created_at timestamptz not null default now()
);

create unique index if not exists ux_categories_root_name
  on categories (warehouse_id, lower(name))
  where parent_id is null;

create unique index if not exists ux_categories_child_name
  on categories (warehouse_id, parent_id, lower(name))
  where parent_id is not null;