package com.example.productos.controller;

import com.example.productos.model.Marca;
import com.example.productos.repository.MarcaRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marcas")
public class MarcaController {
    private final MarcaRepository marcaRepository;
    private final AuditService auditService;

    public MarcaController(MarcaRepository marcaRepository, AuditService auditService) {
        this.marcaRepository = marcaRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Marca> listar() {
        return marcaRepository.findAll();
    }

    @PostMapping
    @Transactional
    public Marca crear(@RequestBody Marca marca, HttpSession session) {
        Marca creada = marcaRepository.save(marca);
        auditService.registrarDesdeSesion(session, "CREAR_MARCA", "marcas", creada.getId());
        return creada;
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Marca> actualizar(@PathVariable Integer id, @RequestBody Marca marca,
                                            HttpSession session) {
        return marcaRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(marca.getNombre());
                    actual.setEstatus(marca.getEstatus());
                    Marca actualizada = marcaRepository.save(actual);
                    auditService.registrarDesdeSesion(session, "EDITAR_MARCA", "marcas", actualizada.getId());
                    return ResponseEntity.ok(actualizada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpSession session) {
        if (!marcaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        marcaRepository.deleteById(id);
        auditService.registrarDesdeSesion(session, "ELIMINAR_MARCA", "marcas", id);
        return ResponseEntity.noContent().build();
    }
}
