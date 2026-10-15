package com.example.productos.repository;

import com.example.productos.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaRepository extends JpaRepository<Entrada, Integer> {
    boolean existsByProveedor_Id(Integer proveedorId);
}
