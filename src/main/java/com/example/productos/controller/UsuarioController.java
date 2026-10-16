package com.example.productos.controller;

import com.example.productos.model.Usuario;
import com.example.productos.model.Rol;
import com.example.productos.repository.RolRepository;
import com.example.productos.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository,
                             RolRepository rolRepository,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario usuario) {
        if (usuario.getClave() == null || usuario.getClave().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Set<Rol> roles = buscarRoles(usuario.getRoles());
        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        usuario.setRoles(roles);
        return ResponseEntity.ok(usuarioRepository.save(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Integer id, @RequestBody Usuario usuario) {
        Set<Rol> roles = buscarRoles(usuario.getRoles());
        if (roles == null || roles.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return usuarioRepository.findById(id)
                .map(actual -> {
                    actual.setNombre(usuario.getNombre());
                    actual.setTelefono(usuario.getTelefono());
                    actual.setCorreo(usuario.getCorreo());
                    if (usuario.getClave() != null && !usuario.getClave().isBlank()) {
                        actual.setClave(passwordEncoder.encode(usuario.getClave()));
                    }
                    actual.setRoles(roles);
                    actual.setEstatus(usuario.getEstatus());
                    return ResponseEntity.ok(usuarioRepository.save(actual));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Set<Rol> buscarRoles(Set<Rol> rolesSolicitados) {
        if (rolesSolicitados == null || rolesSolicitados.isEmpty()
                || rolesSolicitados.stream().anyMatch(rol -> rol.getId() == null || rol.getId() <= 0)) {
            return null;
        }
        List<Integer> ids = rolesSolicitados.stream().map(Rol::getId).distinct().toList();
        List<Rol> encontrados = rolRepository.findAllById(ids);
        return encontrados.size() == ids.size()
                ? encontrados.stream().collect(Collectors.toSet())
                : null;
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
