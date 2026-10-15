package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
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
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String correo,
                        @RequestParam String clave,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(correo.trim());
        boolean passwordMatches = usuarioOpt.map(u -> {
            String stored = u.getClave();
            return stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"))
                    ? passwordEncoder.matches(clave, stored)
                    : stored != null && stored.equals(clave);
        }).orElse(false);
        if (usuarioOpt.isEmpty() || !passwordMatches
                || usuarioOpt.get().getEstatus() == null || usuarioOpt.get().getEstatus() != 1) {
            redirectAttributes.addFlashAttribute("error", "Correo o contraseña incorrectos.");
            return "redirect:/login";
        }

        Usuario usuario = usuarioOpt.get();
        if (!usuario.getClave().startsWith("$2")) {
            usuario.setClave(passwordEncoder.encode(clave));
            usuarioRepository.save(usuario);
        }
        session.setAttribute("usuarioLogueado", usuario);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
