package com.example.productos.controller;

import com.example.productos.model.Producto;
import com.example.productos.model.Usuario;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductoWebController {
    private final ProductoService productoService;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public ProductoWebController(ProductoService productoService,
                                 CategoriaRepository categoriaRepository,
                                 MarcaRepository marcaRepository) {
        this.productoService = productoService;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

    @GetMapping("/")
    public String listar(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("esAdmin", esAdmin(usuario));
        model.addAttribute("esVendedor", esVendedor(usuario));
        model.addAttribute("esCliente", !esAdmin(usuario) && !esVendedor(usuario));
        model.addAttribute("productos", productoService.listar());
        if (esAdmin(usuario)) {
            model.addAttribute("producto", new Producto());
            agregarCatalogos(model);
        }
        return "index";
    }

    @PostMapping("/productos/guardar")
    public String guardar(@ModelAttribute Producto producto, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!hayAccesoAdmin(session)) {
            return "redirect:/";
        }

        if (!datosProductoValidos(producto)) {
            redirectAttributes.addFlashAttribute("error", "Verifica el código, nombre, precios y cantidades del producto.");
            return "redirect:/";
        }
        if (productoService.codigoEnUso(producto.getCodigo(), producto.getId())) {
            redirectAttributes.addFlashAttribute("error", "Ese código ya pertenece a otro producto.");
            return "redirect:/";
        }

        if (producto.getId() == null) {
            productoService.crear(producto);
        } else {
            productoService.actualizar(producto.getId(), producto);
        }
        return "redirect:/";
    }

    @GetMapping("/productos/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, HttpSession session) {
        if (!hayAccesoAdmin(session)) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogueado", session.getAttribute("usuarioLogueado"));
        model.addAttribute("esAdmin", true);
        model.addAttribute("esVendedor", false);
        model.addAttribute("esCliente", false);
        model.addAttribute("producto", productoService.listar().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(new Producto()));
        model.addAttribute("productos", productoService.listar());
        agregarCatalogos(model);
        return "index";
    }

    private void agregarCatalogos(Model model) {
        model.addAttribute("marcas", marcaRepository.findAll());
        model.addAttribute("categorias", categoriaRepository.findAll());
    }

    @PostMapping("/productos/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes attributes) {
        if (!hayAccesoAdmin(session)) {
            return "redirect:/";
        }
        try {
            productoService.eliminar(id);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error", "No se puede eliminar: el producto tiene movimientos registrados.");
        }
        return "redirect:/";
    }

    private boolean hayAccesoAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null && usuarioPuedeEditar(usuario);
    }

    private boolean esAdmin(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || usuario.getRol().getNombre() == null) {
            return false;
        }
        return usuario.getRol().getNombre().equalsIgnoreCase("ADMIN") ||
                usuario.getRol().getNombre().equalsIgnoreCase("admin");
    }

    private boolean datosProductoValidos(Producto producto) {
        if (producto == null) {
            return false;
        }
        if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
            return false;
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            return false;
        }
        if (producto.getPrecioCompra() == null || producto.getPrecioCompra().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (producto.getPrecioActual() == null || producto.getPrecioActual().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (producto.getStockActual() == null || producto.getStockActual() < 0) {
            return false;
        }
        if (producto.getStockMinimo() == null || producto.getStockMinimo() < 0) {
            return false;
        }
        return true;
    }

    public boolean usuarioPuedeEditar(Usuario usuario) {
        return esAdmin(usuario);
    }

    private boolean esVendedor(Usuario usuario) {
        return usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null
                && usuario.getRol().getNombre().equalsIgnoreCase("VENDEDOR");
    }
}
