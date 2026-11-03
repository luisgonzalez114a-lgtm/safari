package com.example.productos;

import com.example.productos.model.Usuario;
import com.example.productos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteRegistrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void guestCanOpenCustomerRegistration() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Crear cuenta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CUENTA DE CLIENTE")));
    }

    @Test
    void registrationCreatesOnlyClientRoleWithEncodedPassword() throws Exception {
        String email = "cliente-nuevo@safari.test";
        mockMvc.perform(post("/registro")
                        .param("nombre", "Cliente Nuevo")
                        .param("correo", email)
                        .param("telefono", "5550100")
                        .param("clave", "clave-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        Usuario cliente = usuarioRepository.findByCorreoIgnoreCase(email).orElseThrow();
        assertEquals("Cliente Nuevo", cliente.getNombre());
        assertEquals("5550100", cliente.getTelefono());
        assertEquals("CLIENTE", cliente.getNombresRoles());
        assertFalse(cliente.getClave().equals("clave-segura-123"));
        assertTrue(passwordEncoder.matches("clave-segura-123", cliente.getClave()));

        mockMvc.perform(post("/login")
                        .param("correo", email.toUpperCase(java.util.Locale.ROOT))
                        .param("clave", "clave-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", cliente);
        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Catálogo de productos")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Estado del inventario"))));
        mockMvc.perform(get("/salidas").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void registrationRejectsWeakPasswordAndDuplicateEmailCaseInsensitively() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("nombre", "Cliente")
                        .param("correo", "cliente-debil@safari.test")
                        .param("clave", "corta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"));
        assertTrue(usuarioRepository.findByCorreoIgnoreCase("cliente-debil@safari.test").isEmpty());

        mockMvc.perform(post("/registro")
                        .param("nombre", "Cliente")
                        .param("correo", "admin@safari.com")
                        .param("clave", "clave-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"));
        assertTrue(usuarioRepository.findByCorreoIgnoreCase("admin@safari.com").isPresent());
    }
}
