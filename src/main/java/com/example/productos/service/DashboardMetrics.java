package com.example.productos.service;

import java.math.BigDecimal;

public record DashboardMetrics(long totalProductos,
                               long unidadesEnStock,
                               long productosEnStockMinimo,
                               long entradasDelMes,
                               long ventasDelMes,
                               BigDecimal totalVentasDelMes) {
    public int porcentajeStockMinimo() {
        return totalProductos == 0
                ? 0
                : (int) Math.round((double) productosEnStockMinimo * 100 / totalProductos);
    }

    public int porcentajeEntradasDelMes() {
        long maximo = Math.max(entradasDelMes, ventasDelMes);
        return maximo == 0 ? 0 : (int) Math.round((double) entradasDelMes * 100 / maximo);
    }

    public int porcentajeVentasDelMes() {
        long maximo = Math.max(entradasDelMes, ventasDelMes);
        return maximo == 0 ? 0 : (int) Math.round((double) ventasDelMes * 100 / maximo);
    }
}
