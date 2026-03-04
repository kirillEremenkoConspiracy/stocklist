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
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Transactional
    @PostMapping("/warehouses/{wid}/categories/{cid}/items/{balanceId}/delete")
    public String deleteBalance(@PathVariable Long wid,
                                @PathVariable Long cid,
                                @PathVariable Long balanceId){
        InventoryBalance b = inventoryBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new IllegalArgumentException("Balance not found: " + balanceId));

        if(!b.getWarehouse().getId().equals(wid) || !b.getCategory().getId().equals(cid)){
            throw new IllegalArgumentException("Balance does not belong to this warehouse/category");
        }

        inventoryBalanceRepository.delete(b);
        return "redirect:/warehouses/" + wid + "/categories/" + cid;
    }

    @GetMapping("/warehouses/{wid}/categories/{cid}/edit")
    @Transactional(readOnly = true)
    public String editCategory(@PathVariable Long wid,
                               @PathVariable Long cid,
                               Model model){
        Warehouse warehouse = warehouseRepository.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        if (!category.getWarehouse().getId().equals(wid)) {
            throw new IllegalArgumentException("Category not in warehouse");
        }

        model.addAttribute("activePage", "warehouses");
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("category", category);

        if (!model.containsAttribute("categoryForm")) {
            CategoryForm f = new CategoryForm();
            f.setName(category.getName());
            model.addAttribute("categoryForm", f);
        }

        List<Long> subtreeIds = collectSubtreeIds(wid, cid);
        boolean hasBalances = inventoryBalanceRepository.existsByCategoryIdIn(subtreeIds);

        model.addAttribute("subtreeSize", subtreeIds.size());
        model.addAttribute("hasBalancesInSubtree", hasBalances);

        Long archiveId = findArchiveRoot(wid).map(Category::getId).orElse(null);

        var all = categoryRepository.findAllByWarehouseIdOrderByNameAsc(wid);
        var options = all.stream()
                .filter(c -> !subtreeIds.contains(c.getId()))
                .map(c -> new CategoryOption(c.getId(), buildCategoryPath(c)))
                .toList();

        model.addAttribute("moveOptions", options);
        model.addAttribute("archiveId", archiveId);

        return "category-edit";
    }

    @Transactional
    @PostMapping("/warehouses/{wid}/categories/{cid}")
    public String updateCategory(@PathVariable Long wid,
                                 @PathVariable Long cid,
                                 @Valid @ModelAttribute("categoryForm") CategoryForm form,
                                 BindingResult bindingResult,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            return editCategory(wid, cid, model);
        }

        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        if (!category.getWarehouse().getId().equals(wid)) {
            throw new IllegalArgumentException("Category not in warehouse");
        }

        category.setName(form.getName().trim());
        try {
            categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            bindingResult.rejectValue("name", "duplicate", "Такая папка уже существует в этом месте");
            return editCategory(wid, cid, model);
        }

        return "redirect:/warehouses/" + wid + "/categories/" + cid;
    }

    @Transactional
    @PostMapping("/warehouses/{wid}/categories/{cid}/delete")
    public String deleteCategory(@PathVariable Long wid,
                                 @PathVariable Long cid,
                                 @RequestParam(name = "moveTargetId", required = false) Long moveTargetId,
                                 Model model) {

        Warehouse warehouse = warehouseRepository.findById(wid)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + wid));

        Category category = categoryRepository.findById(cid)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + cid));

        if (!category.getWarehouse().getId().equals(wid)) {
            throw new IllegalArgumentException("Category not in warehouse");
        }

        List<Long> subtreeIds = collectSubtreeIds(wid, cid);
        boolean hasBalances = inventoryBalanceRepository.existsByCategoryIdIn(subtreeIds);


        if (hasBalances) {
            Category archive = ensureArchiveRoot(warehouse);
            Long targetId = (moveTargetId != null) ? moveTargetId : archive.getId();

            if (subtreeIds.contains(targetId)) {
                model.addAttribute("categoryForm", new CategoryForm());
                model.addAttribute("deleteError", "Нельзя переносить в удаляемую папку или её подпапки");
                return editCategory(wid, cid, model);
            }

            moveBalances(subtreeIds, targetId, warehouse.getId());
        }

        deleteCategoriesBottomUp(wid, subtreeIds);

        return "redirect:/warehouses/" + wid;
    }

    private Optional<Category> findArchiveRoot(Long warehouseId) {
        return categoryRepository.findByWarehouseIdAndParentIsNullAndNameIgnoreCase(warehouseId, "Архив");
    }

    private Category ensureArchiveRoot(Warehouse warehouse) {
        return findArchiveRoot(warehouse.getId())
                .orElseGet(() -> categoryRepository.save(new Category(warehouse, null, "Архив")));
    }


    private List<Long> collectSubtreeIds(Long wid, Long rootCid) {
        var all = categoryRepository.findAllByWarehouseIdOrderByIdAsc(wid);

        java.util.Map<Long, java.util.List<Category>> children = new java.util.HashMap<>();
        for (Category c : all) {
            Long pid = (c.getParent() == null) ? null : c.getParent().getId();
            children.computeIfAbsent(pid, k -> new java.util.ArrayList<>()).add(c);
        }

        java.util.List<Long> result = new java.util.ArrayList<>();
        java.util.ArrayDeque<Long> dq = new java.util.ArrayDeque<>();
        dq.add(rootCid);

        while (!dq.isEmpty()) {
            Long cur = dq.removeFirst();
            result.add(cur);
            for (Category ch : children.getOrDefault(cur, java.util.List.of())) {
                dq.addLast(ch.getId());
            }
        }
        return result;
    }

    private void moveBalances(List<Long> fromCategoryIds, Long targetCategoryId, Long warehouseId) {
        var balances = inventoryBalanceRepository.findAllByCategoryIdInWithProduct(fromCategoryIds);

        for (InventoryBalance b : balances) {
            Long productId = b.getProduct().getId();

            InventoryBalance target = inventoryBalanceRepository
                    .findByCategoryIdAndProductId(targetCategoryId, productId)
                    .orElseGet(() -> {
                        var w = warehouseRepository.findById(warehouseId).orElseThrow();
                        var targetCat = categoryRepository.findById(targetCategoryId).orElseThrow();
                        return new InventoryBalance(w, targetCat, b.getProduct(), 0);
                    });

            target.setQty(target.getQty() + b.getQty());
            inventoryBalanceRepository.save(target);

            inventoryBalanceRepository.delete(b);
        }
    }

    private void deleteCategoriesBottomUp(Long wid, List<Long> subtreeIds) {
        java.util.Map<Long, Category> map = new java.util.HashMap<>();
        for (Category c : categoryRepository.findAllByWarehouseIdOrderByIdAsc(wid)) {
            map.put(c.getId(), c);
        }

        java.util.List<Long> sorted = new java.util.ArrayList<>(subtreeIds);
        sorted.sort((a, b) -> Integer.compare(depth(map.get(b)), depth(map.get(a)))); // desc

        for (Long id : sorted) {
            categoryRepository.deleteById(id);
        }
    }

    private int depth(Category c) {
        int d = 0;
        while (c != null && c.getParent() != null) {
            d++;
            c = c.getParent();
        }
        return d;
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
