package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.categoria.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Servicio reactivo de categorías
public interface CategoriaService {

    // Crea una categoría
    Mono<CategoriaResponse> create(CategoriaCreateRequest request);

    // Obtiene categoría por id
    Mono<CategoriaResponse> getById(Long id);

    // Lista categorías
    Flux<CategoriaResponse> list();

    // Actualiza categoría
    Mono<CategoriaResponse> update(Long id, CategoriaUpdateRequest request);

    // Elimina categoría
    Mono<Void> delete(Long id);
}