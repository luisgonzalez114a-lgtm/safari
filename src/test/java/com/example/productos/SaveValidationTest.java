package com.example.productos;

import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaveValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidProductRequestShouldRedirectInsteadOfCrashing() throws Exception {
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

        mockMvc.perform(post("/productos/guardar")
                        .session(session)
                        .param("codigo", "")
                        .param("nombre", "")
                        .param("descripcion", "")
                        .param("idMarca", "")
                        .param("idCategoria", "")
                        .param("precioCompra", "")
                        .param("precioActual", "")
                        .param("stockActual", "")
                        .param("stockMinimo", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void invalidUserRequestShouldRedirectInsteadOfCrashing() throws Exception {
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

        mockMvc.perform(post("/usuarios/guardar")
                        .session(session)
                        .param("nombre", "")
                        .param("correo", "")
                        .param("clave", "")
                        .param("rolesIds", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
    }
}
