package com.example.productos;

import com.example.productos.model.Usuario;
import com.example.productos.model.Rol;
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
import static org.hamcrest.Matchers.not;
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
            user.setRoles(admin.getRoles());
            usuarioRepository.save(user);
        }
        long totalPages = (usuarioRepository.count() + 8) / 9;

        mockMvc.perform(get("/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("users-table")))
                .andExpect(content().string(containsString("user-row-details")))
                .andExpect(content().string(containsString("user-expanded-grid")))
                .andExpect(content().string(containsString("Desactivar usuario")))
                .andExpect(content().string(containsString("topbar-user-menu")))
                .andExpect(content().string(containsString("Cerrar sesión")))
                .andExpect(content().string(containsString("user-form-disclosure")))
                .andExpect(content().string(containsString("Crear usuario")))
                .andExpect(content().string(containsString("Asignar rol a usuario")))
                .andExpect(content().string(containsString("role-checkbox-options")))
                .andExpect(content().string(containsString("type=\"checkbox\"")))
                .andExpect(content().string(containsString("Confirmar roles")))
                .andExpect(content().string(not(containsString("Mantén Ctrl"))))
                .andExpect(content().string(containsString("sidebar-group")))
                .andExpect(content().string(containsString("Usuarios y roles")))
                .andExpect(content().string(not(containsString("onclick="))))
                .andExpect(content().string(not(containsString("onsubmit="))))
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
    void administratorCanDeactivateAndReactivateUsersButNotTheirOwnAccount() throws Exception {
        MockHttpSession adminSession = adminSession();
        Usuario admin = usuarioRepository.findByCorreo("admin@safari.com").orElseThrow();
        Usuario user = usuarioRepository.findByCorreo("usuario@safari.com").orElseThrow();
        MockHttpSession userSession = new MockHttpSession();
        userSession.setAttribute("usuarioLogueado", user);

        mockMvc.perform(post("/usuarios/estado")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(user.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        assertEquals(0, usuarioRepository.findById(user.getId()).orElseThrow().getEstatus());
        mockMvc.perform(get("/entradas").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/usuarios/estado")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(user.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        assertEquals(1, usuarioRepository.findById(user.getId()).orElseThrow().getEstatus());

        mockMvc.perform(post("/usuarios/estado")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(admin.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        assertEquals(1, usuarioRepository.findById(admin.getId()).orElseThrow().getEstatus());
    }

    @Test
    void userCanHaveMultipleRolesAndReceivesCombinedPermissions() throws Exception {
        MockHttpSession adminSession = adminSession();
        Rol adminRole = rolRepository.findAll().stream()
                .filter(role -> role.getNombre().equalsIgnoreCase("ADMIN"))
                .findFirst().orElseThrow();
        Rol sellerRole = rolRepository.findAll().stream()
                .filter(role -> role.getNombre().equalsIgnoreCase("VENDEDOR"))
                .findFirst().orElseThrow();
        String email = "multi-role-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/usuarios/guardar")
                        .session(adminSession)
                        .param("nombre", "Usuario con varios roles")
                        .param("correo", email)
                        .param("clave", "multi-role-pass")
                        .param("rolesIds", adminRole.getId().toString(), sellerRole.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario usuario = usuarioRepository.findByCorreo(email).orElseThrow();
        assertEquals(2, usuario.getRoles().size());
        assertTrue(usuario.tieneRol("ADMIN"));
        assertTrue(usuario.tieneRol("VENDEDOR"));

        MockHttpSession multiRoleSession = new MockHttpSession();
        multiRoleSession.setAttribute("usuarioLogueado", usuario);
        mockMvc.perform(get("/usuarios").session(multiRoleSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/entradas").session(multiRoleSession))
                .andExpect(status().isOk());
    }

    @Test
    void administratorCanCreateEditViewAndDeleteUsersAndRoles() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String roleName = "SAFARI_TEST_" + suffix;

        mockMvc.perform(get("/usuarios").session(adminSession)).andExpect(status().isOk());
        mockMvc.perform(get("/roles").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crear rol")))
                .andExpect(content().string(containsString("permission-reference")))
                .andExpect(content().string(containsString("Gestionar usuarios, roles y catálogos")))
                .andExpect(content().string(containsString("VENDEDOR")))
                .andExpect(content().string(containsString("sidebar-group")));

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
                        .param("rolesIds", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario createdUser = usuarioRepository.findByCorreo(email).orElseThrow();
        int userId = createdUser.getId();
        assertTrue(passwordEncoder.matches("initial-pass", createdUser.getClave()));

        mockMvc.perform(get("/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(email)))
                .andExpect(content().string(containsString("Contraseña actual: protegida y no visible")))
                .andExpect(content().string(containsString("Contraseña anterior para autorizar el cambio")))
                .andExpect(content().string(not(containsString(createdUser.getClave()))));
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
                        .param("rolesIds", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        Usuario updatedUser = usuarioRepository.findById(userId).orElseThrow();
        assertTrue(passwordEncoder.matches("initial-pass", updatedUser.getClave()));
        mockMvc.perform(get("/api/usuarios").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(updatedEmail)));

        mockMvc.perform(post("/usuarios/actualizar")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(userId))
                        .param("nombre", "Cambio rechazado")
                        .param("correo", updatedEmail)
                        .param("clave", "replacement-pass")
                        .param("rolesIds", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        Usuario unchangedWithoutConfirmation = usuarioRepository.findById(userId).orElseThrow();
        assertEquals("Usuario Editado " + suffix, unchangedWithoutConfirmation.getNombre());
        assertTrue(passwordEncoder.matches("initial-pass", unchangedWithoutConfirmation.getClave()));

        mockMvc.perform(post("/usuarios/actualizar")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(userId))
                        .param("nombre", "Cambio rechazado")
                        .param("correo", updatedEmail)
                        .param("claveActual", "wrong-current-pass")
                        .param("clave", "replacement-pass")
                        .param("rolesIds", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        Usuario unchangedUser = usuarioRepository.findById(userId).orElseThrow();
        assertEquals("Usuario Editado " + suffix, unchangedUser.getNombre());
        assertTrue(passwordEncoder.matches("initial-pass", unchangedUser.getClave()));

        mockMvc.perform(post("/usuarios/actualizar")
                        .session(adminSession)
                        .param("usuarioId", Integer.toString(userId))
                        .param("nombre", "Usuario Editado " + suffix)
                        .param("correo", updatedEmail)
                        .param("claveActual", "initial-pass")
                        .param("clave", "replacement-pass")
                        .param("rolesIds", Integer.toString(roleId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));
        Usuario passwordChangedUser = usuarioRepository.findById(userId).orElseThrow();
        assertTrue(passwordEncoder.matches("replacement-pass", passwordChangedUser.getClave()));
        assertFalse(passwordEncoder.matches("initial-pass", passwordChangedUser.getClave()));

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
        mockMvc.perform(post("/usuarios/estado")
                        .session(userSession)
                        .param("usuarioId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(post("/usuarios/guardar")
                        .session(userSession)
                        .param("nombre", "No autorizado")
                        .param("correo", "unauthorized@safari.com")
                        .param("clave", "password")
                        .param("rolesIds", "1"))
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
