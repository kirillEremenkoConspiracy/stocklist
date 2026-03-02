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
import ru.kirill.stocklist.stocklist.repository.InventoryBalanceRepository;
import ru.kirill.stocklist.stocklist.domain.InventoryBalance;
import ru.kirill.stocklist.stocklist.repository.ProductRepository;
import ru.kirill.stocklist.stocklist.repository.InventoryBalanceRepository;
import ru.kirill.stocklist.stocklist.domain.Product;


@Controller
public class WarehouseController {

    private record CategoryOption(Long id, String path) {}
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public WarehouseController(InventoryBalanceRepository inventoryBalanceRepository,
                               WarehouseRepository warehouseRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.warehouseRepository = warehouseRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
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

    private void fillCategoryModel(Long wid, Long cid, Model model, Category category){
        Warehouse warehouse =warehouseRepository.findById(wid).orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        if(!category.getWarehouse().getId().equals(wid)){
            throw new IllegalArgumentException("Category " + cid + " is not in warehouse " + wid);
        }

        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("category", category);

        model.addAttribute("categories",
                categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));

        model.addAttribute("breadcrumb", buildBreadcrumb(category));

        model.addAttribute("items", inventoryBalanceRepository.findAllByCategoryIdWithProduct(cid));

        var addCats = categoryRepository.findAllByWarehouseIdOrderByIdAsc(wid);
        var options = addCats.stream()
                .map(c -> new CategoryOption(c.getId(),
                        buildCategoryPath(c))).toList();

        model.addAttribute("categoryOptions", options);

        if(!model.containsAttribute("categoryForm")){
            model.addAttribute("categoryForm", new CategoryForm());
        }
        if(!model.containsAttribute("productCreateForm")){
            model.addAttribute("productCreateForm", new ProductCreateForm());
        }

    }


    //GET открыть папку
    @Transactional(readOnly = true)
    @GetMapping("/warehouses/{wid}/categories/{cid}")
    public String viewCategory(@PathVariable Long wid,
                               @PathVariable Long cid,
                               Model model) {
        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        fillCategoryModel(wid, cid, model, category);
        return "category-view";
    }

    private void fillCategoryPageModel(Long wid, Long cid, Model model){
        Warehouse warehouse = warehouseRepository.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        if(!category.getWarehouse().getId().equals(wid)){
            throw new IllegalArgumentException("Category " + cid + " is not in warehouse " + wid);
        }

        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("category", category);

        //подпапки
        model.addAttribute("categories",
                categoryRepository.findByWarehouseIdAndParentIdOrderByNameAsc(wid, cid));

        //формы
        if(!model.containsAttribute("categoryForm")){
            model.addAttribute("categoryForm", new CategoryForm());
        }
        if(!model.containsAttribute("productCreateForm")){
            model.addAttribute("productCreateForm", new ProductCreateForm());
        }

        //хлебные крошки
        model.addAttribute("breadcrumb", buildBreadcrumb(category));

        //товары
        model.addAttribute("items", inventoryBalanceRepository.findAllByCategoryIdWithProduct(cid));

        //заполняет выпадающий список выбора папки в окошке добавить товар
        var addCats = categoryRepository.findAllByWarehouseIdOrderByIdAsc(wid);
        var options = addCats.stream().map(c -> new CategoryOption(c.getId(), buildCategoryPath(c)))
                .toList();
        model.addAttribute("categoryOptions", options);
    }

    //POST создать подпапку внутри папки
    @Transactional
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
            model.addAttribute("categoryForm", form);
            fillCategoryModel(wid, cid, model, parent);
            return "category-view";
        }

        String name = form.getName().trim();

        //запрещаем дублирование
        if (categoryRepository.existsByWarehouseIdAndParentIdAndNameIgnoreCase(wid, cid, name)) {
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("categoryForm", form);
            fillCategoryModel(wid, cid, model, parent);
            return "category-view";
        }

        //гонка молния макуин кчау-кчау молния я молния
        //ладно оке это защита от гонки, ну я надеюсь что защитит
        try {
            categoryRepository.save(new Category(warehouse, parent, name));
        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже есть в этом разделе");
            model.addAttribute("categoryForm", form);
            fillCategoryModel(wid, cid, model, parent);
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


    @Transactional
    @PostMapping("/warehouses/{wid}/categories/{cid}/items")
    public String addItem(@PathVariable Long wid,
                          @PathVariable Long cid,
                          @Valid @ModelAttribute("productCreateForm") ProductCreateForm form,
                          BindingResult bindingResult,
                          Model model) {

        Long targetCategoryId = (form.getCategoryId() != null) ? form.getCategoryId() : cid;

        if (bindingResult.hasErrors()) {
            //сохраним форму с ошибками и откроем окошко
            model.addAttribute("productCreateForm", form);
            model.addAttribute("openProductModal", true);
            fillCategoryPageModel(wid, targetCategoryId, model);
            return "category-view";
        }

        String name = form.getName().trim();

        Product product = productRepository.findByNameIgnoreCase(name).
                orElseGet(() -> productRepository.save(new Product(name)));

        //СДЕЛАТЬ БОЛЕЕ ЧИТАЕМО В БУДУЩЕМ
        InventoryBalance balance = inventoryBalanceRepository.findByCategoryIdAndProductId(targetCategoryId, product.getId())
                .orElseGet(() -> {
                    var warehouse = warehouseRepository.findById(wid).orElseThrow();
                    var category = categoryRepository.findById(targetCategoryId).orElseThrow();
                    return new InventoryBalance(warehouse, category, product, 0);
                });

        balance.setQty(balance.getQty() + form.getQty());
        inventoryBalanceRepository.save(balance);

        return "redirect:/warehouses/" + wid + "/categories/" + targetCategoryId;
    }

    private String buildCategoryPath(Category category) {
        ArrayList<String> parts = new ArrayList<>();
        Category cur = category;

        while (cur != null) {
            parts.add(cur.getName());
            cur = cur.getParent();
        }

        Collections.reverse(parts);
        return String.join(" / ", parts);
    }
}
