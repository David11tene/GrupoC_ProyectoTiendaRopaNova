package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.usuario.*;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.service.UsuarioService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.tienda.ropa.backend.service.reactive.UsuarioReactiveService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repo;
    private final UsuarioReactiveService usuarioReactiveService;

    public UsuarioServiceImpl(UsuarioRepository repo, UsuarioReactiveService usuarioReactiveService) {
        this.repo = repo;
        this.usuarioReactiveService = usuarioReactiveService;
    }

    @Override
    public Mono<UsuarioResponse> create(UsuarioCreateRequest request) {
        return Mono.fromCallable(() -> {
            if (repo.existsByCorreo(request.getCorreo())) {
                throw new ConflictException("El correo " + request.getCorreo() + " ya está registrado en el sistema");
            }

            Usuario u = new Usuario();

            u.setNombre(request.getNombre());
            u.setCorreo(request.getCorreo());
            u.setContrasena(request.getContrasena());
            u.setRol(request.getRol());
            u.setActive(true);

            UsuarioResponse response = toResponse(repo.save(u));
            if (usuarioReactiveService != null) {
                usuarioReactiveService.publishUsuario(response);
            }
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UsuarioResponse> getById(Long id) {
        return Mono.fromCallable(() -> {
            Usuario u = repo.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException("Usuario con ID " + id + " no existe en el sistema"));

            return toResponse(u);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<UsuarioResponse> list() {
        return Mono.fromCallable(repo::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UsuarioResponse> deactivate(Long id) {
        return Mono.fromCallable(() -> {
            Usuario u = repo.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException("No se puede desactivar: usuario con ID " + id + " no existe"));

            u.setActive(false);

            UsuarioResponse response = toResponse(repo.save(u));
            if (usuarioReactiveService != null) {
                usuarioReactiveService.publishUsuario(response);
            }
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UsuarioResponse> update(Long id, UsuarioUpdateRequest request) {
        return Mono.fromCallable(() -> {
            Usuario u = repo.findById(id)
                    .orElseThrow(() ->
                            new NotFoundException("No se puede actualizar: usuario con ID " + id + " no existe"));

            if (request.getCorreo() != null &&
                    !request.getCorreo().equals(u.getCorreo()) &&
                    repo.existsByCorreo(request.getCorreo())) {
                throw new ConflictException("El correo " + request.getCorreo() + " ya está registrado en otro usuario");
            }

            if (request.getNombre() != null)
                u.setNombre(request.getNombre());

            if (request.getCorreo() != null)
                u.setCorreo(request.getCorreo());

            if (request.getContrasena() != null)
                u.setContrasena(request.getContrasena());

            if (request.getRol() != null)
                u.setRol(request.getRol());

            if (request.getActive() != null)
                u.setActive(request.getActive());

            UsuarioResponse response = toResponse(repo.save(u));
            if (usuarioReactiveService != null) {
                usuarioReactiveService.publishUsuario(response);
            }
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Page<UsuarioResponse>> searchByName(
            String name,
            int page,
            int size) {

        return Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page, size);
            return repo.findByNombreContainingIgnoreCase(name, pageable)
                    .map(this::toResponse);
        }).subscribeOn(Schedulers.boundedElastic());
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

