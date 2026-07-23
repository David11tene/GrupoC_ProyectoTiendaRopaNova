package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.usuario.*;
import com.tienda.ropa.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Controlador reactivo de usuarios
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UsuarioResponse> create(
            @Valid @RequestBody UsuarioCreateRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Mono<UsuarioResponse> getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<UsuarioResponse> getAll() {
        return service.list();
    }

    @PatchMapping("/{id}/deactivate")
    public Mono<UsuarioResponse> deactivate(
            @PathVariable Long id
    ) {
        return service.deactivate(id);
    }

    @GetMapping("/search")
    public Mono<Page<UsuarioResponse>> search(
            @RequestParam String name,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return service.searchByName(name, page, size);
    }

    @PutMapping("/{id}")
    public Mono<UsuarioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return service.update(id, request);
    }
}