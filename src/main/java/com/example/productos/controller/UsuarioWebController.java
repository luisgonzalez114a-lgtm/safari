package com.example.productos.controller;

import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuarioWebController {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioWebController(UsuarioRepository usuarioRepository,
                                RolRepository rolRepository,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/usuarios")
    public String usuarios(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!esAdmin(usuario)) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("usuarioForm", new Usuario());
        return "usuarios";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@RequestParam String nombre,
                                 @RequestParam String correo,
                                 @RequestParam String clave,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) Integer rolId,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        if (!sesionEsAdmin(session)) {
            return "redirect:/login";
        }
        if (!datosUsuarioValidos(nombre, correo, clave, rolId)) {
            attributes.addFlashAttribute("error", "Completa nombre, correo, contraseña y selecciona un rol.");
            return "redirect:/usuarios";
        }
        if (usuarioRepository.findByCorreo(correo.trim()).isPresent()) {
            attributes.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }
        Rol rol = rolRepository.findById(rolId).orElse(null);
        if (rol == null) {
            attributes.addFlashAttribute("error", "Selecciona un rol existente.");
            return "redirect:/usuarios";
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setCorreo(correo.trim());
        usuario.setClave(passwordEncoder.encode(clave.trim()));
        usuario.setTelefono(limpiar(telefono));
        usuario.setEstatus(1);
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        attributes.addFlashAttribute("mensaje", "Usuario creado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/actualizar")
    public String actualizarUsuario(@RequestParam Integer usuarioId,
                                    @RequestParam String nombre,
                                    @RequestParam String correo,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String clave,
                                    @RequestParam Integer rolId,
                                    HttpSession session,
                                    RedirectAttributes attributes) {
        Usuario admin = usuarioDeSesion(session);
        if (admin == null || !esAdmin(admin)) {
            return "redirect:/login";
        }
        if (nombre.isBlank() || correo.isBlank()) {
            attributes.addFlashAttribute("error", "El nombre y correo son obligatorios.");
            return "redirect:/usuarios";
        }
        Usuario actual = usuarioRepository.findById(usuarioId).orElse(null);
        Rol rol = rolRepository.findById(rolId).orElse(null);
        if (actual == null || rol == null) {
            attributes.addFlashAttribute("error", "No se encontró el usuario o rol seleccionado.");
            return "redirect:/usuarios";
        }
        if (usuarioRepository.findByCorreo(correo.trim()).filter(u -> !u.getId().equals(usuarioId)).isPresent()) {
            attributes.addFlashAttribute("error", "Ese correo ya está asignado a otro usuario.");
            return "redirect:/usuarios";
        }
        actual.setNombre(nombre.trim());
        actual.setCorreo(correo.trim());
        actual.setTelefono(limpiar(telefono));
        if (clave != null && !clave.isBlank()) {
            actual.setClave(passwordEncoder.encode(clave.trim()));
        }
        actual.setRol(rol);
        usuarioRepository.save(actual);
        if (actual.getId().equals(admin.getId())) {
            session.setAttribute("usuarioLogueado", actual);
        }
        attributes.addFlashAttribute("mensaje", "Usuario actualizado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar")
    public String eliminarUsuario(@RequestParam Integer usuarioId,
                                  HttpSession session,
                                  RedirectAttributes attributes) {
        Usuario admin = usuarioDeSesion(session);
        if (admin == null || !esAdmin(admin)) {
            return "redirect:/login";
        }
        if (admin.getId().equals(usuarioId)) {
            attributes.addFlashAttribute("error", "No puedes eliminar la cuenta con la que tienes la sesión abierta.");
            return "redirect:/usuarios";
        }
        try {
            usuarioRepository.deleteById(usuarioId);
            attributes.addFlashAttribute("mensaje", "Usuario eliminado.");
        } catch (DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error", "No se puede eliminar: el usuario ya tiene movimientos registrados.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/rol")
    public String actualizarRolUsuario(@RequestParam Integer usuarioId,
                                       @RequestParam Integer rolId,
                                       HttpSession session,
                                       RedirectAttributes attributes) {
        if (!sesionEsAdmin(session)) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Rol rol = rolRepository.findById(rolId).orElse(null);
        if (usuario == null || rol == null) {
            attributes.addFlashAttribute("error", "Debes seleccionar un usuario y un rol válidos.");
            return "redirect:/usuarios";
        }
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        attributes.addFlashAttribute("mensaje", "Rol actualizado.");
        return "redirect:/usuarios";
    }

    @GetMapping("/roles")
    public String roles(HttpSession session, Model model) {
        Usuario usuario = usuarioDeSesion(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!esAdmin(usuario)) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("rolForm", new Rol());
        return "roles";
    }

    @PostMapping("/roles/guardar")
    public String guardarRol(@RequestParam String nombre,
                             HttpSession session,
                             RedirectAttributes attributes) {
        if (!sesionEsAdmin(session)) {
            return "redirect:/login";
        }
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 50) {
            attributes.addFlashAttribute("error", "El nombre del rol es obligatorio y debe tener 50 caracteres o menos.");
            return "redirect:/roles";
        }
        if (rolRepository.findAll().stream().anyMatch(r -> r.getNombre().equalsIgnoreCase(nombre.trim()))) {
            attributes.addFlashAttribute("error", "Ya existe un rol con ese nombre.");
            return "redirect:/roles";
        }
        Rol rol = new Rol();
        rol.setNombre(nombre.trim());
        rolRepository.save(rol);
        attributes.addFlashAttribute("mensaje", "Rol creado.");
        return "redirect:/roles";
    }

    private Usuario usuarioDeSesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private boolean sesionEsAdmin(HttpSession session) {
        return esAdmin(usuarioDeSesion(session));
    }

    private boolean datosUsuarioValidos(String nombre, String correo, String clave, Integer rolId) {
        return nombre != null && !nombre.isBlank()
                && correo != null && !correo.isBlank()
                && clave != null && !clave.isBlank()
                && rolId != null && rolId > 0;
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null
                && usuario.getRol().getNombre().equalsIgnoreCase("ADMIN");
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
