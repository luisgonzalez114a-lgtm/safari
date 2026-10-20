package com.example.productos;

import com.example.productos.model.Producto;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Usuario;
import com.example.productos.repository.DetalleEntradaRepository;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventorySalesFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EntradaRepository entradaRepository;

    @Autowired
    private DetalleEntradaRepository detalleEntradaRepository;

    @Autowired
    private SalidaRepository salidaRepository;

    @Autowired
    private DetalleSalidaRepository detalleSalidaRepository;

    @Test
    void administratorCanReceiveStockSellProductsAndReviewMovementHistory() throws Exception {
        MockHttpSession adminSession = adminSession();
        Proveedor supplier = proveedorRepository.findAll().get(0);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String productCode = "MOV-" + suffix;
        String productName = "Producto SAFARI " + suffix;

        MvcResult createdProduct = mockMvc.perform(post("/api/productos")
                        .session(adminSession)
                        .contentType(APPLICATION_JSON)
                        .content(productJson(productCode, productName, 7.50, 0)))
                .andExpect(status().isCreated())
                .andReturn();
        int productId = objectMapper.readTree(createdProduct.getResponse().getContentAsString())
                .get("id").asInt();

        mockMvc.perform(post("/entradas/guardar")
                        .session(adminSession)
                        .param("proveedorId", supplier.getId().toString())
                        .param("productoId", Integer.toString(productId))
                        .param("cantidad", "5")
                        .param("precioCompra", "4.25")
                        .param("observaciones", "Recepcion SAFARI"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entradas"))
                .andExpect(flash().attribute("mensaje",
                        "Entrada guardada; las existencias se actualizaron."));

        Producto afterReceipt = productoRepository.findById(productId).orElseThrow();
        assertEquals(5, afterReceipt.getStockActual());
        assertEquals(new BigDecimal("4.25"), afterReceipt.getPrecioCompra());
        assertTrue(entradaRepository.findAll().stream()
                .anyMatch(entry -> entry.getProveedor().getId().equals(supplier.getId())));
        assertTrue(detalleEntradaRepository.findAll().stream()
                .anyMatch(detail -> detail.getProducto().getId().equals(productId)
                        && detail.getCantidad() == 5));

        mockMvc.perform(get("/api/entradas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recepcion SAFARI")));
        mockMvc.perform(get("/api/detalles-entradas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(productName)));

        mockMvc.perform(post("/salidas/guardar")
                        .session(adminSession)
                        .param("productoId", Integer.toString(productId))
                        .param("cantidad", "2")
                        .param("observaciones", "Venta SAFARI"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salidas"))
                .andExpect(flash().attribute("mensaje",
                        "Venta registrada; las existencias se actualizaron."));

        assertEquals(3, productoRepository.findById(productId).orElseThrow().getStockActual());
        assertEquals(0, salidaRepository.findAll().stream()
                .filter(sale -> "Venta SAFARI".equals(sale.getObservaciones()))
                .findFirst().orElseThrow().getMontoTotal().compareTo(new BigDecimal("15.00")));
        assertTrue(detalleSalidaRepository.findAll().stream()
                .anyMatch(detail -> detail.getProducto().getId().equals(productId)
                        && detail.getCantidad() == 2
                        && detail.getPrecioVenta().compareTo(new BigDecimal("7.50")) == 0));

        mockMvc.perform(get("/salidas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(productName)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("wide-add-disclosure")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sale-lines")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sale-product-search")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Añadir a la venta")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("name=\"observaciones\""))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("15.00")));
        mockMvc.perform(get("/api/salidas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Venta SAFARI")));
        mockMvc.perform(get("/api/detalles-salidas").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(productName)));
    }

    @Test
    void saleCannotOversellAndMovementScreensRequireAnAuthorizedSession() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Producto product = new Producto();
        product.setCodigo("LOW-" + suffix);
        product.setNombre("Pocas existencias " + suffix);
        product.setPrecioCompra(BigDecimal.ONE);
        product.setPrecioActual(new BigDecimal("3.00"));
        product.setStockActual(1);
        product.setStockMinimo(0);
        product = productoRepository.saveAndFlush(product);

        mockMvc.perform(post("/salidas/guardar")
                        .session(adminSession)
                        .param("productoId", product.getId().toString())
                        .param("cantidad", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salidas"))
                .andExpect(flash().attribute("error",
                        "No hay existencias suficientes para completar la venta."));

        assertEquals(1, productoRepository.findById(product.getId()).orElseThrow().getStockActual());
        mockMvc.perform(get("/salidas")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/salidas/guardar")
                        .param("productoId", product.getId().toString())
                        .param("cantidad", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void groupedSaleStoresAllItemsUpdatesStockAndShowsPrintableReceipt() throws Exception {
        MockHttpSession adminSession = adminSession();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Producto first = crearProducto("GROUP-A-" + suffix, "Grupo producto A " + suffix,
                new BigDecimal("7.50"), 8);
        Producto second = crearProducto("GROUP-B-" + suffix, "Grupo producto B " + suffix,
                new BigDecimal("2.25"), 10);

        MvcResult response = mockMvc.perform(post("/salidas/guardar")
                        .session(adminSession)
                        .param("productoIds", first.getId().toString(), second.getId().toString())
                        .param("cantidades", "2", "3")
                        .param("observaciones", "Venta en grupo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje",
                        "Venta registrada; las existencias se actualizaron."))
                .andReturn();

        com.example.productos.model.Salida venta = salidaRepository.findAll().stream()
                .filter(sale -> "Venta en grupo".equals(sale.getObservaciones()))
                .findFirst().orElseThrow();
        assertEquals(0, venta.getMontoTotal().compareTo(new BigDecimal("21.75")));
        assertEquals(6, productoRepository.findById(first.getId()).orElseThrow().getStockActual());
        assertEquals(7, productoRepository.findById(second.getId()).orElseThrow().getStockActual());
        assertEquals(2, detalleSalidaRepository.findAllBySalida_IdOrderByIdAsc(venta.getId()).size());
        assertEquals("/salidas/" + venta.getId() + "/comprobante",
                response.getResponse().getRedirectedUrl());

        mockMvc.perform(get("/salidas/{id}/comprobante", venta.getId()).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("COMPROBANTE DE VENTA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(first.getNombre())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(second.getNombre())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("$21.75")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No es una factura fiscal")));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("usuarioLogueado",
                usuarioRepository.findByCorreo("admin@safari.com").orElseThrow());
        return session;
    }

    private String productJson(String code, String name, double price, int stock) {
        return "{\"codigo\":\"" + code + "\",\"nombre\":\"" + name
                + "\",\"descripcion\":\"Prueba SAFARI\",\"precioCompra\":1.00,\"precioActual\":"
                + price + ",\"stockActual\":" + stock + ",\"stockMinimo\":0}";
    }

    private Producto crearProducto(String codigo, String nombre, BigDecimal precio, int stock) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioActual(precio);
        producto.setStockActual(stock);
        producto.setStockMinimo(0);
        return productoRepository.saveAndFlush(producto);
    }
}
