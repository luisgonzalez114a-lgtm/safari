package com.example.productos.controller;

import com.example.productos.model.Producto;
import com.example.productos.model.Usuario;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.service.AuditService;
import com.example.productos.service.DashboardService;
import com.example.productos.service.RbacService;
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
    private final RbacService rbacService;
    private final DashboardService dashboardService;
    private final AuditService auditService;

    public ProductoWebController(ProductoService productoService,
                                 CategoriaRepository categoriaRepository,
                                 MarcaRepository marcaRepository,
                                 RbacService rbacService,
                                 DashboardService dashboardService,
                                 AuditService auditService) {
        this.productoService = productoService;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.rbacService = rbacService;
        this.dashboardService = dashboardService;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String listar(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("esAdmin", esAdmin(usuario));
        model.addAttribute("esVendedor", esVendedor(usuario));
        model.addAttribute("esCliente", usuario == null);
        model.addAttribute("dashboard", dashboardService.obtenerResumenDelMes());
        model.addAttribute("puedeCrearProducto", puede(usuario, "/inventario", RbacService.CREAR));
        model.addAttribute("puedeEditarProducto", puede(usuario, "/inventario", RbacService.EDITAR));
        model.addAttribute("puedeEliminarProducto", puede(usuario, "/inventario", RbacService.ELIMINAR));
        model.addAttribute("puedeGestionarProducto",
                puede(usuario, "/inventario", RbacService.CREAR)
                        || puede(usuario, "/inventario", RbacService.EDITAR)
                        || puede(usuario, "/inventario", RbacService.ELIMINAR));
        model.addAttribute("productos", productoService.listar());
        if (puede(usuario, "/inventario", RbacService.CREAR)
                || puede(usuario, "/inventario", RbacService.EDITAR)) {
            model.addAttribute("producto", new Producto());
            agregarCatalogos(model);
        }
        return "index";
    }

    @PostMapping("/productos/guardar")
    public String guardar(@ModelAttribute Producto producto, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!datosProductoValidos(producto)) {
            redirectAttributes.addFlashAttribute("error", "Verifica el código, nombre, precios y cantidades del producto.");
            return "redirect:/";
        }
        if (productoService.codigoEnUso(producto.getCodigo(), producto.getId())) {
            redirectAttributes.addFlashAttribute("error", "Ese código ya pertenece a otro producto.");
            return "redirect:/";
        }

        Usuario actor = (Usuario) session.getAttribute("usuarioLogueado");
        if (producto.getId() == null) {
            Producto creado = productoService.crear(producto);
            auditService.registrar(actor.getId(), "CREAR_PRODUCTO", "productos", creado.getId());
        } else {
            Producto actualizado = productoService.actualizar(producto.getId(), producto);
            if (actualizado != null) {
                auditService.registrar(actor.getId(), "EDITAR_PRODUCTO", "productos", actualizado.getId());
            }
        }
        return "redirect:/";
    }

    @GetMapping("/productos/editar/{id}")
    public String editar(@PathVariable Integer id, Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("esAdmin", esAdmin(usuario));
        model.addAttribute("esVendedor", esVendedor(usuario));
        model.addAttribute("esCliente", false);
        model.addAttribute("dashboard", dashboardService.obtenerResumenDelMes());
        model.addAttribute("puedeCrearProducto", puede(usuario, "/inventario", RbacService.CREAR));
        model.addAttribute("puedeEditarProducto", puede(usuario, "/inventario", RbacService.EDITAR));
        model.addAttribute("puedeEliminarProducto", puede(usuario, "/inventario", RbacService.ELIMINAR));
        model.addAttribute("puedeGestionarProducto", true);
        var producto = productoService.listar().stream().filter(p -> p.getId().equals(id))
                .findFirst().orElse(new Producto());
        model.addAttribute("producto", producto);
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
        try {
            if (productoService.eliminar(id)) {
                Usuario actor = (Usuario) session.getAttribute("usuarioLogueado");
                auditService.registrar(actor.getId(), "ELIMINAR_PRODUCTO", "productos", id);
            }
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error", "No se puede eliminar: el producto tiene movimientos registrados.");
        }
        return "redirect:/";
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.tieneRol("ADMIN");
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
        return usuario != null && usuario.tieneRol("VENDEDOR");
    }

    private boolean puede(Usuario usuario, String vista, String accion) {
        return usuario != null && rbacService.usuarioTienePermiso(usuario.getId(), vista, accion);
    }
}
