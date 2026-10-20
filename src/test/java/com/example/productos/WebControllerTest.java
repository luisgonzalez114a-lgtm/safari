package com.example.productos;

import com.example.productos.model.Rol;
import com.example.productos.model.Producto;
import com.example.productos.model.Usuario;
import com.example.productos.service.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@AutoConfigureMockMvc
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void loginPageShowsAdministratorDemoAccess() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin@safari.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin123")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ver productos disponibles")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/\"")));
    }

    @Test
    void homePageLoads() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Admin");
        usuario.setCorreo("admin@safari.com");
        usuario.setClave("admin123");
        usuario.setRoles(java.util.Set.of(rol));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SAFARI")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir al inventario")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-add-disclosure")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inventory-table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("width: 20%")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inventory-row-details")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Inventario")));
    }

    @Test
    void administratorCanOpenProductEditForm() throws Exception {
        Producto producto = new Producto();
        producto.setCodigo("EDIT-TEST");
        producto.setNombre("Producto para editar");
        producto.setDescripcion("Prueba de formulario");
        producto.setPrecioCompra(new java.math.BigDecimal("1.25"));
        producto.setPrecioActual(new java.math.BigDecimal("2.50"));
        producto.setStockActual(5);
        producto.setStockMinimo(1);
        producto = productoRepository.saveAndFlush(producto);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuarioAdmin());

        MvcResult result = mockMvc.perform(get("/productos/editar/{id}", producto.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("esAdmin", true))
                .andExpect(model().attributeExists("marcas", "categorias"))
                .andExpect(view().name("index"))
                .andReturn();

        Producto editedProduct = (Producto) result.getModelAndView().getModel().get("producto");
        assertEquals("Producto para editar", editedProduct.getNombre());
    }

    @Test
    void inventoryShowsStockLevelAndUnavailableState() throws Exception {
        Producto halfStock = crearProducto("STOCK-HALF", "Producto mitad", 5, 10);
        Producto emptyStock = crearProducto("STOCK-EMPTY", "Producto agotado", 0, 10);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("stock-level")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--stock-hue: 60.0deg")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No disponible")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("stock-unavailable")));

        productoRepository.deleteAll(java.util.List.of(halfStock, emptyStock));
    }

    private Producto crearProducto(String codigo, String nombre, int stock, int stockMinimo) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecioCompra(java.math.BigDecimal.ONE);
        producto.setPrecioActual(java.math.BigDecimal.ONE);
        producto.setStockActual(stock);
        producto.setStockMinimo(stockMinimo);
        return productoRepository.saveAndFlush(producto);
    }

    private Usuario usuarioAdmin() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Admin");
        usuario.setCorreo("admin@safari.com");
        usuario.setClave("admin123");
        usuario.setRoles(java.util.Set.of(rol));
        return usuario;
    }
}
