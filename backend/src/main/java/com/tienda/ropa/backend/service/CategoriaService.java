package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.categoria.*;

import java.util.List;

// Servicio de categorías
public interface CategoriaService {

    // Crea una categoría
    CategoriaResponse create(CategoriaCreateRequest request);

    // Obtiene categoría por id
    CategoriaResponse getById(Long id);

    // Lista categorías
    List<CategoriaResponse> list();

    // Actualiza categoría
    CategoriaResponse update(Long id, CategoriaUpdateRequest request);

    // Elimina categoría
    void delete(Long id);
}