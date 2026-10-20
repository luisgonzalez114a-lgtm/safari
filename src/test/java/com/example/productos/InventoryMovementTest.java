package com.example.productos;

import com.example.productos.model.Producto;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Usuario;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.AuditService;
import com.example.productos.service.MovimientoInventarioService;
import com.example.productos.service.ProductoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.show-sql=false")
@Transactional
class InventoryMovementTest {
    @Autowired
    private MovimientoInventarioService movimientoService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private SalidaRepository salidaRepository;

    @Autowired
    private DetalleSalidaRepository detalleSalidaRepository;

    @Autowired
    private EntityManager entityManager;

    @SpyBean
    private AuditService auditService;

    @Test
    void receiptIncreasesStockAndSaleDecreasesItAtCurrentPrice() {
        Usuario vendedor = usuarioRepository.findByCorreo("vendedor@safari.com").orElseThrow();
        Proveedor proveedor = proveedorRepository.findAll().get(0);
        Producto producto = new Producto();
        producto.setCodigo("TEST-MOV-001");
        producto.setNombre("Producto para prueba de movimientos");
        producto.setPrecioCompra(new BigDecimal("4.00"));
        producto.setPrecioActual(new BigDecimal("7.50"));
        producto.setStockActual(3);
        producto.setStockMinimo(0);
        producto = productoRepository.saveAndFlush(producto);

        movimientoService.registrarEntrada(proveedor.getId(), producto.getId(), 4,
                new BigDecimal("5.00"), "Prueba", vendedor.getId());
        Producto conEntrada = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(7, conEntrada.getStockActual());
        assertEquals(new BigDecimal("5.00"), conEntrada.getPrecioCompra());

        movimientoService.registrarSalida(producto.getId(), 2, "Venta de prueba", vendedor.getId());
        Producto conSalida = productoRepository.findById(producto.getId()).orElseThrow();
        assertEquals(5, conSalida.getStockActual());
    }

    @Test
    void saleCannotOversellAvailableStock() {
        Usuario vendedor = usuarioRepository.findByCorreo("vendedor@safari.com").orElseThrow();
        Producto producto = new Producto();
        producto.setCodigo("TEST-MOV-002");
        producto.setNombre("Producto con pocas existencias");
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioActual(new BigDecimal("3.00"));
        producto.setStockActual(1);
        producto.setStockMinimo(0);
        producto = productoRepository.saveAndFlush(producto);

        Integer productoId = producto.getId();
        assertThrows(IllegalArgumentException.class,
                () -> movimientoService.registrarSalida(productoId, 2, null, vendedor.getId()));
        assertEquals(1, productoRepository.findById(productoId).orElseThrow().getStockActual());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void auditFailureRollsBackSaleHeaderDetailAndStockChange() {
        Usuario vendedor = usuarioRepository.findByCorreo("vendedor@safari.com").orElseThrow();
        Producto producto = new Producto();
        producto.setCodigo("TEST-MOV-ROLLBACK");
        producto.setNombre("Producto para prueba de reversión");
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioActual(new BigDecimal("3.00"));
        producto.setStockActual(5);
        producto.setStockMinimo(0);
        producto = productoRepository.saveAndFlush(producto);
        Integer productoId = producto.getId();
        long salidasAntes = salidaRepository.count();
        long detallesAntes = detalleSalidaRepository.count();

        doThrow(new IllegalStateException("Fallo simulado al escribir auditoría"))
                .when(auditService).registrar(eq(vendedor.getId()), eq("REGISTRAR_VENTA"),
                        eq("salidas"), anyInt());

        assertThrows(IllegalStateException.class,
                () -> movimientoService.registrarSalida(productoId, 2, "Prueba de reversión", vendedor.getId()));

        entityManager.clear();
        assertEquals(salidasAntes, salidaRepository.count());
        assertEquals(detallesAntes, detalleSalidaRepository.count());
        assertEquals(5, productoRepository.findById(productoId).orElseThrow().getStockActual());
    }

    @Test
    void groupedSaleWithOneInsufficientItemDoesNotPartiallyChangeAnything() {
        Usuario vendedor = usuarioRepository.findByCorreo("vendedor@safari.com").orElseThrow();
        Producto available = crearProducto("TEST-GROUP-OK", "Producto disponible", 5);
        Producto insufficient = crearProducto("TEST-GROUP-LOW", "Producto sin stock", 1);
        long salesBefore = salidaRepository.count();
        long detailsBefore = detalleSalidaRepository.count();

        assertThrows(IllegalArgumentException.class, () -> movimientoService.registrarSalida(List.of(
                new MovimientoInventarioService.LineaVenta(available.getId(), 2),
                new MovimientoInventarioService.LineaVenta(insufficient.getId(), 2)),
                "Venta que no debe procesarse parcialmente", vendedor.getId()));

        assertEquals(salesBefore, salidaRepository.count());
        assertEquals(detailsBefore, detalleSalidaRepository.count());
        assertEquals(5, productoRepository.findById(available.getId()).orElseThrow().getStockActual());
        assertEquals(1, productoRepository.findById(insufficient.getId()).orElseThrow().getStockActual());
    }

    private Producto crearProducto(String codigo, String nombre, int stock) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecioCompra(BigDecimal.ONE);
        producto.setPrecioActual(new BigDecimal("2.50"));
        producto.setStockActual(stock);
        producto.setStockMinimo(0);
        return productoRepository.saveAndFlush(producto);
    }
}
