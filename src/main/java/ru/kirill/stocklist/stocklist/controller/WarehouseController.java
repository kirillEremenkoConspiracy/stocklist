package ru.kirill.stocklist.stocklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.kirill.stocklist.stocklist.repository.WarehouseRepository;

@Controller
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseController(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping("/warehouses")
    public String list(Model model) {
        model.addAttribute("warehouses", warehouseRepository.findAll());
        return "warehouses";
    }
}
