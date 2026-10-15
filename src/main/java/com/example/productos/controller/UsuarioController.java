package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @PostMapping
    public Usuario crear(@RequestBody Usuario usuario) {
        if (usuario.getClave() == null || usuario.getClave().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        return usuarioRepository.save(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario usuario) {
        return usuarioRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(usuario.getNombre());
                    actual.setTelefono(usuario.getTelefono());
                    actual.setCorreo(usuario.getCorreo());
                    if (usuario.getClave() != null && !usuario.getClave().isBlank()) {
                        actual.setClave(passwordEncoder.encode(usuario.getClave()));
                    }
                    actual.setRol(usuario.getRol());
                    actual.setEstatus(usuario.getEstatus());
                    return ResponseEntity.ok(usuarioRepository.save(actual));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!usuarioRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
