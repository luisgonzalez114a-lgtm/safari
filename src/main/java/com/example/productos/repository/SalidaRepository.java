package com.example.productos.repository;

import com.example.productos.model.Salida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SalidaRepository extends JpaRepository<Salida, Integer> {
    @Query("select coalesce(sum(s.montoTotal), 0) from Salida s where s.fechaSalida >= :inicio and s.fechaSalida < :fin")
    BigDecimal sumarVentasEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("select count(s) from Salida s where s.fechaSalida >= :inicio and s.fechaSalida < :fin")
    long contarEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
