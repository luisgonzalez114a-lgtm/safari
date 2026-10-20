package com.example.productos.repository;

import com.example.productos.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EntradaRepository extends JpaRepository<Entrada, Integer> {
    boolean existsByProveedor_Id(Integer proveedorId);

    @Query("select count(e) from Entrada e where e.fechaEntrada >= :inicio and e.fechaEntrada < :fin")
    long contarEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
