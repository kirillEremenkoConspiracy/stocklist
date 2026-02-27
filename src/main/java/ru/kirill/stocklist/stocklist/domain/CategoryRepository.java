package ru.kirill.stocklist.stocklist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByWarehouseIdAndParentIsNullOrderByNameAsc(Long warehouseId);


    List<Category> findByWarehouseIdAndParentIdOrderByNameAsc(Long warehouseId, Long parentId);

    boolean existsByWarehouseIdAndParentIsNullAndNameIgnoreCase(Long warehouseId, String name);

    boolean existsByWarehouseIdAndParentIdAndNameIgnoreCase(Long warehouseId, Long parentId, String name);
}
