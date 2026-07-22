package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.usuario.*;
import org.springframework.data.domain.Page;

import java.util.List;

// Servicio de usuarios
public interface UsuarioService {

    // Crea un usuario
    UsuarioResponse create(UsuarioCreateRequest request);

    // Obtiene usuario por id
    UsuarioResponse getById(Long id);

    // Lista usuarios
    List<UsuarioResponse> list();

    // Desactiva usuario
    UsuarioResponse deactivate(Long id);

    // Actualiza usuario
    UsuarioResponse update(Long id, UsuarioUpdateRequest request);

    // Busca usuarios por nombre
    Page<UsuarioResponse> searchByName(
            String name,
            int page,
            int size
    );
}