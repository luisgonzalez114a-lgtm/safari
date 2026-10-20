package com.example.productos.controller;

import com.example.productos.model.Rol;
import com.example.productos.repository.RolRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {
    private final RolRepository rolRepository;
    private final AuditService auditService;

    public RolController(RolRepository rolRepository, AuditService auditService) {
        this.rolRepository = rolRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    @PostMapping
    @Transactional
    public Rol crear(@RequestBody Rol rol, HttpSession session) {
        Rol creado = rolRepository.save(rol);
        auditService.registrarDesdeSesion(session, "CREAR_ROL", "roles", creado.getId());
        return creado;
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Rol> actualizar(@PathVariable Integer id, @RequestBody Rol rol, HttpSession session) {
        return rolRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(rol.getNombre());
                    Rol actualizado = rolRepository.save(actual);
                    auditService.registrarDesdeSesion(session, "EDITAR_ROL", "roles", actualizado.getId());
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpSession session) {
        Rol rol = rolRepository.findById(id).orElse(null);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        if (!rol.getUsuarios().isEmpty()) {
            return ResponseEntity.status(409).build();
        }
        rolRepository.deleteById(id);
        auditService.registrarDesdeSesion(session, "ELIMINAR_ROL", "roles", id);
        return ResponseEntity.noContent().build();
    }
}
