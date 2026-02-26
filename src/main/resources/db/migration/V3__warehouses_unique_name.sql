create unique index if not exists ux_warehouses_name_lower
on warehouses (lower(name));