package ru.kirill.stocklist.stocklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.kirill.stocklist.stocklist.domain.Warehouse;
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

    //GET
    @GetMapping("/warehouses/new")
    public String newWarehouse(Model model){
        model.addAttribute("warehouseForm", new WarehouseForm());
        return "warehouse-form";
    }

    //создание склада POST(ловит POST запросы)
    @PostMapping("/warehouses")
    public String createWarehouse(@ModelAttribute("warehouseForm") WarehouseForm form){
        Warehouse w = new Warehouse(form.getName(), form.getAddress());
        warehouseRepository.save(w);
        return "redirect:/warehouses";
    }
}
