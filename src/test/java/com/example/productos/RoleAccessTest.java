package com.example.productos;

import com.example.productos.controller.ProductoWebController;
import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class RoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoWebController productoWebController;

    @Test
    void guestCanBrowsePublicCatalog() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Catálogo de consulta")));
    }

    @Test
    void regularUserCannotAccessProductEdition() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("USER");

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Usuario normal");
        usuario.setCorreo("usuario@safari.com");
        usuario.setClave("user123");
        usuario.setRol(rol);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/productos/editar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void regularUserCannotOpenSalesScreen() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("USER");
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Usuario normal");
        usuario.setRol(rol);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/salidas").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void publicCatalogIsVisibleWithoutSessionButPrivateMovementApiIsNot() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/entradas")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/proveedores")).andExpect(status().isForbidden());
    }

    @Test
    void sellerCanOpenSalesAndGoodsReceiptScreens() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("VENDEDOR");
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Vendedor");
        usuario.setRol(rol);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/salidas").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/entradas").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/entradas").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/proveedores").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminCanReadSuppliersApi() throws Exception {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Administrador");
        usuario.setRol(rol);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);

        mockMvc.perform(get("/api/proveedores").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAdminRoleHasEditAccess() {
        Rol rol = new Rol();
        rol.setNombre("EDITOR");

        Usuario usuario = new Usuario();
        usuario.setRol(rol);

        assertFalse(productoWebController.usuarioPuedeEditar(usuario));
    }

    @Test
    void adminRoleCanEdit() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setRol(rol);

        assertTrue(productoWebController.usuarioPuedeEditar(usuario));
    }
}
