package ru.kirill.stocklist.stocklist.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductCreateForm {

    @NotBlank(message = "Название обязательно")
    private String name;

    private Long categoryId;

    @NotNull(message = "Количество обязательно")
    @Min(value = 1, message = "Количество должно быть больше 1")
    private Integer qty;

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
