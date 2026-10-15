package com.example.productos.controller;

import com.example.productos.model.DetalleSalida;
import com.example.productos.repository.DetalleSalidaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/detalles-salidas")
public class DetalleSalidaController {
    private final DetalleSalidaRepository detalleSalidaRepository;

    public DetalleSalidaController(DetalleSalidaRepository detalleSalidaRepository) {
        this.detalleSalidaRepository = detalleSalidaRepository;
    }

    @GetMapping
    public List<DetalleSalida> listar() {
        return detalleSalidaRepository.findAll();
    }
}
