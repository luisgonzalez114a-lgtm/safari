package com.example.productos.repository;

import com.example.productos.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Integer> {
    Optional<Modulo> findByNombreIgnoreCase(String nombre);
}
