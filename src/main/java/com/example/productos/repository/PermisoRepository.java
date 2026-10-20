package com.example.productos.repository;

import com.example.productos.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    Optional<Permiso> findByNombreIgnoreCase(String nombre);
}
