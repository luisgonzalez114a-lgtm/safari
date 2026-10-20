package com.example.productos.controller;

import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
public class UsuarioWebController {
    private static final int USUARIOS_POR_PAGINA = 9;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UsuarioWebController(UsuarioRepository usuarioRepository,
                                RolRepository rolRepository,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/usuarios")
    public String usuarios(@RequestParam(defaultValue = "0") int page, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!esAdmin(usuario)) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogueado", usuario);
        Page<Usuario> usuariosPagina = usuarioRepository.findAll(
                PageRequest.of(Math.max(page, 0), USUARIOS_POR_PAGINA, Sort.by("nombre").ascending()));
        if (usuariosPagina.getTotalPages() > 0 && usuariosPagina.isEmpty()) {
            usuariosPagina = usuarioRepository.findAll(PageRequest.of(
                    usuariosPagina.getTotalPages() - 1, USUARIOS_POR_PAGINA, Sort.by("nombre").ascending()));
        }
        model.addAttribute("usuarios", usuarioRepository.findAll(Sort.by("nombre").ascending()));
        model.addAttribute("usuariosPagina", usuariosPagina);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("usuarioForm", new Usuario());
        return "usuarios";
    }

    @PostMapping("/usuarios/guardar")
    @Transactional
    public String guardarUsuario(@RequestParam String nombre,
                                 @RequestParam String correo,
                                 @RequestParam String clave,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) List<Integer> rolesIds,
                                 HttpSession session,
                                 RedirectAttributes attributes) {
        if (!sesionEsAdmin(session)) {
            return "redirect:/login";
        }
        if (!datosUsuarioValidos(nombre, correo, clave, rolesIds)) {
            attributes.addFlashAttribute("error", "Completa nombre, correo, contraseña y selecciona al menos un rol válido.");
            return "redirect:/usuarios";
        }
        if (usuarioRepository.findByCorreo(correo.trim()).isPresent()) {
            attributes.addFlashAttribute("error", "Ya existe un usuario con ese correo.");
            return "redirect:/usuarios";
        }
        Set<Rol> roles = buscarRoles(rolesIds);
        if (roles == null || roles.isEmpty()) {
            attributes.addFlashAttribute("error", "Selecciona al menos un rol existente.");
            return "redirect:/usuarios";
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre.trim());
        usuario.setCorreo(correo.trim());
        usuario.setClave(passwordEncoder.encode(clave.trim()));
        usuario.setTelefono(limpiar(telefono));
        usuario.setEstatus(1);
        usuario.setRoles(roles);
        usuarioRepository.save(usuario);
        auditService.registrar(usuarioDeSesion(session).getId(), "CREAR_USUARIO", "usuarios", usuario.getId());
        attributes.addFlashAttribute("mensaje", "Usuario creado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/actualizar")
    @Transactional
    public String actualizarUsuario(@RequestParam Integer usuarioId,
                                    @RequestParam String nombre,
                                    @RequestParam String correo,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String clave,
                                    @RequestParam(required = false) String claveActual,
                                    @RequestParam(required = false) List<Integer> rolesIds,
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
        Set<Rol> roles = buscarRoles(rolesIds);
        if (actual == null || roles == null || roles.isEmpty()) {
            attributes.addFlashAttribute("error", "Selecciona al menos un rol existente para el usuario.");
            return "redirect:/usuarios";
        }
        if (clave != null && !clave.isBlank()
                && (claveActual == null || claveActual.isBlank()
                || actual.getClave() == null
                || !passwordEncoder.matches(claveActual.trim(), actual.getClave()))) {
            attributes.addFlashAttribute("error",
                    "Para cambiar la contraseña, confirma correctamente la contraseña actual del usuario.");
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
        actual.setRoles(roles);
        usuarioRepository.save(actual);
        auditService.registrar(admin.getId(), "EDITAR_USUARIO", "usuarios", actual.getId());
        if (actual.getId().equals(admin.getId())) {
            session.setAttribute("usuarioLogueado", actual);
        }
        attributes.addFlashAttribute("mensaje", "Usuario actualizado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar")
    @Transactional
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
            auditService.registrar(admin.getId(), "ELIMINAR_USUARIO", "usuarios", usuarioId);
            attributes.addFlashAttribute("mensaje", "Usuario eliminado.");
        } catch (DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("error", "No se puede eliminar: el usuario ya tiene movimientos registrados.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/estado")
    @Transactional
    public String cambiarEstadoUsuario(@RequestParam Integer usuarioId,
                                       HttpSession session,
                                       RedirectAttributes attributes) {
        Usuario admin = usuarioDeSesion(session);
        if (admin == null || !esAdmin(admin)) {
            return "redirect:/login";
        }
        if (admin.getId().equals(usuarioId)) {
            attributes.addFlashAttribute("error", "No puedes desactivar la cuenta con la que tienes la sesión abierta.");
            return "redirect:/usuarios";
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            attributes.addFlashAttribute("error", "No se encontró el usuario que intentas actualizar.");
            return "redirect:/usuarios";
        }
        boolean activar = usuario.getEstatus() == null || usuario.getEstatus() != 1;
        usuario.setEstatus(activar ? 1 : 0);
        usuarioRepository.save(usuario);
        auditService.registrar(admin.getId(), activar ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "usuarios", usuario.getId());
        attributes.addFlashAttribute("mensaje", activar ? "Usuario activado." : "Usuario desactivado.");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/rol")
    @Transactional
    public String actualizarRolUsuario(@RequestParam Integer usuarioId,
                                       @RequestParam(required = false) List<Integer> rolesIds,
                                       HttpSession session,
                                       RedirectAttributes attributes) {
        if (!sesionEsAdmin(session)) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        Set<Rol> roles = buscarRoles(rolesIds);
        if (usuario == null || roles == null || roles.isEmpty()) {
            attributes.addFlashAttribute("error", "Debes seleccionar un usuario y al menos un rol válido.");
            return "redirect:/usuarios";
        }
        usuario.setRoles(roles);
        usuarioRepository.save(usuario);
        auditService.registrar(usuarioDeSesion(session).getId(), "ROLES_USUARIO_ACTUALIZADOS",
                "usuario_roles", usuario.getId());
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
    @Transactional
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
        auditService.registrar(usuarioDeSesion(session).getId(), "CREAR_ROL", "roles", rol.getId());
        attributes.addFlashAttribute("mensaje", "Rol creado.");
        return "redirect:/roles";
    }

    private Usuario usuarioDeSesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private boolean sesionEsAdmin(HttpSession session) {
        return esAdmin(usuarioDeSesion(session));
    }

    private boolean datosUsuarioValidos(String nombre, String correo, String clave, List<Integer> rolesIds) {
        return nombre != null && !nombre.isBlank()
                && correo != null && !correo.isBlank()
                && clave != null && !clave.isBlank()
                && rolesIds != null && !rolesIds.isEmpty()
                && buscarRoles(rolesIds) != null;
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.tieneRol("ADMIN");
    }

    private Set<Rol> buscarRoles(List<Integer> rolesIds) {
        if (rolesIds == null || rolesIds.isEmpty() || rolesIds.stream().anyMatch(id -> id == null || id <= 0)) {
            return null;
        }
        List<Integer> idsUnicos = rolesIds.stream().distinct().toList();
        List<Rol> rolesEncontrados = rolRepository.findAllById(idsUnicos);
        if (rolesEncontrados.size() != idsUnicos.size()) {
            return null;
        }
        return new LinkedHashSet<>(rolesEncontrados);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
