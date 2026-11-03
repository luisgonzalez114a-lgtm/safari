package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.model.Salida;
import com.example.productos.model.DetalleSalida;
import com.example.productos.model.Entrada;
import com.example.productos.model.DetalleEntrada;
import com.example.productos.model.Producto;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.DetalleEntradaRepository;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.MovimientoInventarioService;
import com.example.productos.service.ProductoRepository;
import com.example.productos.service.RbacService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MovimientoWebController {
    private final MovimientoInventarioService movimientoService;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final EntradaRepository entradaRepository;
    private final DetalleEntradaRepository detalleEntradaRepository;
    private final SalidaRepository salidaRepository;
    private final DetalleSalidaRepository detalleSalidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final RbacService rbacService;

    public MovimientoWebController(MovimientoInventarioService movimientoService,
                                   ProductoRepository productoRepository,
                                   ProveedorRepository proveedorRepository,
                                   EntradaRepository entradaRepository,
                                   DetalleEntradaRepository detalleEntradaRepository,
                                   SalidaRepository salidaRepository,
                                   DetalleSalidaRepository detalleSalidaRepository,
                                   UsuarioRepository usuarioRepository,
                                   CategoriaRepository categoriaRepository,
                                   MarcaRepository marcaRepository,
                                   RbacService rbacService) {
        this.movimientoService = movimientoService;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.entradaRepository = entradaRepository;
        this.detalleEntradaRepository = detalleEntradaRepository;
        this.salidaRepository = salidaRepository;
        this.detalleSalidaRepository = detalleSalidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.rbacService = rbacService;
    }

    @GetMapping("/entradas")
    public String entradas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());
        model.addAttribute("puedeCrearProducto",
                usuario != null && rbacService.usuarioTienePermiso(usuario.getId(), "/inventario", RbacService.CREAR));
        List<Entrada> entradas = entradaRepository.findAll().stream()
                .sorted(Comparator.comparing(e -> e.getFechaEntrada(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        Map<Integer, List<DetalleEntrada>> detallesPorEntrada = detalleEntradaRepository.findAll().stream()
                .collect(Collectors.groupingBy(detalle -> detalle.getEntrada().getId()));
        model.addAttribute("entradasResumen", entradas.stream()
                .map(entrada -> {
                    List<DetalleEntrada> detalles = detallesPorEntrada.getOrDefault(entrada.getId(), List.of());
                    BigDecimal total = detalles.stream()
                            .map(detalle -> detalle.getPrecioCompra()
                                    .multiply(BigDecimal.valueOf(detalle.getCantidad())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int totalCantidad = detalles.stream().mapToInt(DetalleEntrada::getCantidad).sum();
                    return new EntradaResumen(entrada, detalles, total, totalCantidad);
                })
                .toList());
        return "entradas";
    }

    @PostMapping("/entradas/guardar")
    public String guardarEntrada(@RequestParam MultiValueMap<String, String> parametros,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        try {
            if (parametros.getOrDefault("tipoProducto", List.of()).contains("NUEVO")
                    && (usuario == null || !rbacService.usuarioTienePermiso(
                    usuario.getId(), "/inventario", RbacService.CREAR))) {
                throw new IllegalArgumentException("No tienes permiso para crear productos desde una entrada.");
            }
            List<MovimientoInventarioService.LineaEntrada> lineas = convertirLineasEntrada(parametros);
            Entrada entrada = movimientoService.registrarEntrada(
                    parsearEntero(parametros.getFirst("proveedorId"), "Selecciona un proveedor válido."),
                    parametros.getFirst("referenciaDocumento"),
                    parametros.getFirst("observaciones"),
                    usuario.getId(),
                    lineas);
            attributes.addFlashAttribute("mensaje", "Entrada guardada; las existencias se actualizaron.");
            return "redirect:/entradas/" + entrada.getId() + "/comprobante";
        } catch (IllegalArgumentException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/entradas";
    }

    @GetMapping("/entradas/{entradaId}/comprobante")
    public String comprobanteEntrada(@PathVariable Integer entradaId, HttpSession session, Model model) {
        Entrada entrada = entradaRepository.findById(entradaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la entrada."));
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        var detalles = detalleEntradaRepository.findAllByEntrada_IdOrderByIdAsc(entradaId);
        BigDecimal totalEntrada = detalles.stream()
                .map(detalle -> detalle.getPrecioCompra().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("entrada", entrada);
        model.addAttribute("detallesEntrada", detalles);
        model.addAttribute("totalEntrada", totalEntrada);
        return "comprobante-entrada";
    }

    private List<MovimientoInventarioService.LineaEntrada> convertirLineasEntrada(
            MultiValueMap<String, String> parametros) {
        List<String> tipos = parametros.get("tipoProducto");
        List<String> productoIds = parametros.get("productoId");
        List<String> cantidades = parametros.get("cantidad");
        List<String> preciosCompra = parametros.get("precioCompra");
        int cantidadLineas = tipos != null ? tipos.size()
                : Math.max(productoIds == null ? 0 : productoIds.size(),
                Math.max(cantidades == null ? 0 : cantidades.size(),
                        preciosCompra == null ? 0 : preciosCompra.size()));
        if (cantidadLineas < 1 || cantidadLineas > 100) {
            throw new IllegalArgumentException("Agrega entre uno y 100 productos a la entrada.");
        }
        for (String campo : List.of("productoId", "codigoNuevo", "nombreNuevo", "descripcionNueva",
                "marcaIdNueva", "categoriaIdNueva", "precioVentaNuevo", "stockMinimoNuevo",
                "cantidad", "precioCompra", "tipoProducto")) {
            List<String> valores = parametros.get(campo);
            if (valores != null && valores.size() != cantidadLineas) {
                throw new IllegalArgumentException("Revisa las líneas de productos de la entrada e inténtalo otra vez.");
            }
        }

        List<MovimientoInventarioService.LineaEntrada> lineas = new ArrayList<>();
        for (int i = 0; i < cantidadLineas; i++) {
            String tipo = obtenerValor(tipos, i);
            if (tipo == null) {
                tipo = "EXISTENTE";
            }
            if (!"EXISTENTE".equals(tipo) && !"NUEVO".equals(tipo)) {
                throw new IllegalArgumentException("Selecciona si cada producto ya existe o es nuevo.");
            }
            Integer productoId = parsearEnteroOpcional(obtenerValor(productoIds, i),
                    "Selecciona un producto válido.");
            if ("NUEVO".equals(tipo) && productoId != null) {
                throw new IllegalArgumentException("No mezcles un producto existente con los datos de un producto nuevo.");
            }
            if ("EXISTENTE".equals(tipo) && productoId == null) {
                throw new IllegalArgumentException("Selecciona un producto del catálogo.");
            }
            lineas.add(new MovimientoInventarioService.LineaEntrada(
                    "NUEVO".equals(tipo) ? null : productoId,
                    obtenerValor(parametros.get("codigoNuevo"), i),
                    obtenerValor(parametros.get("nombreNuevo"), i),
                    obtenerValor(parametros.get("descripcionNueva"), i),
                    parsearEnteroOpcional(obtenerValor(parametros.get("marcaIdNueva"), i),
                            "Selecciona una marca válida."),
                    parsearEnteroOpcional(obtenerValor(parametros.get("categoriaIdNueva"), i),
                            "Selecciona una categoría válida."),
                    parsearDecimalOpcional(obtenerValor(parametros.get("precioVentaNuevo"), i),
                            "Indica un precio de venta válido."),
                    parsearEnteroOpcional(obtenerValor(parametros.get("stockMinimoNuevo"), i),
                            "Indica un stock mínimo válido."),
                    parsearEnteroOpcional(obtenerValor(cantidades, i), "Indica una cantidad recibida válida."),
                    parsearDecimalOpcional(obtenerValor(preciosCompra, i), "Indica un costo unitario válido.")));
        }
        return lineas;
    }

    private String obtenerValor(List<String> valores, int indice) {
        return valores == null || indice >= valores.size() ? null : valores.get(indice);
    }

    private Integer parsearEntero(String valor, String error) {
        Integer resultado = parsearEnteroOpcional(valor, error);
        if (resultado == null) {
            throw new IllegalArgumentException(error);
        }
        return resultado;
    }

    private Integer parsearEnteroOpcional(String valor, String error) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(error, exception);
        }
    }

    private BigDecimal parsearDecimalOpcional(String valor, String error) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(error, exception);
        }
    }

    public record EntradaResumen(Entrada entrada, List<DetalleEntrada> detalles, BigDecimal total,
                                 int totalCantidad) {}

    @GetMapping("/salidas")
    public String salidas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        List<Producto> productos = productoRepository.findAll();
        productos.sort(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Producto::getCodigo, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("clientesRegistrados", usuarioRepository.findAll().stream()
                .filter(cliente -> cliente.getEstatus() != null && cliente.getEstatus() == 1
                        && cliente.tieneRol("CLIENTE"))
                .sorted(Comparator.comparing(Usuario::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList());
        model.addAttribute("productos", productos);
        model.addAttribute("hayProductosDisponibles",
                productos.stream().anyMatch(producto -> producto.getStockActual() != null
                        && producto.getStockActual() > 0));
        List<Salida> salidas = salidaRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getFechaSalida(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<DetalleSalida> detalles = detalleSalidaRepository.findAllByOrderBySalida_FechaSalidaDescIdAsc();
        Map<Integer, List<DetalleSalida>> detallesPorSalida = detalles.stream()
                .collect(Collectors.groupingBy(detalle -> detalle.getSalida().getId()));
        model.addAttribute("ventas", salidas.stream()
                .map(salida -> new VentaResumen(salida,
                        detallesPorSalida.getOrDefault(salida.getId(), List.of())))
                .toList());
        return "salidas";
    }

    @GetMapping("/salidas/{salidaId}/comprobante")
    public String comprobanteVenta(@PathVariable Integer salidaId, HttpSession session, Model model) {
        Salida salida = salidaRepository.findById(salidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta."));
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("salida", salida);
        model.addAttribute("detallesVenta", detalleSalidaRepository.findAllBySalida_IdOrderByIdAsc(salidaId));
        return "comprobante-venta";
    }

    @GetMapping("/salidas/{salidaId}/registrar-cliente")
    public String preguntarRegistroCliente(@PathVariable Integer salidaId, HttpSession session, Model model) {
        Salida salida = salidaRepository.findById(salidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta."));
        if (salida.getClienteUsuarioId() != null || salida.getClienteNombre() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró un cliente pendiente de registro.");
        }
        model.addAttribute("usuarioLogueado", session.getAttribute("usuarioLogueado"));
        model.addAttribute("salida", salida);
        return "registrar-cliente-venta";
    }

    @PostMapping("/salidas/{salidaId}/registrar-cliente")
    public String registrarClienteDeVenta(@PathVariable Integer salidaId) {
        Salida salida = salidaRepository.findById(salidaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la venta."));
        if (salida.getClienteUsuarioId() != null || salida.getClienteNombre() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró un cliente pendiente de registro.");
        }
        return "redirect:/usuarios?modo=cliente&ventaId=" + salidaId;
    }

    @PostMapping("/salidas/guardar")
    public String guardarSalida(@RequestParam(required = false) List<String> productoIds,
                                @RequestParam(required = false) List<String> cantidades,
                                @RequestParam(required = false) Integer productoId,
                                @RequestParam(required = false) Integer cantidad,
                                @RequestParam(required = false) String observaciones,
                                @RequestParam(required = false) String tipoCliente,
                                @RequestParam(required = false) Integer clienteId,
                                @RequestParam(required = false) String clienteNombre,
                                @RequestParam(required = false) String clienteTelefono,
                                @RequestParam(required = false) String clienteCorreo,
                                HttpSession session,
                                RedirectAttributes attributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        try {
            boolean ventaSimple = productoIds == null && productoId != null && cantidad != null;
            List<MovimientoInventarioService.LineaVenta> lineas = ventaSimple
                    ? List.of(new MovimientoInventarioService.LineaVenta(productoId, cantidad))
                    : obtenerLineas(productoIds, cantidades);
            if ("temporal".equals(tipoCliente)) {
                if (clienteNombre == null || clienteNombre.isBlank()) {
                    throw new IllegalArgumentException("Escribe el nombre del cliente no registrado.");
                }
                clienteId = null;
            } else if ("registrado".equals(tipoCliente)) {
                if (clienteId == null) {
                    throw new IllegalArgumentException("Selecciona un cliente registrado.");
                }
                clienteNombre = null;
                clienteTelefono = null;
                clienteCorreo = null;
            } else if (tipoCliente != null) {
                throw new IllegalArgumentException("Selecciona un tipo de cliente válido.");
            }
            var salida = movimientoService.registrarSalida(lineas, observaciones, usuario.getId(),
                    clienteId, clienteNombre, clienteTelefono, clienteCorreo);
            attributes.addFlashAttribute("mensaje", "Venta registrada; las existencias se actualizaron.");
            if ("temporal".equals(tipoCliente)) {
                return "redirect:/salidas/" + salida.getId() + "/registrar-cliente";
            }
            if (!ventaSimple) {
                return "redirect:/salidas/" + salida.getId() + "/comprobante";
            }
        } catch (IllegalArgumentException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/salidas";
    }

    private List<MovimientoInventarioService.LineaVenta> obtenerLineas(List<String> productoIds,
                                                                        List<String> cantidades) {
        if (productoIds == null || cantidades == null || productoIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Revisa las líneas de productos y cantidades de la venta.");
        }
        List<MovimientoInventarioService.LineaVenta> lineas = new java.util.ArrayList<>();
        for (int i = 0; i < productoIds.size(); i++) {
            String productoId = productoIds.get(i) == null ? "" : productoIds.get(i).trim();
            String cantidad = cantidades.get(i) == null ? "" : cantidades.get(i).trim();
            if (productoId.isEmpty() && cantidad.isEmpty()) {
                continue;
            }
            if (productoId.isEmpty()) {
                throw new IllegalArgumentException("Selecciona un producto para cada cantidad ingresada.");
            }
            Integer cantidadProducto = null;
            if (!cantidad.isEmpty()) {
                try {
                    cantidadProducto = Integer.valueOf(cantidad);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Escribe una cantidad válida para cada producto.");
                }
            }
            try {
                lineas.add(new MovimientoInventarioService.LineaVenta(
                        Integer.valueOf(productoId), cantidadProducto));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Selecciona un producto válido para cada línea.");
            }
        }
        return lineas;
    }

    public record VentaResumen(Salida salida, List<DetalleSalida> detalles) {}
}
