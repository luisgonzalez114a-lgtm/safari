package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Controller
public class AuthController {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthController(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    @GetMapping("/registro")
    public String registroForm(HttpSession session) {
        return session.getAttribute("usuarioLogueado") == null ? "registro" : "redirect:/";
    }

    @PostMapping("/registro")
    @Transactional
    public String registrarCliente(@RequestParam String nombre,
                                   @RequestParam String correo,
                                   @RequestParam(required = false) String telefono,
                                   @RequestParam String clave,
                                   HttpSession session,
                                   RedirectAttributes attributes) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }

        String nombreNormalizado = nombre == null ? "" : nombre.trim();
        String correoNormalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        String telefonoNormalizado = telefono == null || telefono.isBlank() ? null : telefono.trim();
        boolean correoValido = correoNormalizado.length() <= 100
                && correoNormalizado.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
        boolean claveValida = clave != null && clave.getBytes(StandardCharsets.UTF_8).length >= 8
                && clave.getBytes(StandardCharsets.UTF_8).length <= 72;
        if (nombreNormalizado.isBlank() || nombreNormalizado.length() > 100
                || !correoValido || !claveValida
                || (telefonoNormalizado != null && telefonoNormalizado.length() > 20)) {
            attributes.addFlashAttribute("error",
                    "Revisa los datos: nombre obligatorio, correo válido y contraseña de 8 a 72 bytes.");
            return "redirect:/registro";
        }
        if (usuarioRepository.findByCorreoIgnoreCase(correoNormalizado).isPresent()) {
            attributes.addFlashAttribute("error", "Ya existe una cuenta con ese correo.");
            return "redirect:/registro";
        }

        var rolCliente = rolRepository.findByNombreIgnoreCase("CLIENTE")
                .orElseThrow(() -> new IllegalStateException("No está configurado el rol CLIENTE."));
        Usuario cliente = new Usuario();
        cliente.setNombre(nombreNormalizado);
        cliente.setCorreo(correoNormalizado);
        cliente.setTelefono(telefonoNormalizado);
        cliente.setClave(passwordEncoder.encode(clave));
        cliente.setRoles(java.util.Set.of(rolCliente));
        cliente.setEstatus(1);
        usuarioRepository.save(cliente);
        auditService.registrar(null, "REGISTRO_CLIENTE", "usuarios", cliente.getId());
        attributes.addFlashAttribute("mensaje", "Cuenta creada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String correo,
                        @RequestParam String clave,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(correo.trim());
        boolean passwordMatches = usuarioOpt.map(u -> {
            String stored = u.getClave();
            return stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))
                    ? passwordEncoder.matches(clave, stored)
                    : stored != null && stored.equals(clave);
        }).orElse(false);
        if (usuarioOpt.isEmpty() || !passwordMatches
                || usuarioOpt.get().getEstatus() == null || usuarioOpt.get().getEstatus() != 1) {
            auditService.registrar(null, "INICIO_SESION_FALLIDO", "usuarios", null);
            redirectAttributes.addFlashAttribute("error", "Correo o contraseña incorrectos.");
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getClave().startsWith("$2")) {
            usuario.setClave(passwordEncoder.encode(clave));
            usuarioRepository.save(usuario);
        }
        session.setAttribute("usuarioLogueado", usuario);
        auditService.registrar(usuario.getId(), "INICIO_SESION", "usuarios", usuario.getId());
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario != null) {
            auditService.registrar(usuario.getId(), "CIERRE_SESION", "usuarios", usuario.getId());
        }
        session.invalidate();
        return "redirect:/login";
    }
}
