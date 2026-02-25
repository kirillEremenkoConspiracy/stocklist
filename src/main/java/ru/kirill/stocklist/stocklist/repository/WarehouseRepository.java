package ru.kirill.stocklist.stocklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirill.stocklist.stocklist.domain.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}