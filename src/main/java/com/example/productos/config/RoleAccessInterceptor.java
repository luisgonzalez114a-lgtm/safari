package com.example.productos.config;

import com.example.productos.model.Usuario;
import com.example.productos.repository.UsuarioRepository;
import com.example.productos.service.AuditService;
import com.example.productos.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class RoleAccessInterceptor implements HandlerInterceptor {
    private final UsuarioRepository usuarioRepository;
    private final RbacService rbacService;
    private final AuditService auditService;

    public RoleAccessInterceptor(UsuarioRepository usuarioRepository,
                                 RbacService rbacService,
                                 AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rbacService = rbacService;
        this.auditService = auditService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if (path.equals("/health") || path.startsWith("/actuator/")
                || path.equals("/login") || path.equals("/logout")
                || path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/favicon.ico")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Usuario user = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
        if (session != null && user != null) {
            Usuario currentUser = user.getId() == null
                    ? null
                    : usuarioRepository.findById(user.getId()).orElse(null);
            if (currentUser == null || currentUser.getEstatus() == null || currentUser.getEstatus() != 1) {
                session.invalidate();
                user = null;
            } else {
                session.setAttribute("usuarioLogueado", currentUser);
                user = currentUser;
            }
        }
        if (user == null && isPublicRead(path, method)) {
            return true;
        }
        Optional<RbacService.AccessRule> rule = RbacService.reglaPara(path, method, request.getParameter("id"));
        if (rule.isEmpty()) {
            if (path.startsWith("/api/")) {
                return deny(request, response, path, user);
            }
            return true;
        }
        if (user != null && rbacService.usuarioTienePermiso(user.getId(),
                rule.get().route(), rule.get().action())) {
            return true;
        }
        return deny(request, response, path, user);
    }

    private boolean isPublicRead(String path, String method) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return path.equals("/") || path.equals("/api/productos")
                || path.equals("/api/categorias") || path.equals("/api/marcas");
    }

    private boolean deny(HttpServletRequest request, HttpServletResponse response,
                         String path, Usuario user) throws IOException {
        String action = "ACCESO_DENEGADO " + path;
        auditService.registrar(user == null ? null : user.getId(),
                action.substring(0, Math.min(action.length(), 100)), "rutas", null);
        if (path.startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para realizar esta operación.");
        } else if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
        return false;
    }
}
