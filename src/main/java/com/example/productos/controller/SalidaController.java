package com.example.productos.controller;

import com.example.productos.model.Salida;
import com.example.productos.repository.SalidaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salidas")
public class SalidaController {
    private final SalidaRepository salidaRepository;

    public SalidaController(SalidaRepository salidaRepository) {
        this.salidaRepository = salidaRepository;
    }

    @GetMapping
    public List<Salida> listar() {
        return salidaRepository.findAll();
    }
}
