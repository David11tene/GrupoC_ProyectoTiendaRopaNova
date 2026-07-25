package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.producto.*;
import com.tienda.ropa.backend.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Controlador reactivo de productos
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductoResponse> create(
            @Valid @RequestBody ProductoCreateRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Mono<ProductoResponse> getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<ProductoResponse> getAll() {
        return service.list();
    }

    @PatchMapping("/{id}/deactivate")
    public Mono<ProductoResponse> deactivate(
            @PathVariable Long id
    ) {
        return service.deactivate(id);
    }

    @GetMapping("/search")
    public Mono<Page<ProductoResponse>> search(
            @RequestParam String name,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return service.searchByName(name, page, size);
    }

    @PutMapping("/{id}")
    public Mono<ProductoResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateRequest request
    ) {
        return service.update(id, request);
    }
}