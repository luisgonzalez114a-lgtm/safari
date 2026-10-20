package com.example.productos.repository;

import com.example.productos.model.Vista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VistaRepository extends JpaRepository<Vista, Integer> {
    Optional<Vista> findByRutaAcceso(String rutaAcceso);
    List<Vista> findAllByOrderByModulo_OrdenAscNombreAsc();
}
