package com.example.productos.service;

import com.example.productos.model.DetalleEntrada;
import com.example.productos.model.DetalleSalida;
import com.example.productos.model.Entrada;
import com.example.productos.model.Producto;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Salida;
import com.example.productos.model.Usuario;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.DetalleEntradaRepository;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public MovimientoInventarioService(ProductoRepository productoRepository,
                                       EntradaRepository entradaRepository,
                                       DetalleEntradaRepository detalleEntradaRepository,
                                       SalidaRepository salidaRepository,
                                       DetalleSalidaRepository detalleSalidaRepository,
                                       ProveedorRepository proveedorRepository,
                                       UsuarioRepository usuarioRepository,
                                       AuditService auditService,
                                       CategoriaRepository categoriaRepository,
                                       MarcaRepository marcaRepository) {
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
        this.detalleEntradaRepository = detalleEntradaRepository;
        this.salidaRepository = salidaRepository;
        this.detalleSalidaRepository = detalleSalidaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

    @Transactional
    public Entrada registrarEntrada(Integer proveedorId, Integer productoId, int cantidad,
                                    BigDecimal precioCompra, String observaciones, Integer usuarioId) {
        return registrarEntrada(proveedorId, null, observaciones, usuarioId,
                List.of(new LineaEntrada(productoId, null, null, null, null, null,
                        null, null, cantidad, precioCompra)));
    }

    @Transactional
    public Entrada registrarEntrada(Integer proveedorId, String referenciaDocumento, String observaciones,
                                    Integer usuarioId, List<LineaEntrada> lineas) {
        List<LineaEntrada> lineasValidadas = validarLineasEntrada(lineas);
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un proveedor válido."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de la sesión."));

        Set<Integer> productosExistentes = new HashSet<>();
        Set<String> codigosNuevos = new HashSet<>();
        List<Producto> productos = new ArrayList<>();
        for (LineaEntrada linea : lineasValidadas) {
            if (linea == null) {
                throw new IllegalArgumentException("Hay una línea de producto vacía en la entrada.");
            }
            if (linea.cantidad() == null || linea.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor que cero.");
            }
            validarCantidadYPrecio(linea.cantidad(), linea.precioCompra());
            if (linea.productoId() != null) {
                if (!productosExistentes.add(linea.productoId())) {
                    throw new IllegalArgumentException("No repitas un producto en la misma entrada.");
                }
                Producto producto = productoRepository.findByIdForUpdate(linea.productoId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Uno de los productos seleccionados ya no existe."));
                int stockActual = producto.getStockActual() == null ? 0 : producto.getStockActual();
                try {
                    Math.addExact(stockActual, linea.cantidad());
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("La cantidad recibida supera el límite permitido.", exception);
                }
                productos.add(producto);
            } else {
                String codigo = limpiar(linea.codigoNuevo());
                String nombre = limpiar(linea.nombreNuevo());
                if (codigo == null || codigo.length() > 50 || nombre == null || nombre.length() > 100) {
                    throw new IllegalArgumentException("El código y nombre del producto nuevo son obligatorios; revisa sus longitudes.");
                }
                if (!codigosNuevos.add(codigo.toLowerCase(java.util.Locale.ROOT))
                        || productoRepository.existsByCodigoIgnoreCase(codigo)) {
                    throw new IllegalArgumentException("El código del producto nuevo ya está registrado o repetido.");
                }
                if (linea.descripcionNueva() != null && linea.descripcionNueva().length() > 10000) {
                    throw new IllegalArgumentException("La descripción del producto es demasiado larga.");
                }
                validarPrecio(linea.precioVenta(), "El precio de venta del producto nuevo");
                if (linea.stockMinimoNuevo() == null || linea.stockMinimoNuevo() < 0) {
                    throw new IllegalArgumentException("El stock mínimo debe ser cero o mayor.");
                }
                if (linea.marcaIdNueva() != null && !marcaRepository.existsById(linea.marcaIdNueva())) {
                    throw new IllegalArgumentException("Selecciona una marca válida para el producto nuevo.");
                }
                if (linea.categoriaIdNueva() != null && !categoriaRepository.existsById(linea.categoriaIdNueva())) {
                    throw new IllegalArgumentException("Selecciona una categoría válida para el producto nuevo.");
                }
                Producto producto = new Producto();
                producto.setCodigo(codigo);
                producto.setNombre(nombre);
                producto.setDescripcion(limpiar(linea.descripcionNueva()));
                producto.setIdMarca(linea.marcaIdNueva());
                producto.setIdCategoria(linea.categoriaIdNueva());
                producto.setPrecioActual(linea.precioVenta());
                producto.setStockMinimo(linea.stockMinimoNuevo());
                producto.setPrecioCompra(BigDecimal.ZERO);
                producto.setStockActual(0);
                productos.add(producto);
            }
        }

        String referencia = limpiar(referenciaDocumento);
        if (referencia != null && referencia.length() > 100) {
            throw new IllegalArgumentException("La referencia del documento no puede superar 100 caracteres.");
        }

        for (int i = 0; i < productos.size(); i++) {
            if (lineasValidadas.get(i).productoId() == null) {
                Producto creado = productoRepository.save(productos.get(i));
                productos.set(i, creado);
                auditService.registrar(usuario.getId(), "CREAR_PRODUCTO_DESDE_ENTRADA",
                        "productos", creado.getId());
            }
        }

        Entrada entrada = new Entrada();
        entrada.setProveedor(proveedor);
        entrada.setUsuario(usuario);
        entrada.setFechaEntrada(LocalDateTime.now());
        entrada.setReferenciaDocumento(referencia);
        entrada.setObservaciones(limpiar(observaciones));
        entrada = entradaRepository.save(entrada);

        for (int i = 0; i < lineasValidadas.size(); i++) {
            LineaEntrada linea = lineasValidadas.get(i);
            Producto producto = productos.get(i);
            DetalleEntrada detalle = new DetalleEntrada();
            detalle.setEntrada(entrada);
            detalle.setProducto(producto);
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioCompra(linea.precioCompra());
            detalleEntradaRepository.save(detalle);

            producto.setStockActual(Math.addExact(
                    producto.getStockActual() == null ? 0 : producto.getStockActual(), linea.cantidad()));
            producto.setPrecioCompra(linea.precioCompra());
            productoRepository.save(producto);
        }
        auditService.registrar(usuario.getId(), "REGISTRAR_ENTRADA", "entradas", entrada.getId());
        return entrada;
    }

    private List<LineaEntrada> validarLineasEntrada(List<LineaEntrada> lineas) {
        if (lineas == null || lineas.isEmpty() || lineas.size() > 100) {
            throw new IllegalArgumentException("Agrega entre uno y 100 productos a la entrada.");
        }
        if (lineas.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Hay una línea de producto vacía en la entrada.");
        }
        return List.copyOf(lineas);
    }

    @Transactional
    public Salida registrarSalida(Integer productoId, int cantidad, String observaciones, Integer usuarioId) {
        return registrarSalida(List.of(new LineaVenta(productoId, cantidad)), observaciones, usuarioId);
    }

    @Transactional
    public Salida registrarSalida(List<LineaVenta> lineas, String observaciones, Integer usuarioId) {
        return registrarSalida(lineas, observaciones, usuarioId, null, null, null, null);
    }

    @Transactional
    public Salida registrarSalida(List<LineaVenta> lineas, String observaciones, Integer usuarioId,
                                  Integer clienteId, String clienteNombre, String clienteTelefono,
                                  String clienteCorreo) {
        Map<Integer, Integer> cantidadesPorProducto = consolidarLineas(lineas);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de la sesión."));

        Usuario cliente = null;
        if (clienteId != null) {
            cliente = usuarioRepository.findByIdForUpdate(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Selecciona un cliente registrado válido."));
            if (cliente.getEstatus() == null || cliente.getEstatus() != 1
                    || !cliente.tieneRol("CLIENTE")) {
                throw new IllegalArgumentException("El usuario seleccionado no está disponible como cliente.");
            }
            clienteNombre = cliente.getNombre();
            clienteTelefono = cliente.getTelefono();
            clienteCorreo = cliente.getCorreo();
        }
        clienteNombre = limpiar(clienteNombre);
        clienteTelefono = limpiar(clienteTelefono);
        clienteCorreo = limpiar(clienteCorreo);
        if (clienteNombre != null && (clienteNombre.length() > 100
                || (clienteTelefono != null && clienteTelefono.length() > 20)
                || (clienteCorreo != null && (clienteCorreo.length() > 100
                || !clienteCorreo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))))) {
            throw new IllegalArgumentException("Revisa el nombre, teléfono y correo del cliente.");
        }

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

        BigDecimal subtotal = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDescuento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal porcentajeDescuento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (cliente != null && cliente.isBonoDescuentoDisponible()) {
            porcentajeDescuento = new BigDecimal("5.00");
            montoDescuento = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
            cliente.setBonoDescuentoDisponible(false);
            usuarioRepository.save(cliente);
        }

        Salida salida = new Salida();
        salida.setUsuario(usuario);
        salida.setFechaSalida(LocalDateTime.now());
        salida.setSubtotal(subtotal);
        salida.setMontoDescuento(montoDescuento);
        salida.setPorcentajeDescuento(porcentajeDescuento);
        salida.setMontoTotal(subtotal.subtract(montoDescuento).setScale(2, RoundingMode.HALF_UP));
        salida.setClienteUsuarioId(cliente == null ? null : cliente.getId());
        salida.setClienteNombre(clienteNombre);
        salida.setClienteTelefono(clienteTelefono);
        salida.setClienteCorreo(clienteCorreo);
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
        validarPrecio(precio, "El precio de compra");
    }

    private void validarPrecio(BigDecimal precio, String nombre) {
        if (precio == null || precio.signum() < 0 || precio.scale() > 2
                || precio.precision() - precio.scale() > 16) {
            throw new IllegalArgumentException(nombre + " debe tener hasta dos decimales y estar dentro del límite permitido.");
        }
    }

    private String limpiar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record LineaVenta(Integer productoId, Integer cantidad) {}

    public record LineaEntrada(Integer productoId, String codigoNuevo, String nombreNuevo,
                               String descripcionNueva, Integer marcaIdNueva, Integer categoriaIdNueva,
                               BigDecimal precioVenta, Integer stockMinimoNuevo, Integer cantidad,
                               BigDecimal precioCompra) {}
}
