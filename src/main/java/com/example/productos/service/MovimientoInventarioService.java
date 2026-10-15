package com.example.productos.service;

import com.example.productos.model.DetalleEntrada;
import com.example.productos.model.DetalleSalida;
import com.example.productos.model.Entrada;
import com.example.productos.model.Producto;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Salida;
import com.example.productos.model.Usuario;
import com.example.productos.repository.DetalleEntradaRepository;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MovimientoInventarioService {
    private final ProductoRepository productoRepository;
    private final EntradaRepository entradaRepository;
    private final DetalleEntradaRepository detalleEntradaRepository;
    private final SalidaRepository salidaRepository;
    private final DetalleSalidaRepository detalleSalidaRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimientoInventarioService(ProductoRepository productoRepository,
                                       EntradaRepository entradaRepository,
                                       DetalleEntradaRepository detalleEntradaRepository,
                                       SalidaRepository salidaRepository,
                                       DetalleSalidaRepository detalleSalidaRepository,
                                       ProveedorRepository proveedorRepository,
                                       UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
        this.detalleEntradaRepository = detalleEntradaRepository;
        this.salidaRepository = salidaRepository;
        this.detalleSalidaRepository = detalleSalidaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrarEntrada(Integer proveedorId, Integer productoId, int cantidad,
                                 BigDecimal precioCompra, String observaciones, Integer usuarioId) {
        validarCantidadYPrecio(cantidad, precioCompra);
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un proveedor válido."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de la sesión."));
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un producto válido."));

        Entrada entrada = new Entrada();
        entrada.setProveedor(proveedor);
        entrada.setUsuario(usuario);
        entrada.setFechaEntrada(LocalDateTime.now());
        entrada.setObservaciones(limpiar(observaciones));
        entrada = entradaRepository.save(entrada);

        DetalleEntrada detalle = new DetalleEntrada();
        detalle.setEntrada(entrada);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioCompra(precioCompra);
        detalleEntradaRepository.save(detalle);

        producto.setStockActual((producto.getStockActual() == null ? 0 : producto.getStockActual()) + cantidad);
        producto.setPrecioCompra(precioCompra);
        productoRepository.save(producto);
    }

    @Transactional
    public void registrarSalida(Integer productoId, int cantidad, String observaciones, Integer usuarioId) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de la sesión."));
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un producto válido."));
        int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
        if (stockActual < cantidad) {
            throw new IllegalArgumentException("No hay existencias suficientes para completar la venta.");
        }

        BigDecimal precioVenta = producto.getPrecioActual();
        BigDecimal total = precioVenta.multiply(BigDecimal.valueOf(cantidad));
        Salida salida = new Salida();
        salida.setUsuario(usuario);
        salida.setFechaSalida(LocalDateTime.now());
        salida.setMontoTotal(total);
        salida.setObservaciones(limpiar(observaciones));
        salida = salidaRepository.save(salida);

        DetalleSalida detalle = new DetalleSalida();
        detalle.setSalida(salida);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioVenta(precioVenta);
        detalleSalidaRepository.save(detalle);

        producto.setStockActual(stockActual - cantidad);
        productoRepository.save(producto);
    }

    private void validarCantidadYPrecio(int cantidad, BigDecimal precio) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException("El precio de compra debe ser cero o mayor.");
        }
    }

    private String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
