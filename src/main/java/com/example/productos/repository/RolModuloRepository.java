package com.example.productos.repository;

import com.example.productos.model.RolModulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolModuloRepository extends JpaRepository<RolModulo, Integer> {
    Optional<RolModulo> findByRol_IdAndModulo_Id(Integer rolId, Integer moduloId);
    List<RolModulo> findAllByRol_Id(Integer rolId);
    void deleteAllByRol_Id(Integer rolId);
}
