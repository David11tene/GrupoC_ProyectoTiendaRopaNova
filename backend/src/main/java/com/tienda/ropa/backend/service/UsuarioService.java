package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.usuario.*;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Servicio reactivo de usuarios
public interface UsuarioService {

    // Crea un usuario
    Mono<UsuarioResponse> create(UsuarioCreateRequest request);

    // Obtiene usuario por id
    Mono<UsuarioResponse> getById(Long id);

    // Lista usuarios
    Flux<UsuarioResponse> list();

    // Desactiva usuario
    Mono<UsuarioResponse> deactivate(Long id);

    // Actualiza usuario
    Mono<UsuarioResponse> update(Long id, UsuarioUpdateRequest request);

    // Busca usuarios por nombre
    Mono<Page<UsuarioResponse>> searchByName(
            String name,
            int page,
            int size
    );
}