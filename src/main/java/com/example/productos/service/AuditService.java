package com.example.productos.service;

import com.example.productos.model.AuditoriaLog;
import com.example.productos.model.Usuario;
import com.example.productos.repository.AuditoriaLogRepository;
import com.example.productos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditService(AuditoriaLogRepository auditoriaLogRepository,
                        UsuarioRepository usuarioRepository) {
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(Integer usuarioId, String accion, String tablaAfectada, Integer registroId) {
        if (accion == null || accion.isBlank() || tablaAfectada == null || tablaAfectada.isBlank()) {
            throw new IllegalArgumentException("La acción y la tabla de auditoría son obligatorias.");
        }
        AuditoriaLog registro = new AuditoriaLog();
        if (usuarioId != null) {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario de auditoría."));
            registro.setUsuario(usuario);
        }
        registro.setAccion(accion.trim());
        registro.setTablaAfectada(tablaAfectada.trim());
        registro.setRegistroId(registroId);
        auditoriaLogRepository.save(registro);
    }

    public void registrarDesdeSesion(HttpSession session, String accion, String tablaAfectada, Integer registroId) {
        Object usuarioSesion = session == null ? null : session.getAttribute("usuarioLogueado");
        if (!(usuarioSesion instanceof Usuario usuario) || usuario.getId() == null) {
            throw new IllegalStateException("No se puede auditar una operación sin un usuario autenticado.");
        }
        registrar(usuario.getId(), accion, tablaAfectada, registroId);
    }
}
