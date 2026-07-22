package com.tienda.ropa.backend.service.impl;


import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.usuario.*;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.service.UsuarioService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repo;

    public UsuarioServiceImpl(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UsuarioResponse create(UsuarioCreateRequest request) {

        if(repo.existsByCorreo(request.getCorreo())) {
            throw new ConflictException("El correo " + request.getCorreo() + " ya está registrado en el sistema");
        }

        Usuario u = new Usuario();

        u.setNombre(request.getNombre());
        u.setCorreo(request.getCorreo());
        u.setContrasena(request.getContrasena());
        u.setRol(request.getRol());
        u.setActive(true);

        return toResponse(repo.save(u));
    }

    @Override
    public UsuarioResponse getById(Long id) {

        Usuario u = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Usuario con ID " + id + " no existe en el sistema"));

        return toResponse(u);
    }

    @Override
    public List<UsuarioResponse> list() {
        return repo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UsuarioResponse deactivate(Long id) {

        Usuario u = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("No se puede desactivar: usuario con ID " + id + " no existe"));

        u.setActive(false);

        return toResponse(repo.save(u));
    }

    @Override
    public UsuarioResponse update(Long id,
                                  UsuarioUpdateRequest request) {

        Usuario u = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("No se puede actualizar: usuario con ID " + id + " no existe"));

        if(request.getCorreo() != null &&
                !request.getCorreo().equals(u.getCorreo()) &&
                repo.existsByCorreo(request.getCorreo())) {
            throw new ConflictException("El correo " + request.getCorreo() + " ya está registrado en otro usuario");
        }

        if(request.getNombre() != null)
            u.setNombre(request.getNombre());

        if(request.getCorreo() != null)
            u.setCorreo(request.getCorreo());

        if(request.getContrasena() != null)
            u.setContrasena(request.getContrasena());

        if(request.getRol() != null)
            u.setRol(request.getRol());

        if(request.getActive() != null)
            u.setActive(request.getActive());

        return toResponse(repo.save(u));
    }

    @Override
    public Page<UsuarioResponse> searchByName(
            String name,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        return repo.findByNombreContainingIgnoreCase(name, pageable)
                .map(this::toResponse);
    }

    private UsuarioResponse toResponse(Usuario u) {

        UsuarioResponse r = new UsuarioResponse();

        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setCorreo(u.getCorreo());
        r.setRol(u.getRol());
        r.setActive(u.getActive());

        return r;
    }
}
