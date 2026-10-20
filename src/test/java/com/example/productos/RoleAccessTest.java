package com.example.productos;

import com.example.productos.controller.ProductoWebController;
import com.example.productos.model.AuditoriaLog;
import com.example.productos.model.Permiso;
import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import com.example.productos.model.Vista;
import com.example.productos.repository.AuditoriaLogRepository;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.RbacService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoWebController productoWebController;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private RbacService rbacService;

    @Test
    void guestCanBrowsePublicCatalog() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Catálogo de consulta")));
    }

    @Test
    void regularUserCannotAccessProductEdition() throws Exception {
        mockMvc.perform(get("/productos/editar/1").session(sessionForRole("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void regularUserCannotOpenSalesScreen() throws Exception {
        mockMvc.perform(get("/salidas").session(sessionForRole("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/salidas/1/comprobante").session(sessionForRole("USER")))
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
        MockHttpSession session = sessionForRole("VENDEDOR");

        mockMvc.perform(get("/salidas").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-add-disclosure")));
        mockMvc.perform(get("/entradas").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-add-disclosure")));
        mockMvc.perform(get("/api/entradas").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/proveedores").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminCanReadSuppliersApi() throws Exception {
        mockMvc.perform(get("/api/proveedores").session(sessionForRole("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void roleGrantsAndRevocationsControlCatalogAccess() throws Exception {
        Rol userRole = rolRepository.findAll().stream()
                .filter(role -> role.getNombre().equalsIgnoreCase("USER"))
                .findFirst().orElseThrow();
        Usuario admin = usuarioRepository.findByCorreo("admin@safari.com").orElseThrow();
        Vista categories = rbacService.listarVistas().stream()
                .filter(view -> view.getRutaAcceso().equals("/catalogos/categorias"))
                .findFirst().orElseThrow();
        Permiso consult = rbacService.listarPermisos().stream()
                .filter(permission -> permission.getNombre().equalsIgnoreCase("CONSULTAR"))
                .findFirst().orElseThrow();
        String grant = RbacService.clave(categories.getModulo().getId(), categories.getId(), consult.getId());

        rbacService.guardarAsignaciones(userRole.getId(), List.of(grant), admin.getId());
        assertTrue(auditoriaLogRepository.findAll().stream()
                .map(AuditoriaLog::getAccion)
                .anyMatch(action -> action.equals("PERMISOS_ROL_ACTUALIZADOS")));
        mockMvc.perform(get("/catalogos/categorias").session(sessionForRole("USER")))
                .andExpect(status().isOk());

        rbacService.guardarAsignaciones(userRole.getId(), List.of(), admin.getId());
        mockMvc.perform(get("/catalogos/categorias").session(sessionForRole("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void deniedApiAccessIsWrittenToAuditLog() throws Exception {
        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isForbidden());

        assertTrue(auditoriaLogRepository.findAll().stream()
                .map(AuditoriaLog::getAccion)
                .anyMatch(action -> action.startsWith("ACCESO_DENEGADO /api/proveedores")));
    }

    @Test
    void authenticatedDashboardShowsVisualMetrics() throws Exception {
        mockMvc.perform(get("/").session(sessionForRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dashboard-analytics")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"progressbar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Actividad de este mes")));
    }

    @Test
    void onlyAdminRoleHasEditAccess() {
        Rol rol = new Rol();
        rol.setNombre("EDITOR");

        Usuario usuario = new Usuario();
        usuario.setRoles(java.util.Set.of(rol));

        assertFalse(productoWebController.usuarioPuedeEditar(usuario));
    }

    @Test
    void adminRoleCanEdit() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setRoles(java.util.Set.of(rol));

        assertTrue(productoWebController.usuarioPuedeEditar(usuario));
    }

    private MockHttpSession sessionForRole(String roleName) {
        Rol role = rolRepository.findAll().stream()
                .filter(candidate -> candidate.getNombre().equalsIgnoreCase(roleName))
                .findFirst()
                .orElseThrow();
        Usuario usuario = new Usuario();
        usuario.setNombre("Test " + roleName);
        usuario.setCorreo("role-access-" + UUID.randomUUID() + "@example.com");
        usuario.setClave("test-password");
        usuario.setRoles(java.util.Set.of(role));
        usuario.setEstatus(1);
        usuario = usuarioRepository.save(usuario);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);
        return session;
    }
}
