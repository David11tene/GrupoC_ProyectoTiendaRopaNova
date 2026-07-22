package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.producto.*;
import org.springframework.data.domain.Page;

import java.util.List;

// Servicio de productos
public interface ProductoService {

    // Crea un producto
    ProductoResponse create(ProductoCreateRequest request);

    // Obtiene producto por id
    ProductoResponse getById(Long id);

    // Lista productos
    List<ProductoResponse> list();

    // Desactiva producto
    ProductoResponse deactivate(Long id);

    // Actualiza producto
    ProductoResponse update(Long id, ProductoUpdateRequest request);

    // Busca productos por nombre
    Page<ProductoResponse> searchByName(
            String name,
            int page,
            int size
    );
}