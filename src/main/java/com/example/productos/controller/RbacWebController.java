package com.example.productos.controller;

import com.example.productos.model.Rol;
import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.service.RbacService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
public class RbacWebController {
    private final RbacService rbacService;
    private final RolRepository rolRepository;

    public RbacWebController(RbacService rbacService, RolRepository rolRepository) {
        this.rbacService = rbacService;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/roles/permisos")
    public String permisos(@RequestParam(required = false) Integer rolId,
                           HttpSession session, Model model) {
        Usuario usuario = usuarioAdmin(session);
        if (usuario == null) {
            return "redirect:/";
        }
        List<Rol> roles = rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Rol seleccionado = roles.stream().filter(rol -> rol.getId().equals(rolId))
                .findFirst()
                .orElseGet(() -> roles.stream()
                        .filter(rol -> !rol.getNombre().equalsIgnoreCase("ADMIN"))
                        .findFirst().orElse(roles.isEmpty() ? null : roles.get(0)));

        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("roles", roles);
        model.addAttribute("rolSeleccionado", seleccionado);
        model.addAttribute("modulos", rbacService.listarModulos());
        model.addAttribute("vistas", rbacService.listarVistas());
        model.addAttribute("permisos", rbacService.listarPermisos());
        model.addAttribute("clavesAsignadas",
                seleccionado == null ? java.util.Set.of() : rbacService.clavesAsignadas(seleccionado.getId()));
        return "permisos";
    }

    @PostMapping("/roles/permisos/guardar")
    @Transactional
    public String guardarPermisos(@RequestParam Integer rolId,
                                  @RequestParam(required = false) List<String> claves,
                                  HttpSession session,
                                  RedirectAttributes attributes) {
        Usuario usuario = usuarioAdmin(session);
        if (usuario == null) {
            return "redirect:/";
        }
        try {
            rbacService.guardarAsignaciones(rolId, claves, usuario.getId());
            attributes.addFlashAttribute("mensaje", "Permisos del rol actualizados.");
        } catch (IllegalArgumentException exception) {
            attributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/roles/permisos?rolId=" + rolId;
    }

    private Usuario usuarioAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null && usuario.tieneRol("ADMIN") ? usuario : null;
    }
}
