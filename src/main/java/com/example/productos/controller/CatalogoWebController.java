package com.example.productos.controller;

import com.example.productos.model.Categoria;
import com.example.productos.model.Marca;
import com.example.productos.model.Proveedor;
import com.example.productos.model.Usuario;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.service.ProductoRepository;
import com.example.productos.service.AuditService;
import com.example.productos.service.RbacService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogoWebController {
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final EntradaRepository entradaRepository;
    private final RbacService rbacService;
    private final AuditService auditService;

    public CatalogoWebController(CategoriaRepository categoriaRepository,
                                 MarcaRepository marcaRepository,
    ProveedorRepository proveedorRepository,
    ProductoRepository productoRepository,
    EntradaRepository entradaRepository,
    RbacService rbacService,
    AuditService auditService) {
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
        this.rbacService = rbacService;
        this.auditService = auditService;
    }

    @GetMapping("/catalogos")
    public String catalogos() {
        return "redirect:/catalogos/proveedores";
    }

    @GetMapping("/catalogos/proveedores")
    public String proveedores(HttpSession session, Model model) {
        agregarPermisosVista(session, model, "/catalogos/proveedores");
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "proveedores";
    }

    @GetMapping("/catalogos/marcas")
    public String marcas(HttpSession session, Model model) {
        agregarPermisosVista(session, model, "/catalogos/marcas");
        model.addAttribute("marcas", marcaRepository.findAll());
        return "marcas";
    }

    @GetMapping("/catalogos/categorias")
    public String categorias(HttpSession session, Model model) {
        agregarPermisosVista(session, model, "/catalogos/categorias");
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "categorias";
    }

    @PostMapping("/catalogos/categorias")
    @Transactional
    public String crearCategoria(@RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return "redirect:/catalogos/categorias";
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(limpiar(descripcion));
        categoriaRepository.save(categoria);
        auditService.registrar(actorId(session), "CREAR_CATEGORIA", "categorias", categoria.getId());
        attributes.addFlashAttribute("mensaje", "Categoría guardada.");
        return "redirect:/catalogos/categorias";
    }

    @PostMapping("/catalogos/marcas")
    @Transactional
    public String crearMarca(@RequestParam String nombre, HttpSession session, RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la marca es obligatorio.");
            return "redirect:/catalogos/marcas";
        }
        Marca marca = new Marca();
        marca.setNombre(nombre.trim());
        marcaRepository.save(marca);
        auditService.registrar(actorId(session), "CREAR_MARCA", "marcas", marca.getId());
        attributes.addFlashAttribute("mensaje", "Marca guardada.");
        return "redirect:/catalogos/marcas";
    }

    @PostMapping("/catalogos/proveedores")
    @Transactional
    public String crearProveedor(@RequestParam String nombre,
                                 @RequestParam(required = false) String rifNit,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) String correo,
                                 @RequestParam(required = false) String direccion,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/catalogos/proveedores";
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre.trim());
        proveedor.setRifNit(limpiar(rifNit));
        proveedor.setTelefono(limpiar(telefono));
        proveedor.setCorreo(limpiar(correo));
        proveedor.setDireccion(limpiar(direccion));
        proveedorRepository.save(proveedor);
        auditService.registrar(actorId(session), "CREAR_PROVEEDOR", "proveedores", proveedor.getId());
        attributes.addFlashAttribute("mensaje", "Proveedor guardado.");
        return "redirect:/catalogos/proveedores";
    }

    @PostMapping("/catalogos/categorias/actualizar")
    @Transactional
    public String actualizarCategoria(@RequestParam int id,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) String descripcion,
                                      HttpSession session,
                                      RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return "redirect:/catalogos/categorias";
        }
        Categoria categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null) {
            attributes.addFlashAttribute("error", "No se encontró la categoría que intentas editar.");
            return "redirect:/catalogos/categorias";
        }
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(limpiar(descripcion));
        categoriaRepository.save(categoria);
        auditService.registrar(actorId(session), "EDITAR_CATEGORIA", "categorias", categoria.getId());
        attributes.addFlashAttribute("mensaje", "Categoría actualizada.");
        return "redirect:/catalogos/categorias";
    }

    @PostMapping("/catalogos/marcas/actualizar")
    @Transactional
    public String actualizarMarca(@RequestParam int id,
                                  @RequestParam String nombre,
                                  HttpSession session,
                                  RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la marca es obligatorio.");
            return "redirect:/catalogos/marcas";
        }
        Marca marca = marcaRepository.findById(id).orElse(null);
        if (marca == null) {
            attributes.addFlashAttribute("error", "No se encontró la marca que intentas editar.");
            return "redirect:/catalogos/marcas";
        }
        marca.setNombre(nombre.trim());
        marcaRepository.save(marca);
        auditService.registrar(actorId(session), "EDITAR_MARCA", "marcas", marca.getId());
        attributes.addFlashAttribute("mensaje", "Marca actualizada.");
        return "redirect:/catalogos/marcas";
    }

    @PostMapping("/catalogos/proveedores/actualizar")
    @Transactional
    public String actualizarProveedor(@RequestParam int id,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) String rifNit,
                                      @RequestParam(required = false) String telefono,
                                      @RequestParam(required = false) String correo,
                                      @RequestParam(required = false) String direccion,
                                      HttpSession session,
                                      RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/catalogos/proveedores";
        }
        Proveedor proveedor = proveedorRepository.findById(id).orElse(null);
        if (proveedor == null) {
            attributes.addFlashAttribute("error", "No se encontró el proveedor que intentas editar.");
            return "redirect:/catalogos/proveedores";
        }
        proveedor.setNombre(nombre.trim());
        proveedor.setRifNit(limpiar(rifNit));
        proveedor.setTelefono(limpiar(telefono));
        proveedor.setCorreo(limpiar(correo));
        proveedor.setDireccion(limpiar(direccion));
        proveedorRepository.save(proveedor);
        auditService.registrar(actorId(session), "EDITAR_PROVEEDOR", "proveedores", proveedor.getId());
        attributes.addFlashAttribute("mensaje", "Proveedor actualizado.");
        return "redirect:/catalogos/proveedores";
    }

    @PostMapping("/catalogos/categorias/eliminar")
    @Transactional
    public String eliminarCategoria(@RequestParam int id, HttpSession session, RedirectAttributes attributes) {
        if (productoRepository.existsByIdCategoria(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar la categoría porque está asignada a productos.");
            return "redirect:/catalogos/categorias";
        }
        return eliminarCatalogo(categoriaRepository, id, "categoría", "categorias",
                "/catalogos/categorias", session, attributes);
    }

    @PostMapping("/catalogos/marcas/eliminar")
    @Transactional
    public String eliminarMarca(@RequestParam int id, HttpSession session, RedirectAttributes attributes) {
        if (productoRepository.existsByIdMarca(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar la marca porque está asignada a productos.");
            return "redirect:/catalogos/marcas";
        }
        return eliminarCatalogo(marcaRepository, id, "marca", "marcas", "/catalogos/marcas", session, attributes);
    }

    @PostMapping("/catalogos/proveedores/eliminar")
    @Transactional
    public String eliminarProveedor(@RequestParam int id, HttpSession session, RedirectAttributes attributes) {
        if (entradaRepository.existsByProveedor_Id(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar el proveedor porque tiene entradas registradas.");
            return "redirect:/catalogos/proveedores";
        }
        return eliminarCatalogo(proveedorRepository, id, "proveedor", "proveedores",
                "/catalogos/proveedores", session, attributes);
    }

    private <T> String eliminarCatalogo(org.springframework.data.jpa.repository.JpaRepository<T, Integer> repository,
                                        int id,
                                        String tipo,
                                        String tabla,
                                        String returnPath,
                                        HttpSession session,
                                        RedirectAttributes attributes) {
        if (!repository.existsById(id)) {
            attributes.addFlashAttribute("error", "No se encontró el " + tipo + " que intentas eliminar.");
            return "redirect:" + returnPath;
        }
        try {
            repository.deleteById(id);
            repository.flush();
            auditService.registrar(actorId(session), "ELIMINAR_" + tabla.toUpperCase(java.util.Locale.ROOT),
                    tabla, id);
            attributes.addFlashAttribute("mensaje", "Se eliminó el " + tipo + ".");
        } catch (DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error",
                    "No se puede eliminar el " + tipo + " porque está relacionado con productos o movimientos.");
        }
        return "redirect:" + returnPath;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void agregarPermisosVista(HttpSession session, Model model, String ruta) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("puedeCrear", rbacService.usuarioTienePermiso(
                usuario.getId(), ruta, RbacService.CREAR));
        model.addAttribute("puedeEditar", rbacService.usuarioTienePermiso(
                usuario.getId(), ruta, RbacService.EDITAR));
        model.addAttribute("puedeEliminar", rbacService.usuarioTienePermiso(
                usuario.getId(), ruta, RbacService.ELIMINAR));
    }

    private Integer actorId(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario == null ? null : usuario.getId();
    }
}
