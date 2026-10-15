package com.example.productos.controller;

import com.example.productos.model.Usuario;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;

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
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("detalles", detalleSalidaRepository.findAll());
        model.addAttribute("salidas", salidaRepository.findAll().stream()
                .sorted(Comparator.comparing(s -> s.getFechaSalida(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        return "salidas";
    }

    @PostMapping("/salidas/guardar")
    public String guardarSalida(@RequestParam Integer productoId,
                                @RequestParam int cantidad,
                                @RequestParam(required = false) String observaciones,
                                HttpSession session,
                                RedirectAttributes attributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        try {
            movimientoService.registrarSalida(productoId, cantidad, observaciones, usuario.getId());
            attributes.addFlashAttribute("mensaje", "Venta registrada; las existencias se actualizaron.");
        } catch (IllegalArgumentException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/salidas";
    }
}
