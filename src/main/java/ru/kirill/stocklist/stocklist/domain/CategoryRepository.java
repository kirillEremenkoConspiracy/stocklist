package ru.kirill.stocklist.stocklist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByWarehouseIdAndParentIsNullOrderByNameAsc(Long warehouseId);


    List<Category> findByWarehouseIdAndParentIdOrderByNameAsc(Long warehouseId, Long parentId);

    boolean existsByWarehouseIdAndParentIsNullAndNameIgnoreCase(Long warehouseId, String name);

    boolean existsByWarehouseIdAndParentIdAndNameIgnoreCase(Long warehouseId, Long parentId, String name);

    List<Category> findAllByWarehouseIdOrderByIdAsc(Long warehouseId);

    List<Category> findAllByWarehouseIdOrderByNameAsc(Long warehouseId);

    boolean existsByWarehouseId(Long warehouseId);

    Optional<Category> findByWarehouseIdAndParentIsNullAndNameIgnoreCase(Long warehouseId, String name);

    boolean existsByParentId(Long parentId);

}
