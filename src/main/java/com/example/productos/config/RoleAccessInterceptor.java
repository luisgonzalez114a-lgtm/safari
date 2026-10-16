package com.example.productos.config;

import com.example.productos.model.Usuario;
import com.example.productos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class RoleAccessInterceptor implements HandlerInterceptor {
    private static final Set<String> ADMIN_WEB_PATHS = Set.of(
            "/usuarios", "/usuarios/guardar", "/usuarios/rol", "/usuarios/actualizar", "/usuarios/estado",
            "/usuarios/eliminar", "/roles", "/roles/guardar", "/catalogos",
            "/catalogos/categorias", "/catalogos/categorias/actualizar", "/catalogos/categorias/eliminar",
            "/catalogos/marcas", "/catalogos/marcas/actualizar", "/catalogos/marcas/eliminar",
            "/catalogos/proveedores", "/catalogos/proveedores/actualizar", "/catalogos/proveedores/eliminar");
    private static final Set<String> OPERATIONS_WEB_PATHS = Set.of(
            "/entradas", "/entradas/guardar", "/salidas", "/salidas/guardar");
    private final UsuarioRepository usuarioRepository;

    public RoleAccessInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        if (path.equals("/health") || path.startsWith("/actuator/")) {
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
        boolean admin = hasRole(user, "ADMIN");
        boolean seller = hasRole(user, "VENDEDOR");

        if (ADMIN_WEB_PATHS.contains(path) && !admin) {
            return deny(request, response, path.startsWith("/api/"));
        }
        if (OPERATIONS_WEB_PATHS.contains(path) && !admin && !seller) {
            return deny(request, response, false);
        }

        if (!path.startsWith("/api/")) {
            return true;
        }

        if (path.startsWith("/api/productos")) {
            if ("GET".equals(method)) {
                return true;
            }
            return admin || deny(request, response, true);
        }
        if (path.startsWith("/api/entradas") || path.startsWith("/api/salidas")
                || path.startsWith("/api/detalles-entradas") || path.startsWith("/api/detalles-salidas")) {
            return admin || seller || deny(request, response, true);
        }
        if (path.startsWith("/api/usuarios") || path.startsWith("/api/roles")) {
            return admin || deny(request, response, true);
        }
        if (path.startsWith("/api/proveedores")) {
            return admin || deny(request, response, true);
        }
        if (path.startsWith("/api/categorias") || path.startsWith("/api/marcas")) {
            if ("GET".equals(method)) {
                return true;
            }
            return admin || deny(request, response, true);
        }
        return deny(request, response, true);
    }

    private boolean hasRole(Usuario user, String roleName) {
        return user != null && user.tieneRol(roleName);
    }

    private boolean deny(HttpServletRequest request, HttpServletResponse response, boolean api) throws IOException {
        if (api) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No tienes permiso para realizar esta operación.");
        } else if (request.getSession(false) == null
                || request.getSession(false).getAttribute("usuarioLogueado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
        } else {
            response.sendRedirect(request.getContextPath() + "/");
        }
        return false;
    }
}
