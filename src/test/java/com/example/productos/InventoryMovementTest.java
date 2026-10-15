package com.example.productos;

import com.example.productos.model.Producto;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Usuario;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.MovimientoInventarioService;
import com.example.productos.service.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
