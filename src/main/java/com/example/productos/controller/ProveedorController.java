package com.example.productos.controller;

import com.example.productos.model.Proveedor;
import com.example.productos.repository.ProveedorRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {
    private final ProveedorRepository proveedorRepository;
    private final AuditService auditService;

    public ProveedorController(ProveedorRepository proveedorRepository, AuditService auditService) {
        this.proveedorRepository = proveedorRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    @PostMapping
    @Transactional
    public Proveedor crear(@RequestBody Proveedor proveedor, HttpSession session) {
        Proveedor creado = proveedorRepository.save(proveedor);
        auditService.registrarDesdeSesion(session, "CREAR_PROVEEDOR", "proveedores", creado.getId());
        return creado;
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Proveedor> actualizar(@PathVariable Integer id, @RequestBody Proveedor proveedor,
                                                HttpSession session) {
        return proveedorRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(proveedor.getNombre());
                    actual.setRifNit(proveedor.getRifNit());
                    actual.setTelefono(proveedor.getTelefono());
                    actual.setCorreo(proveedor.getCorreo());
                    actual.setDireccion(proveedor.getDireccion());
                    Proveedor actualizado = proveedorRepository.save(actual);
                    auditService.registrarDesdeSesion(session, "EDITAR_PROVEEDOR", "proveedores", actualizado.getId());
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpSession session) {
        if (!proveedorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        proveedorRepository.deleteById(id);
        auditService.registrarDesdeSesion(session, "ELIMINAR_PROVEEDOR", "proveedores", id);
        return ResponseEntity.noContent().build();
    }
}
