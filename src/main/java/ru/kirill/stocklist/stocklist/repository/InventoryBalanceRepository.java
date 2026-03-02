package ru.kirill.stocklist.stocklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirill.stocklist.stocklist.domain.InventoryBalance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    List<InventoryBalance> findAllByCategoryIdOrderByIdDesc(Long categoryId);

    Optional<InventoryBalance> findByCategoryIdAndProductId(Long categoryId, Long productId);

    @Query("""
       select b
       from InventoryBalance b
       join fetch b.product
       where b.category.id = :categoryId
       order by b.id desc
       """)
    List<InventoryBalance> findAllByCategoryIdWithProduct(@Param("categoryId") Long categoryId);
}
