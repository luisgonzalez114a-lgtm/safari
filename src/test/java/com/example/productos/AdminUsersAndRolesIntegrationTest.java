package com.example.productos;

import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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
class AdminUsersAndRolesIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void administratorCanBrowseUsersGridAcrossPages() throws Exception {
        MockHttpSession adminSession = adminSession();
        Usuario admin = usuarioRepository.findByCorreo("admin@safari.com").orElseThrow();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        for (int i = 0; i < 10; i++) {
            Usuario user = new Usuario();
            user.setNombre("ZZZ Page User " + i);
            user.setCorreo("page-" + suffix + "-" + i + "@example.com");
            user.setClave(passwordEncoder.encode("page-pass"));
            user.setEstatus(1);
            user.setRol(admin.getRol());
            usuarioRepository.save(user);
        }
        long totalPages = (usuarioRepository.count() + 8) / 9;

        mockMvc.perform(get("/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("users-table")))
                .andExpect(content().string(containsString("user-row-details")))
                .andExpect(content().string(containsString("user-expanded-grid")))
                .andExpect(content().string(containsString("id=\"crearUsuarioModal\"")))
                .andExpect(content().string(containsString("id=\"asignarRolModal\"")))
                .andExpect(content().string(containsString("href=\"/usuarios?page=1\"")))
                .andExpect(content().string(containsString("Página 1 de")));

        mockMvc.perform(get("/usuarios").param("page", "1").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página 2 de")))
                .andExpect(content().string(containsString("href=\"/usuarios?page=0\"")))
                .andExpect(content().string(containsString("ZZZ Page User")));

        mockMvc.perform(get("/usuarios").param("page", "999").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página " + totalPages + " de " + totalPages)));
    }

    @Test
    void administratorCanCreateEditViewAndDeleteUsersAndRoles() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String roleName = "SAFARI_TEST_" + suffix;

        mockMvc.perform(get("/usuarios").session(adminSession)).andExpect(status().isOk());
        mockMvc.perform(get("/roles").session(adminSession)).andExpect(status().isOk());

        MvcResult createdRole = mockMvc.perform(post("/api/roles")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"" + roleName + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        int roleId = objectMapper.readTree(createdRole.getResponse().getContentAsString())
                .get("id").asInt();

        mockMvc.perform(put("/api/roles/{id}", roleId)
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"" + roleName + "_EDITED\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(roleName + "_EDITED")));

        String email = "safari-" + suffix + "@example.com";
        mockMvc.perform(post("/usuarios/guardar")
                        .session(adminSession)
                        .param("nombre", "Usuario SAFARI " + suffix)
                        .param("correo", email)
                        .param("clave", "initial-pass")
                        .param("telefono", "555-0100")
                        .param("rolId", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario createdUser = usuarioRepository.findByCorreo(email).orElseThrow();
        int userId = createdUser.getId();
        assertTrue(passwordEncoder.matches("initial-pass", createdUser.getClave()));

        mockMvc.perform(get("/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(email)));
        mockMvc.perform(get("/api/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(email)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("initial-pass"))));

        String updatedEmail = "safari-updated-" + suffix + "@example.com";
        mockMvc.perform(post("/usuarios/actualizar")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(userId))
                        .param("nombre", "Usuario Editado " + suffix)
                        .param("correo", updatedEmail)
                        .param("telefono", "555-0200")
                        .param("clave", "")
                        .param("rolId", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario updatedUser = usuarioRepository.findById(userId).orElseThrow();
        assertTrue(passwordEncoder.matches("initial-pass", updatedUser.getClave()));
        mockMvc.perform(get("/api/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(updatedEmail)));

        mockMvc.perform(post("/usuarios/eliminar")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(userId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        assertFalse(usuarioRepository.existsById(userId));

        mockMvc.perform(delete("/api/roles/{id}", roleId).session(adminSession))
                .andExpect(status().isNoContent());
        assertFalse(rolRepository.existsById(roleId));
    }

    @Test
    void guestsAndRegularUsersCannotManageUsersOrRoles() throws Exception {
        Usuario regularUser = usuarioRepository.findByCorreo("usuario@safari.com").orElseThrow();
        MockHttpSession userSession = new MockHttpSession();
        userSession.setAttribute("usuarioLogueado", regularUser);

        mockMvc.perform(post("/api/roles")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nombre\":\"No autorizado\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/usuarios/guardar")
                        .session(userSession)
                        .param("nombre", "No autorizado")
                        .param("correo", "unauthorized@safari.com")
                        .param("clave", "password")
                        .param("rolId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void safariAdminLoginOpensProtectedScreensAndRejectsWrongPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("correo", "admin@safari.com")
                        .param("clave", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/usuarios").session(session))
                .andExpect(status().isOk());

        MockHttpSession rejectedSession = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .session(rejectedSession)
                        .param("correo", "admin@safari.com")
                        .param("clave", "incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/usuarios").session(rejectedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado",
                usuarioRepository.findByCorreo("admin@safari.com").orElseThrow());
        return session;
    }
}
