package com.example.productos.repository;

import com.example.productos.model.DetalleSalida;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleSalidaRepository extends JpaRepository<DetalleSalida, Integer> {
    void deleteBySalida_Id(Integer salidaId);

    @EntityGraph(attributePaths = {"producto", "salida", "salida.usuario"})
    java.util.List<DetalleSalida> findAllBySalida_IdOrderByIdAsc(Integer salidaId);

    @EntityGraph(attributePaths = {"producto", "salida", "salida.usuario"})
    java.util.List<DetalleSalida> findAllByOrderBySalida_FechaSalidaDescIdAsc();
}
