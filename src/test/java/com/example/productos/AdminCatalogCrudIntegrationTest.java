package com.example.productos;

import com.example.productos.model.Usuario;
import com.example.productos.model.Entrada;
import com.example.productos.model.Producto;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCatalogCrudIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private EntradaRepository entradaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void administratorCanCreateReadUpdateAndDeleteProductsAndCatalogs() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        MvcResult createdCategory = mockMvc.perform(post("/api/categorias")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Cat " + suffix + "\",\"descripcion\":\"Inicial\",\"estatus\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        int categoryId = objectMapper.readTree(createdCategory.getResponse().getContentAsString())
                .get("id").asInt();
        mockMvc.perform(put("/api/categorias/{id}", categoryId)
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Cat Updated " + suffix + "\",\"descripcion\":\"Editada\",\"estatus\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Safari Cat Updated " + suffix)));
        mockMvc.perform(get("/api/categorias").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Safari Cat Updated " + suffix)));

        mockMvc.perform(post("/api/marcas")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Brand " + suffix + "\",\"estatus\":1}"))
                .andExpect(status().isOk());
        var brand = marcaRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Safari Brand " + suffix))
                .findFirst().orElseThrow();
        mockMvc.perform(put("/api/marcas/{id}", brand.getId())
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Brand Updated " + suffix + "\",\"estatus\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Safari Brand Updated " + suffix)));

        mockMvc.perform(post("/api/proveedores")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Supplier " + suffix + "\",\"rifNit\":\"NIT-" + suffix + "\",\"telefono\":\"5551234\",\"correo\":\"supplier-" + suffix + "@safari.com\",\"direccion\":\"Local\"}"))
                .andExpect(status().isOk());
        var supplier = proveedorRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Safari Supplier " + suffix))
                .findFirst().orElseThrow();
        mockMvc.perform(put("/api/proveedores/{id}", supplier.getId())
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"Safari Supplier Updated " + suffix + "\",\"rifNit\":\"NIT-EDIT-" + suffix + "\",\"telefono\":\"5555678\",\"correo\":\"updated-" + suffix + "@safari.com\",\"direccion\":\"Nueva dirección\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Safari Supplier Updated " + suffix)));
        mockMvc.perform(get("/api/proveedores").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Safari Supplier Updated " + suffix)));

        int updatedCategoryId = categoriaRepository.findAll().stream()
                .filter(category -> category.getNombre().equals("Safari Cat Updated " + suffix))
                .findFirst().orElseThrow().getId();
        int updatedBrandId = marcaRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Safari Brand Updated " + suffix))
                .findFirst().orElseThrow().getId();
        String code = "SAF-" + suffix;
        MvcResult createdProduct = mockMvc.perform(post("/api/productos")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content(productJson(code, "Producto Safari " + suffix, updatedBrandId, updatedCategoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        int productId = objectMapper.readTree(createdProduct.getResponse().getContentAsString())
                .get("id").asInt();

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(code)));
        mockMvc.perform(put("/api/productos/{id}", productId)
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content(productJson(code, "Producto Safari Editado " + suffix, updatedBrandId, updatedCategoryId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Producto Safari Editado " + suffix)));
        assertEquals("Producto Safari Editado " + suffix,
                productoRepository.findById(productId).orElseThrow().getNombre());

        mockMvc.perform(delete("/api/productos/{id}", productId).session(adminSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/proveedores/{id}", supplier.getId()).session(adminSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/marcas/{id}", brand.getId()).session(adminSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/categorias/{id}", updatedCategoryId).session(adminSession))
                .andExpect(status().isNoContent());

        assertFalse(productoRepository.existsById(productId));
        assertFalse(proveedorRepository.existsById(supplier.getId()));
        assertFalse(marcaRepository.existsById(brand.getId()));
        assertFalse(categoriaRepository.existsById(updatedCategoryId));
    }

    @Test
    void anonymousAndNonAdminUsersCannotWriteAdminCatalogs() throws Exception {
        Usuario regularUser = usuarioRepository.findByCorreo("usuario@safari.com").orElseThrow();
        MockHttpSession userSession = new MockHttpSession();
        userSession.setAttribute("usuarioLogueado", regularUser);

        mockMvc.perform(post("/api/categorias")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/catalogos/marcas/eliminar")
                        .session(userSession)
                        .param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/api/marcas")
                        .session(userSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/proveedores")
                        .session(userSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanAddCatalogItemsFromTheWebForms() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String categoryName = "Web Category " + suffix;
        String brandName = "Web Brand " + suffix;
        String supplierName = "Web Supplier " + suffix;

        mockMvc.perform(post("/catalogos/categorias")
                        .session(adminSession)
                        .param("nombre", categoryName)
                        .param("descripcion", "Creada desde SAFARI"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogos/categorias"));
        mockMvc.perform(post("/catalogos/marcas")
                        .session(adminSession)
                        .param("nombre", brandName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogos/marcas"));
        mockMvc.perform(post("/catalogos/proveedores")
                        .session(adminSession)
                        .param("nombre", supplierName)
                        .param("rifNit", "WEB-" + suffix))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogos/proveedores"));

        mockMvc.perform(get("/catalogos").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogos/proveedores"));
        mockMvc.perform(get("/catalogos/proveedores").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("suppliers-table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir proveedor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar proveedor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar proveedor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(supplierName)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("brands-table"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("categories-table"))));
        mockMvc.perform(get("/catalogos/marcas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("brands-table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir marca")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(brandName)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("suppliers-table"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("categories-table"))));
        mockMvc.perform(get("/catalogos/categorias").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("categories-table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir categoría")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Editar categoría")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Eliminar categoría")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(categoryName)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("suppliers-table"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("brands-table"))));
        assertTrue(categoriaRepository.findAll().stream().anyMatch(value -> value.getNombre().equals(categoryName)));
        assertTrue(marcaRepository.findAll().stream().anyMatch(value -> value.getNombre().equals(brandName)));
        assertTrue(proveedorRepository.findAll().stream().anyMatch(value -> value.getNombre().equals(supplierName)));
    }

    @Test
    void administratorCanEditAndDeleteCatalogItemsFromTheWebPage() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/catalogos/categorias")
                        .session(adminSession)
                        .param("nombre", "Category " + suffix)
                        .param("descripcion", "Before"))
                .andExpect(status().is3xxRedirection());
        var category = categoriaRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Category " + suffix))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/catalogos/categorias/actualizar")
                        .session(adminSession)
                        .param("id", category.getId().toString())
                        .param("nombre", "Category edited " + suffix)
                        .param("descripcion", "After"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogos/categorias"));
        assertEquals("After", categoriaRepository.findById(category.getId()).orElseThrow().getDescripcion());

        mockMvc.perform(post("/catalogos/marcas")
                        .session(adminSession)
                        .param("nombre", "Brand " + suffix))
                .andExpect(status().is3xxRedirection());
        var brand = marcaRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Brand " + suffix))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/catalogos/marcas/actualizar")
                        .session(adminSession)
                        .param("id", brand.getId().toString())
                        .param("nombre", "Brand edited " + suffix))
                .andExpect(status().is3xxRedirection());
        assertEquals("Brand edited " + suffix, marcaRepository.findById(brand.getId()).orElseThrow().getNombre());

        mockMvc.perform(post("/catalogos/proveedores")
                        .session(adminSession)
                        .param("nombre", "Supplier " + suffix)
                        .param("rifNit", "BEFORE"))
                .andExpect(status().is3xxRedirection());
        var supplier = proveedorRepository.findAll().stream()
                .filter(value -> value.getNombre().equals("Supplier " + suffix))
                .findFirst().orElseThrow();
        mockMvc.perform(post("/catalogos/proveedores/actualizar")
                        .session(adminSession)
                        .param("id", supplier.getId().toString())
                        .param("nombre", "Supplier edited " + suffix)
                        .param("rifNit", "AFTER")
                        .param("telefono", "5551234")
                        .param("correo", "supplier-" + suffix + "@safari.com")
                        .param("direccion", "Nueva dirección"))
                .andExpect(status().is3xxRedirection());
        assertEquals("AFTER", proveedorRepository.findById(supplier.getId()).orElseThrow().getRifNit());

        mockMvc.perform(post("/catalogos/categorias/eliminar").session(adminSession)
                        .param("id", category.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/catalogos/marcas/eliminar").session(adminSession)
                        .param("id", brand.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/catalogos/proveedores/eliminar").session(adminSession)
                        .param("id", supplier.getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertFalse(categoriaRepository.existsById(category.getId()));
        assertFalse(marcaRepository.existsById(brand.getId()));
        assertFalse(proveedorRepository.existsById(supplier.getId()));
    }

    @Test
    void catalogItemsInUseCannotBeDeleted() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        var category = new com.example.productos.model.Categoria();
        category.setNombre("Used category " + suffix);
        category = categoriaRepository.save(category);

        var brand = new com.example.productos.model.Marca();
        brand.setNombre("Used brand " + suffix);
        brand = marcaRepository.save(brand);

        var supplier = new com.example.productos.model.Proveedor();
        supplier.setNombre("Used supplier " + suffix);
        supplier = proveedorRepository.save(supplier);

        Producto product = new Producto();
        product.setCodigo("USED-" + suffix);
        product.setNombre("Product using catalog");
        product.setPrecioCompra(BigDecimal.ONE);
        product.setPrecioActual(BigDecimal.ONE);
        product.setStockActual(0);
        product.setStockMinimo(0);
        product.setIdCategoria(category.getId());
        product.setIdMarca(brand.getId());
        productoRepository.save(product);

        Entrada entry = new Entrada();
        entry.setProveedor(supplier);
        entry.setUsuario(usuarioRepository.findByCorreo("admin@safari.com").orElseThrow());
        entradaRepository.save(entry);

        mockMvc.perform(post("/catalogos/categorias/eliminar").session(adminSession)
                        .param("id", category.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", "No se puede eliminar la categoría porque está asignada a productos."));
        mockMvc.perform(post("/catalogos/marcas/eliminar").session(adminSession)
                        .param("id", brand.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", "No se puede eliminar la marca porque está asignada a productos."));
        mockMvc.perform(post("/catalogos/proveedores/eliminar").session(adminSession)
                        .param("id", supplier.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("error", "No se puede eliminar el proveedor porque tiene entradas registradas."));

        assertTrue(categoriaRepository.existsById(category.getId()));
        assertTrue(marcaRepository.existsById(brand.getId()));
        assertTrue(proveedorRepository.existsById(supplier.getId()));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado",
                usuarioRepository.findByCorreo("admin@safari.com").orElseThrow());
        return session;
    }

    private String productJson(String code, String name, int brandId, int categoryId) {
        return "{\"codigo\":\"" + code + "\",\"nombre\":\"" + name
                + "\",\"descripcion\":\"Prueba SAFARI\",\"idMarca\":" + brandId
                + ",\"idCategoria\":" + categoryId
                + ",\"precioCompra\":5.25,\"precioActual\":8.50,\"stockActual\":7,\"stockMinimo\":1}";
    }

}
