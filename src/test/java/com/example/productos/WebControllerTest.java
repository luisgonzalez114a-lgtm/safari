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
        usuario.setRol(rol);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SAFARI")));
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

    private Usuario usuarioAdmin() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Admin");
        usuario.setCorreo("admin@safari.com");
        usuario.setClave("admin123");
        usuario.setRol(rol);
        return usuario;
    }
}
