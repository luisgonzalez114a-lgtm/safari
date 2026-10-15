package com.example.productos.controller;

import com.example.productos.model.Categoria;
import com.example.productos.model.Marca;
import com.example.productos.model.Proveedor;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.repository.EntradaRepository;
import com.example.productos.service.ProductoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
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

    public CatalogoWebController(CategoriaRepository categoriaRepository,
                                 MarcaRepository marcaRepository,
    ProveedorRepository proveedorRepository,
    ProductoRepository productoRepository,
    EntradaRepository entradaRepository) {
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
    }

    @GetMapping("/catalogos")
    public String catalogos(HttpSession session, Model model) {
        model.addAttribute("usuarioLogueado", session.getAttribute("usuarioLogueado"));
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());
        model.addAttribute("proveedores", proveedorRepository.findAll());
        return "catalogos";
    }

    @PostMapping("/catalogos/categorias")
    public String crearCategoria(@RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return "redirect:/catalogos";
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(limpiar(descripcion));
        categoriaRepository.save(categoria);
        attributes.addFlashAttribute("mensaje", "Categoría guardada.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/marcas")
    public String crearMarca(@RequestParam String nombre, RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la marca es obligatorio.");
            return "redirect:/catalogos";
        }
        Marca marca = new Marca();
        marca.setNombre(nombre.trim());
        marcaRepository.save(marca);
        attributes.addFlashAttribute("mensaje", "Marca guardada.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/proveedores")
    public String crearProveedor(@RequestParam String nombre,
                                 @RequestParam(required = false) String rifNit,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) String correo,
                                 @RequestParam(required = false) String direccion,
                                 RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/catalogos";
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre.trim());
        proveedor.setRifNit(limpiar(rifNit));
        proveedor.setTelefono(limpiar(telefono));
        proveedor.setCorreo(limpiar(correo));
        proveedor.setDireccion(limpiar(direccion));
        proveedorRepository.save(proveedor);
        attributes.addFlashAttribute("mensaje", "Proveedor guardado.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/categorias/actualizar")
    public String actualizarCategoria(@RequestParam int id,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) String descripcion,
                                      RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la categoría es obligatorio.");
            return "redirect:/catalogos";
        }
        Categoria categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null) {
            attributes.addFlashAttribute("error", "No se encontró la categoría que intentas editar.");
            return "redirect:/catalogos";
        }
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(limpiar(descripcion));
        categoriaRepository.save(categoria);
        attributes.addFlashAttribute("mensaje", "Categoría actualizada.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/marcas/actualizar")
    public String actualizarMarca(@RequestParam int id,
                                  @RequestParam String nombre,
                                  RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre de la marca es obligatorio.");
            return "redirect:/catalogos";
        }
        Marca marca = marcaRepository.findById(id).orElse(null);
        if (marca == null) {
            attributes.addFlashAttribute("error", "No se encontró la marca que intentas editar.");
            return "redirect:/catalogos";
        }
        marca.setNombre(nombre.trim());
        marcaRepository.save(marca);
        attributes.addFlashAttribute("mensaje", "Marca actualizada.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/proveedores/actualizar")
    public String actualizarProveedor(@RequestParam int id,
                                      @RequestParam String nombre,
                                      @RequestParam(required = false) String rifNit,
                                      @RequestParam(required = false) String telefono,
                                      @RequestParam(required = false) String correo,
                                      @RequestParam(required = false) String direccion,
                                      RedirectAttributes attributes) {
        if (nombre == null || nombre.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/catalogos";
        }
        Proveedor proveedor = proveedorRepository.findById(id).orElse(null);
        if (proveedor == null) {
            attributes.addFlashAttribute("error", "No se encontró el proveedor que intentas editar.");
            return "redirect:/catalogos";
        }
        proveedor.setNombre(nombre.trim());
        proveedor.setRifNit(limpiar(rifNit));
        proveedor.setTelefono(limpiar(telefono));
        proveedor.setCorreo(limpiar(correo));
        proveedor.setDireccion(limpiar(direccion));
        proveedorRepository.save(proveedor);
        attributes.addFlashAttribute("mensaje", "Proveedor actualizado.");
        return "redirect:/catalogos";
    }

    @PostMapping("/catalogos/categorias/eliminar")
    public String eliminarCategoria(@RequestParam int id, RedirectAttributes attributes) {
        if (productoRepository.existsByIdCategoria(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar la categoría porque está asignada a productos.");
            return "redirect:/catalogos";
        }
        return eliminarCatalogo(categoriaRepository, id, "categoría", attributes);
    }

    @PostMapping("/catalogos/marcas/eliminar")
    public String eliminarMarca(@RequestParam int id, RedirectAttributes attributes) {
        if (productoRepository.existsByIdMarca(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar la marca porque está asignada a productos.");
            return "redirect:/catalogos";
        }
        return eliminarCatalogo(marcaRepository, id, "marca", attributes);
    }

    @PostMapping("/catalogos/proveedores/eliminar")
    public String eliminarProveedor(@RequestParam int id, RedirectAttributes attributes) {
        if (entradaRepository.existsByProveedor_Id(id)) {
            attributes.addFlashAttribute("error", "No se puede eliminar el proveedor porque tiene entradas registradas.");
            return "redirect:/catalogos";
        }
        return eliminarCatalogo(proveedorRepository, id, "proveedor", attributes);
    }

    private <T> String eliminarCatalogo(org.springframework.data.jpa.repository.JpaRepository<T, Integer> repository,
                                        int id,
                                        String tipo,
                                        RedirectAttributes attributes) {
        if (!repository.existsById(id)) {
            attributes.addFlashAttribute("error", "No se encontró el " + tipo + " que intentas eliminar.");
            return "redirect:/catalogos";
        }
        try {
            repository.deleteById(id);
            repository.flush();
            attributes.addFlashAttribute("mensaje", "Se eliminó el " + tipo + ".");
        } catch (DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error",
                    "No se puede eliminar el " + tipo + " porque está relacionado con productos o movimientos.");
        }
        return "redirect:/catalogos";
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
