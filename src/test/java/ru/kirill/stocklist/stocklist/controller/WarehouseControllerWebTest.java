package ru.kirill.stocklist.stocklist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.kirill.stocklist.stocklist.domain.CategoryRepository;
import ru.kirill.stocklist.stocklist.domain.Warehouse;
import ru.kirill.stocklist.stocklist.repository.InventoryBalanceRepository;
import ru.kirill.stocklist.stocklist.repository.ProductRepository;
import ru.kirill.stocklist.stocklist.repository.WarehouseRepository;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WarehouseController.class)
public class WarehouseControllerWebTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    WarehouseRepository warehouseRepository;

    @MockitoBean
    InventoryBalanceRepository inventoryBalanceRepository;

    @MockitoBean
    CategoryRepository categoryRepository;

    @MockitoBean
    ProductRepository productRepository;

    @MockitoBean
    Warehouse warehouse;


    @Test
    void createWarehouse_success_redirects() throws Exception {
        when(warehouseRepository.existsByNameIgnoreCase("QA Warehouse X")).thenReturn(false);

        mvc.perform(post("/warehouses")
                .param("name", "QA Warehouse X")
                .param("address", "Test street"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/warehouses"));

    }

    @Test
    void createWarehouse_duplicate_showsErrorOnForm() throws Exception{
        when(warehouseRepository.existsByNameIgnoreCase("QA Warehouse X"))
                .thenReturn(true);

        mvc.perform(post("/warehouses")
                .param("name", "QA Warehouse X")
                .param("address", "Test street"))
                .andExpect(status().isOk())
                .andExpect(view().name("warehouse-form"))
                .andExpect(model().attributeHasFieldErrors("warehouseForm", "name"))
                .andDo(print());
    }

    @Test
    void createWarehouse_blankName_showsValidationError() throws Exception{
        mvc.perform(post("/warehouses")
                .param("name", "")
                .param("address", "Test"))
                .andExpect(status().isOk())
                .andExpect(view().name("warehouse-form"))
                .andExpect(model().attributeHasFieldErrors("warehouseForm", "name"))
                .andDo(print());
    }

    @Test
    void createRootCategory_success_redirectsBackToWarehouse() throws Exception{
        long wid = 13L;

        var wh = new Warehouse("Any name", "Any address");
        when(warehouseRepository.findById(wid)).thenReturn(Optional.of(wh));

        when(categoryRepository.existsByWarehouseIdAndParentIdAndNameIgnoreCase(wid, null, "Root QA"))
                .thenReturn(false);

        mvc.perform(post("/warehouses/{wid}/categories", wid)
                .param("name", "Root QA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/warehouses/" + wid));
    }


    //Тест на проверку дубликата папки в одном складе
    @Test
    void createRootCategory_duplicate_showsErrorAndStaysOnPage() throws Exception {
        long wid = 13L;
        var wh = new Warehouse("Any name", "Any address");

        when(warehouseRepository.findById(wid)).thenReturn(Optional.of(wh));

        when(categoryRepository.existsByWarehouseIdAndParentIsNullAndNameIgnoreCase(wid, "Root QA"))
                .thenReturn(true);

        mvc.perform(post("/warehouses/{wid}/categories", wid)
                        .param("name", "Root QA"))
                .andExpect(status().isOk())
                .andExpect(view().name("warehouse-view"))
                .andExpect(model().attributeHasFieldErrors("categoryForm", "name"));;
    }
}
