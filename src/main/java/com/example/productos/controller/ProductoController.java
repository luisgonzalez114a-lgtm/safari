package com.example.productos.controller;

import com.example.productos.model.Producto;
import com.example.productos.service.AuditService;
import com.example.productos.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService productoService;
    private final AuditService auditService;

    public ProductoController(ProductoService productoService, AuditService auditService) {
        this.productoService = productoService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Producto> crear(@RequestBody Producto producto, HttpSession session) {
        Producto creado = productoService.crear(producto);
        auditService.registrarDesdeSesion(session, "CREAR_PRODUCTO", "productos", creado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Producto> actualizar(@PathVariable Integer id, @RequestBody Producto producto,
                                               HttpSession session) {
        Producto actualizado = productoService.actualizar(id, producto);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        auditService.registrarDesdeSesion(session, "EDITAR_PRODUCTO", "productos", actualizado.getId());
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Integer id, HttpSession session) {
        boolean eliminado = productoService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        auditService.registrarDesdeSesion(session, "ELIMINAR_PRODUCTO", "productos", id);
        return ResponseEntity.noContent().build();
    }
}
