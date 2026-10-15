package com.example.productos.controller;

import com.example.productos.model.DetalleEntrada;
import com.example.productos.repository.DetalleEntradaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-entradas")
public class DetalleEntradaController {
    private final DetalleEntradaRepository detalleEntradaRepository;

    public DetalleEntradaController(DetalleEntradaRepository detalleEntradaRepository) {
        this.detalleEntradaRepository = detalleEntradaRepository;
    }

    @GetMapping
    public List<DetalleEntrada> listar() {
        return detalleEntradaRepository.findAll();
    }
}
