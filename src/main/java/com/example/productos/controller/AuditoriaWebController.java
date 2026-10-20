package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.repository.AuditoriaLogRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditoriaWebController {
    private final AuditoriaLogRepository auditoriaLogRepository;

    public AuditoriaWebController(AuditoriaLogRepository auditoriaLogRepository) {
        this.auditoriaLogRepository = auditoriaLogRepository;
    }

    @GetMapping("/auditoria")
    public String auditoria(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || !usuario.tieneRol("ADMIN")) {
            return "redirect:/";
        }
        model.addAttribute("usuarioLogueado", usuario);
        model.addAttribute("registros", auditoriaLogRepository.findAllByOrderByFechaHoraDesc(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "fechaHora"))));
        return "auditoria";
    }
}
