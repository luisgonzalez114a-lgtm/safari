package com.example.productos.repository;

import com.example.productos.model.DetalleEntrada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleEntradaRepository extends JpaRepository<DetalleEntrada, Integer> {
    void deleteByEntrada_Id(Integer entradaId);

    @EntityGraph(attributePaths = {"producto", "entrada", "entrada.proveedor", "entrada.usuario"})
    java.util.List<DetalleEntrada> findAllByEntrada_IdOrderByIdAsc(Integer entradaId);
}
