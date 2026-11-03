package com.example.productos;

import com.example.productos.model.Producto;
import com.example.productos.model.Rol;
import com.example.productos.model.Salida;
import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerSalesFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SalidaRepository salidaRepository;

    @Test
    void registrationAwardsOneTimeDiscountAppliedToRegisteredCustomerSale() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String correo = "cliente-" + suffix + "@safari.test";
        mockMvc.perform(post("/registro")
                        .param("nombre", "Cliente Bono " + suffix)
                        .param("correo", correo)
                        .param("clave", "clave-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje",
                        "¡Felicidades por tu registro! Has ganado un bono del 5% de descuento para tu primera compra."));

        Usuario cliente = usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
        assertTrue(cliente.isBonoDescuentoDisponible());

        Producto producto = crearProducto(suffix, new BigDecimal("10.00"), 5);
        MockHttpSession session = sesionAdmin();
        mockMvc.perform(post("/salidas/guardar")
                        .session(session)
                        .param("productoIds", producto.getId().toString())
                        .param("cantidades", "2")
                        .param("tipoCliente", "registrado")
                        .param("clienteId", cliente.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/salidas/*/comprobante"));

        Salida primeraVenta = salidaRepository.findAll().stream()
                .filter(salida -> cliente.getId().equals(salida.getClienteUsuarioId()))
                .findFirst().orElseThrow();
        assertEquals(0, primeraVenta.getSubtotal().compareTo(new BigDecimal("20.00")));
        assertEquals(0, primeraVenta.getMontoDescuento().compareTo(new BigDecimal("1.00")));
        assertEquals(0, primeraVenta.getMontoTotal().compareTo(new BigDecimal("19.00")));
        assertFalse(usuarioRepository.findById(cliente.getId()).orElseThrow().isBonoDescuentoDisponible());

        mockMvc.perform(get("/salidas/{id}/comprobante", primeraVenta.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente Bono " + suffix)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bono de registro (5.00%)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$19.00")));

        mockMvc.perform(post("/salidas/guardar")
                        .session(session)
                        .param("productoIds", producto.getId().toString())
                        .param("cantidades", "1")
                        .param("tipoCliente", "registrado")
                        .param("clienteId", cliente.getId().toString()))
                .andExpect(status().is3xxRedirection());
        Salida segundaVenta = salidaRepository.findAll().stream()
                .filter(salida -> cliente.getId().equals(salida.getClienteUsuarioId())
                        && !salida.getId().equals(primeraVenta.getId()))
                .findFirst().orElseThrow();
        assertEquals(0, segundaVenta.getMontoDescuento().compareTo(BigDecimal.ZERO));
        assertEquals(0, segundaVenta.getMontoTotal().compareTo(new BigDecimal("10.00")));
    }

    @Test
    void temporaryCustomerStaysOutOfUserTableAndCanBeRegisteredFromSalePrompt() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String temporalEmail = "temporal-" + suffix + "@safari.test";
        Producto producto = crearProducto(suffix, new BigDecimal("12.50"), 4);
        MockHttpSession session = sesionVendedor();

        mockMvc.perform(get("/usuarios").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        String adminOnlyEmail = "vendedor-no-admin-" + suffix + "@safari.test";
        mockMvc.perform(post("/usuarios/guardar")
                        .session(session)
                        .param("nombre", "No autorizado")
                        .param("correo", adminOnlyEmail)
                        .param("clave", "clave-segura-123")
                        .param("rolesIds", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        assertTrue(usuarioRepository.findByCorreoIgnoreCase(adminOnlyEmail).isEmpty());

        mockMvc.perform(post("/salidas/guardar")
                        .session(session)
                        .param("productoIds", producto.getId().toString())
                        .param("cantidades", "1")
                        .param("tipoCliente", "temporal")
                        .param("clienteNombre", "Cliente temporal " + suffix)
                        .param("clienteTelefono", "5550100")
                        .param("clienteCorreo", temporalEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/salidas/*/registrar-cliente"));

        Salida venta = salidaRepository.findAll().stream()
                .filter(salida -> temporalEmail.equals(salida.getClienteCorreo()))
                .findFirst().orElseThrow();
        assertEquals(0, venta.getMontoTotal().compareTo(new BigDecimal("12.50")));
        assertEquals(0, venta.getSubtotal().compareTo(new BigDecimal("12.50")));
        assertTrue(usuarioRepository.findByCorreoIgnoreCase(temporalEmail).isEmpty());

        mockMvc.perform(get("/salidas/{id}/registrar-cliente", venta.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("¿Quieres registrar este usuario")));
        mockMvc.perform(get("/salidas/{id}/comprobante", venta.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente no registrado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente temporal " + suffix)));

        mockMvc.perform(post("/salidas/{id}/registrar-cliente", venta.getId()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios?modo=cliente&ventaId=" + venta.getId()));
        mockMvc.perform(get("/usuarios")
                        .session(session)
                        .param("modo", "cliente")
                        .param("ventaId", venta.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir usuario al sistema")));

        String permanentEmail = "cliente-permanente-" + suffix + "@safari.test";
        mockMvc.perform(post("/usuarios/guardar-cliente")
                        .session(session)
                        .param("ventaId", venta.getId().toString())
                        .param("nombre", "Cliente registrado " + suffix)
                        .param("correo", permanentEmail)
                        .param("telefono", "5550199")
                        .param("clave", "clave-segura-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salidas/" + venta.getId() + "/comprobante"));

        Usuario cliente = usuarioRepository.findByCorreoIgnoreCase(permanentEmail).orElseThrow();
        assertTrue(cliente.isBonoDescuentoDisponible());
        assertEquals("CLIENTE", cliente.getNombresRoles());
        Salida ventaActualizada = salidaRepository.findById(venta.getId()).orElseThrow();
        assertEquals(cliente.getId(), ventaActualizada.getClienteUsuarioId());
        assertEquals("Cliente registrado " + suffix, ventaActualizada.getClienteNombre());
        assertNotNull(ventaActualizada.getClienteCorreo());
        mockMvc.perform(get("/salidas/{id}/comprobante", venta.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usuario registrado")));
    }

    private Producto crearProducto(String suffix, BigDecimal precio, int stock) {
        Producto producto = new Producto();
        producto.setCodigo("CLI-" + suffix);
        producto.setNombre("Producto para cliente " + suffix);
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioActual(precio);
        producto.setStockActual(stock);
        producto.setStockMinimo(0);
        return productoRepository.saveAndFlush(producto);
    }

    private MockHttpSession sesionAdmin() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado",
                usuarioRepository.findByCorreo("admin@safari.com").orElseThrow());
        return session;
    }

    private MockHttpSession sesionVendedor() {
        Rol vendedor = rolRepository.findByNombreIgnoreCase("VENDEDOR").orElseThrow();
        Usuario usuario = new Usuario();
        String correo = "vendedor-" + UUID.randomUUID().toString().substring(0, 8) + "@safari.test";
        usuario.setNombre("Vendedor de prueba");
        usuario.setCorreo(correo);
        usuario.setClave("hash-de-prueba");
        usuario.setRoles(Set.of(vendedor));
        usuario.setEstatus(1);
        usuario = usuarioRepository.saveAndFlush(usuario);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado", usuario);
        return session;
    }
}
