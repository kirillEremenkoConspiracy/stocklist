package ru.kirill.stocklist.stocklist.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String address;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    //protected чтобы случайно не создавали пустые склады в коде, но Hibernate мог
    protected Warehouse() { } // Hibernate создаёт объект рефлексией ему нужен конструктор без аргументов

    //для создания объекта в коде
    public Warehouse(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
}
