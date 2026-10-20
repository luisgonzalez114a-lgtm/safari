package com.example.productos.controller;

import com.example.productos.model.Categoria;
import com.example.productos.repository.CategoriaRepository;
import com.example.productos.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final CategoriaRepository categoriaRepository;
    private final AuditService auditService;

    public CategoriaController(CategoriaRepository categoriaRepository, AuditService auditService) {
        this.categoriaRepository = categoriaRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    @PostMapping
    @Transactional
    public Categoria crear(@RequestBody Categoria categoria, HttpSession session) {
        Categoria creada = categoriaRepository.save(categoria);
        auditService.registrarDesdeSesion(session, "CREAR_CATEGORIA", "categorias", creada.getId());
        return creada;
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Categoria> actualizar(@PathVariable Integer id, @RequestBody Categoria categoria,
                                                HttpSession session) {
        return categoriaRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(categoria.getNombre());
                    actual.setDescripcion(categoria.getDescripcion());
                    actual.setEstatus(categoria.getEstatus());
                    Categoria actualizada = categoriaRepository.save(actual);
                    auditService.registrarDesdeSesion(session, "EDITAR_CATEGORIA", "categorias", actualizada.getId());
                    return ResponseEntity.ok(actualizada);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpSession session) {
        if (!categoriaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoriaRepository.deleteById(id);
        auditService.registrarDesdeSesion(session, "ELIMINAR_CATEGORIA", "categorias", id);
        return ResponseEntity.noContent().build();
    }
}
