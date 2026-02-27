package ru.kirill.stocklist.stocklist.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // заглушка под фото
    @Column(name = "photoKey")
    private String photoKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Product(String name){
        this.name = name;
    }

    protected Product(String name, String photoKey){
        this.name = name;
        this.photoKey = photoKey;
    }

    @PrePersist
    void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
    }

    //геттеры

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPhotoKey() {
        return photoKey;
    }

    //сеттеры

    public void setName(String name) {
        this.name = name;
    }

    public void setPhotoKey(String photoKey) {
        this.photoKey = photoKey;
    }
}
