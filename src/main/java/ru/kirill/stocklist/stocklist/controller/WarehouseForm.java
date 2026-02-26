package ru.kirill.stocklist.stocklist.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WarehouseForm {

    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 80, message = "Название должно быть от 2 до 80 символов")
    private String name;

    @Size(max = 200, message = "Адрес должен быть не длинее 200 символов")
    private String address;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
}
