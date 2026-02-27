package ru.kirill.stocklist.stocklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.kirill.stocklist.stocklist.domain.Category;
import ru.kirill.stocklist.stocklist.domain.CategoryForm;
import ru.kirill.stocklist.stocklist.domain.Warehouse;
import ru.kirill.stocklist.stocklist.repository.WarehouseRepository;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kirill.stocklist.stocklist.domain.CategoryRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIsNullOrderByNameAsc(id));

        return "warehouse-view";
    }

    @PostMapping("/warehouses/{id}/categories")
    public String createRootCategory(@PathVariable Long id, @Valid @ModelAttribute("categoryForm")CategoryForm form,
                                     BindingResult bindingResult, Model model){
        Warehouse warehouse = warehouseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));

        //Остаемся на странице если валидация не прошла
        if(bindingResult.hasErrors()){
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIsNullOrderByNameAsc(id));

            return "warehouse-view";
        }

        String name = form.getName().trim();

        //проверка в корне
        if(categoryRepository.existsByWarehouseIdAndParentIsNullAndNameIgnoreCase(id, name)){
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIsNullOrderByNameAsc(id));

            return "warehouse-view";
        }

        try{
            categoryRepository.save(new Category(warehouse, null, name));
        } catch (DataIntegrityViolationException e){
            //от гонки
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIsNullOrderByNameAsc(id));

            return "warehouse-view";
        }

        return "redirect:/warehouses/" + id;
    }

    //GET открыть папку
    @Transactional(readOnly = true)
    @GetMapping("/warehouses/{wid}/categories/{cid}")
    public String viewCategory(@PathVariable Long wid,
                               @PathVariable Long cid,
                               Model model) {
        Warehouse warehouse = warehouseRepository.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        //папка должна принадлежать этому складу
        if(!category.getWarehouse().getId().equals(wid)){
            throw new IllegalArgumentException("Category " + cid + " is not in warehouse " + wid);
        }

        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("category", category);

        //показать подпапки в папке
        model.addAttribute("categories", categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));

        model.addAttribute("categoryForm", new CategoryForm());

        model.addAttribute("breadcrumb", buildBreadcrumb(category));
        return "category-view";
    }

    //POST создать подпапку внутри папки
    @Transactional(readOnly = true)
    @PostMapping("/warehouses/{wid}/categories/{cid}/categories")
    public String createSubCategory(@PathVariable Long wid,
                                    @PathVariable Long cid,
                                    @Valid @ModelAttribute("categoryForm") CategoryForm form,
                                    BindingResult bindingResult,
                                    Model model){
        Warehouse warehouse = warehouseRepository.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        Category parent = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        if (!parent.getWarehouse().getId().equals(wid)) {
            throw new IllegalArgumentException("Category " + cid + " is not in warehouse " + wid);
        }

        //Проверка валидации и возврат страницы с ошибкой
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("category", parent);
            model.addAttribute("categories",
                    categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));
            model.addAttribute("breadcrumb", buildBreadcrumb(parent));
            return "category-view";
        }

        String name = form.getName().trim();

        //запрещаем дублирование
        if (categoryRepository.existsByWarehouseIdAndParentIdAndNameIgnoreCase(wid, cid, name)) {
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("category", parent);
            model.addAttribute("categories",
                    categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));
            model.addAttribute("breadcrumb", buildBreadcrumb(parent));
            return "category-view";
        }

        //гонка молния макуин кчау-кчау молния я молния
        //ладно оке это защита от гонки, ну я надеюсь что защитит
        try {
            categoryRepository.save(new Category(warehouse, parent, name));
        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("activePage", "warehouses");
            model.addAttribute("warehouse", warehouse);
            model.addAttribute("category", parent);
            model.addAttribute("categories",
                    categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));
            model.addAttribute("breadcrumb", buildBreadcrumb(parent));
            return "category-view";
        }

        return "redirect:/warehouses/" + wid + "/categories/" + cid;
    }

    private List<Category> buildBreadcrumb(Category current){
        List<Category> path = new ArrayList<>();
        Category c = current;
        while (c != null) {
            path.add(c);
            c = c.getParent();
        }
        Collections.reverse(path);
        return path;
    }
}
