package com.example.productos.repository;

import com.example.productos.model.DetalleEntrada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleEntradaRepository extends JpaRepository<DetalleEntrada, Integer> {
    void deleteByEntrada_Id(Integer entradaId);
}
