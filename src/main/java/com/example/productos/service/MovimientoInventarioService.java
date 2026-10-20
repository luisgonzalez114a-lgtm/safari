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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoInventarioService {
    private final ProductoRepository productoRepository;
    private final EntradaRepository entradaRepository;
    private final DetalleEntradaRepository detalleEntradaRepository;
    private final SalidaRepository salidaRepository;
    private final DetalleSalidaRepository detalleSalidaRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public MovimientoInventarioService(ProductoRepository productoRepository,
                                       EntradaRepository entradaRepository,
                                       DetalleEntradaRepository detalleEntradaRepository,
                                       SalidaRepository salidaRepository,
                                       DetalleSalidaRepository detalleSalidaRepository,
                                       ProveedorRepository proveedorRepository,
                                       UsuarioRepository usuarioRepository,
                                       AuditService auditService) {
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
        this.detalleEntradaRepository = detalleEntradaRepository;
        this.salidaRepository = salidaRepository;
        this.detalleSalidaRepository = detalleSalidaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
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
        auditService.registrar(usuario.getId(), "REGISTRAR_ENTRADA", "entradas", entrada.getId());
    }

    @Transactional
    public Salida registrarSalida(Integer productoId, int cantidad, String observaciones, Integer usuarioId) {
        return registrarSalida(List.of(new LineaVenta(productoId, cantidad)), observaciones, usuarioId);
    }

    @Transactional
    public Salida registrarSalida(List<LineaVenta> lineas, String observaciones, Integer usuarioId) {
        Map<Integer, Integer> cantidadesPorProducto = consolidarLineas(lineas);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de la sesión."));

        Map<Integer, Producto> productos = new LinkedHashMap<>();
        for (Integer productoId : cantidadesPorProducto.keySet().stream().sorted().toList()) {
            Producto producto = productoRepository.findByIdForUpdate(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos seleccionados ya no existe."));
            int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
            if (stockActual < cantidadesPorProducto.get(productoId)) {
                throw new IllegalArgumentException("No hay existencias suficientes para completar la venta.");
            }
            if (producto.getPrecioActual() == null || producto.getPrecioActual().signum() < 0) {
                throw new IllegalArgumentException("Uno de los productos tiene un precio de venta inválido.");
            }
            productos.put(productoId, producto);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleSalida> detalles = new ArrayList<>();
        for (Map.Entry<Integer, Integer> linea : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(linea.getKey());
            int cantidad = linea.getValue();
            BigDecimal precioVenta = producto.getPrecioActual();
            total = total.add(precioVenta.multiply(BigDecimal.valueOf(cantidad)));

            DetalleSalida detalle = new DetalleSalida();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioVenta(precioVenta);
            detalles.add(detalle);
        }

        Salida salida = new Salida();
        salida.setUsuario(usuario);
        salida.setFechaSalida(LocalDateTime.now());
        salida.setMontoTotal(total.setScale(2, java.math.RoundingMode.HALF_UP));
        salida.setObservaciones(limpiar(observaciones));
        salida = salidaRepository.save(salida);

        for (DetalleSalida detalle : detalles) {
            Producto producto = detalle.getProducto();
            int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
            producto.setStockActual(stockActual - detalle.getCantidad());
            productoRepository.save(producto);
            detalle.setSalida(salida);
            detalleSalidaRepository.save(detalle);
        }
        auditService.registrar(usuario.getId(), "REGISTRAR_VENTA", "salidas", salida.getId());
        return salida;
    }

    private Map<Integer, Integer> consolidarLineas(List<LineaVenta> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto a la venta.");
        }
        Map<Integer, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        for (LineaVenta linea : lineas) {
            if (linea == null || linea.productoId() == null || linea.productoId() <= 0) {
                throw new IllegalArgumentException("Selecciona un producto válido en cada línea.");
            }
            if (linea.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor que cero.");
            }
            try {
                cantidadesPorProducto.merge(linea.productoId(), linea.cantidad(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("La cantidad total de un producto es demasiado grande.", exception);
            }
        }
        return cantidadesPorProducto;
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

    public record LineaVenta(Integer productoId, Integer cantidad) {}
}
