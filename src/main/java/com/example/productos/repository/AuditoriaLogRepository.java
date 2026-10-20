package com.example.productos.repository;

import com.example.productos.model.AuditoriaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Integer> {
    @EntityGraph(attributePaths = "usuario")
    Page<AuditoriaLog> findAllByOrderByFechaHoraDesc(Pageable pageable);
}
