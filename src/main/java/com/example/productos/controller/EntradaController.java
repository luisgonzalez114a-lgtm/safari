package com.example.productos.controller;

import com.example.productos.model.Entrada;
import com.example.productos.repository.EntradaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {
    private final EntradaRepository entradaRepository;

    public EntradaController(EntradaRepository entradaRepository) {
        this.entradaRepository = entradaRepository;
    }

    @GetMapping
    public List<Entrada> listar() {
        return entradaRepository.findAll();
    }
}
