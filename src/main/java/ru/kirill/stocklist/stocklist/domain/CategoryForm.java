package ru.kirill.stocklist.stocklist.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryForm {

    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 120, message = "Название должно быть от 2 до 120 символов")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
