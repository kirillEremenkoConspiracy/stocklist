package ru.kirill.stocklist.stocklist.domain;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, length = 120)
    private String name;

    protected Category(){

    }

    public Category(Warehouse warehouse, Category parent, String name){
        this.warehouse = warehouse;
        this.parent = parent;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Category getParent() {
        return parent;
    }

    public Long getId() {
        return id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setName(String name) {
        this.name = name;
    }
}
