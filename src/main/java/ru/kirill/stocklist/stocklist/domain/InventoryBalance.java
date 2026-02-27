package ru.kirill.stocklist.stocklist.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_balance",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_balance_category_product",
                        columnNames = {"category_id", "product_id"})
        })
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //прямая ссылка на склад
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int qty;


    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InventoryBalance() {
    }

    public InventoryBalance(Warehouse warehouse, Category category, Product product, int qty) {
        this.warehouse = warehouse;
        this.category = category;
        this.product = product;
        this.qty = qty;
    }


    @PrePersist
    @PreUpdate
    void touch(){
        updatedAt = LocalDateTime.now();
    }

    //Геттеры

    public Long getId() {
        return id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public Category getCategory() {
        return category;
    }

    public Product getProduct() {
        return product;
    }

    public int getQty() {
        return qty;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    //сеттеры
    public void setQty(int qty) {
        this.qty = qty;
    }
}
