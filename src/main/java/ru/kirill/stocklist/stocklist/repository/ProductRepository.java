package ru.kirill.stocklist.stocklist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kirill.stocklist.stocklist.domain.Product;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
