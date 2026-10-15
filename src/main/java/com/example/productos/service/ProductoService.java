package com.example.productos.service;

import com.example.productos.model.Producto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {
    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public boolean codigoEnUso(String codigo, Integer id) {
        if (codigo == null) {
            return false;
        }
        String normalizado = codigo.trim();
        return id == null
                ? productoRepository.existsByCodigoIgnoreCase(normalizado)
                : productoRepository.existsByCodigoIgnoreCaseAndIdNot(normalizado, id);
    }

    public Producto crear(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto actualizar(Integer id, Producto datos) {
        Optional<Producto> existenteOpt = productoRepository.findById(id);
        if (existenteOpt.isEmpty()) {
            return null;
        }

        Producto existente = existenteOpt.get();
        existente.setCodigo(datos.getCodigo());
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setIdMarca(datos.getIdMarca());
        existente.setIdCategoria(datos.getIdCategoria());
        existente.setPrecioCompra(datos.getPrecioCompra());
        existente.setPrecioActual(datos.getPrecioActual());
        existente.setStockActual(datos.getStockActual());
        existente.setStockMinimo(datos.getStockMinimo());
        return productoRepository.save(existente);
    }

    public boolean eliminar(Integer id) {
        if (!productoRepository.existsById(id)) {
            return false;
        }
        productoRepository.deleteById(id);
        return true;
    }
}
