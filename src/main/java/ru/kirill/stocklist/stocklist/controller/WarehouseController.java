package ru.kirill.stocklist.stocklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.kirill.stocklist.stocklist.domain.Warehouse;
import ru.kirill.stocklist.stocklist.repository.WarehouseRepository;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kirill.stocklist.stocklist.domain.CategoryRepository;


@Controller
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;

    public WarehouseController(WarehouseRepository warehouseRepository, CategoryRepository categoryRepository) {
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/warehouses")
    public String list(Model model) {
        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouses", warehouseRepository.findAll());
        return "warehouses";
    }

    @GetMapping("/warehouses/new")
    public String newWarehouse(Model model) {
        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouseForm", new WarehouseForm());
        return "warehouse-form";
    }

    @PostMapping("/warehouses")
    public String createWarehouse(
            @Valid @ModelAttribute("warehouseForm")
            WarehouseForm form,
            BindingResult bindingResult){


        if(bindingResult.hasErrors()) {
            return "warehouse-form";
        }

        Warehouse w = new Warehouse(form.getName(), form.getAddress());

        if(warehouseRepository.existsByNameIgnoreCase(form.getName())){
            bindingResult.rejectValue("name", "duplicate", "Склад с таким названием уже существует");
            return "warehouse-form";
        }
        //Подстраховка на случай гонки
        try {
            warehouseRepository.save(w);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            bindingResult.rejectValue("name", "duplicate", "Склад с таким названием уже существует");
            return "warehouse-form";
        }
        return "redirect:/warehouses";
    }

    private String blankToNull(String s){
        if (s == null){
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @GetMapping("/warehouses/{id}")
    public String viewWarehouse(@PathVariable Long id, Model model){
        Warehouse warehouse = warehouseRepository.findById(id)//УБРАТЬ ЛЯМБДУ!!!
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));

        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouse", warehouse);

        model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIsNullOrderByNameAsc(id));

        return "warehouse-view";
    }
}
