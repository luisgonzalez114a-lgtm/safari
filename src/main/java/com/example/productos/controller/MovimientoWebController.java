package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.model.Salida;
import com.example.productos.model.DetalleSalida;
import com.example.productos.model.Producto;
import com.example.productos.repository.DetalleEntradaRepository;
import com.example.productos.repository.DetalleSalidaRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.SalidaRepository;
import com.example.productos.service.MovimientoInventarioService;
import com.example.productos.service.ProductoRepository;
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

import java.math.BigDecimal;
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

    public MovimientoWebController(MovimientoInventarioService movimientoService,
                                   ProductoRepository productoRepository,
                                   ProveedorRepository proveedorRepository,
                                   EntradaRepository entradaRepository,
                                   DetalleEntradaRepository detalleEntradaRepository,
                                   SalidaRepository salidaRepository,
                                   DetalleSalidaRepository detalleSalidaRepository) {
        this.movimientoService = movimientoService;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.entradaRepository = entradaRepository;
        this.detalleEntradaRepository = detalleEntradaRepository;
        this.salidaRepository = salidaRepository;
        this.detalleSalidaRepository = detalleSalidaRepository;
    }

    @GetMapping("/entradas")
    public String entradas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        model.addAttribute("detalles", detalleEntradaRepository.findAll());
        model.addAttribute("entradas", entradaRepository.findAll().stream()
                .sorted(Comparator.comparing(e -> e.getFechaEntrada(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        return "entradas";
    }

    @PostMapping("/entradas/guardar")
    public String guardarEntrada(@RequestParam Integer proveedorId,
                                 @RequestParam Integer productoId,
                                 @RequestParam int cantidad,
                                 @RequestParam BigDecimal precioCompra,
                                 @RequestParam(required = false) String observaciones,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        try {
            movimientoService.registrarEntrada(proveedorId, productoId, cantidad, precioCompra,
                    observaciones, usuario.getId());
            attributes.addFlashAttribute("mensaje", "Entrada guardada; las existencias se actualizaron.");
        } catch (IllegalArgumentException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/entradas";
    }

    @GetMapping("/salidas")
    public String salidas(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        List<Producto> productos = productoRepository.findAll();
        productos.sort(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Producto::getCodigo, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("usuarioLogueado", usuario);
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

    @PostMapping("/salidas/guardar")
    public String guardarSalida(@RequestParam(required = false) List<String> productoIds,
                                @RequestParam(required = false) List<String> cantidades,
                                @RequestParam(required = false) Integer productoId,
                                @RequestParam(required = false) Integer cantidad,
                                @RequestParam(required = false) String observaciones,
                                HttpSession session,
                                RedirectAttributes attributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        try {
            boolean ventaSimple = productoIds == null && productoId != null && cantidad != null;
            List<MovimientoInventarioService.LineaVenta> lineas = ventaSimple
                    ? List.of(new MovimientoInventarioService.LineaVenta(productoId, cantidad))
                    : obtenerLineas(productoIds, cantidades);
            var salida = movimientoService.registrarSalida(lineas, observaciones, usuario.getId());
            attributes.addFlashAttribute("mensaje", "Venta registrada; las existencias se actualizaron.");
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
