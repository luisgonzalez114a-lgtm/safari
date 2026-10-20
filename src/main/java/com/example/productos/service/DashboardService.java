package com.example.productos.service;

import com.example.productos.repository.EntradaRepository;
import com.example.productos.repository.SalidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DashboardService {
    private final ProductoRepository productoRepository;
    private final EntradaRepository entradaRepository;
    private final SalidaRepository salidaRepository;

    public DashboardService(ProductoRepository productoRepository,
                            EntradaRepository entradaRepository,
                            SalidaRepository salidaRepository) {
        this.productoRepository = productoRepository;
        this.entradaRepository = entradaRepository;
        this.salidaRepository = salidaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetrics obtenerResumenDelMes() {
        LocalDate hoy = LocalDate.now();
        var inicio = hoy.withDayOfMonth(1).atStartOfDay();
        var fin = hoy.withDayOfMonth(1).plusMonths(1).atStartOfDay();
        return new DashboardMetrics(
                productoRepository.count(),
                productoRepository.totalUnidadesEnStock(),
                productoRepository.contarProductosEnStockMinimo(),
                entradaRepository.contarEntreFechas(inicio, fin),
                salidaRepository.contarEntreFechas(inicio, fin),
                salidaRepository.sumarVentasEntreFechas(inicio, fin).setScale(2));
    }
}
