package ru.kirill.stocklist.stocklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirill.stocklist.stocklist.domain.InventoryBalance;
import java.util.List;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {
    List<InventoryBalance> findAllByCategoryIdOrderByIdDesc(Long categoryId);

    Optional<InventoryBalance> findByCategoryIdAndProductId(Long categoryId, Long productId);
}
