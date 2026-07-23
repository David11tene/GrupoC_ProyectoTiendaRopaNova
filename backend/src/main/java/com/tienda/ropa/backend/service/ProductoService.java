package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.producto.*;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Servicio reactivo de productos
public interface ProductoService {

    // Crea un producto
    Mono<ProductoResponse> create(ProductoCreateRequest request);

    // Obtiene producto por id
    Mono<ProductoResponse> getById(Long id);

    // Lista productos
    Flux<ProductoResponse> list();

    // Desactiva producto
    Mono<ProductoResponse> deactivate(Long id);

    // Actualiza producto
    Mono<ProductoResponse> update(Long id, ProductoUpdateRequest request);

    // Busca productos por nombre
    Mono<Page<ProductoResponse>> searchByName(
            String name,
            int page,
            int size
    );
}