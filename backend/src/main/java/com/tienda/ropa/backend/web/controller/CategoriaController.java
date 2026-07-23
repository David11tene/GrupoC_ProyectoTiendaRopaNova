package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.categoria.*;
import com.tienda.ropa.backend.service.CategoriaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Controlador reactivo de categorías
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CategoriaResponse> create(
            @Valid @RequestBody CategoriaCreateRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Mono<CategoriaResponse> getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<CategoriaResponse> getAll() {
        return service.list();
    }

    @PutMapping("/{id}")
    public Mono<CategoriaResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}