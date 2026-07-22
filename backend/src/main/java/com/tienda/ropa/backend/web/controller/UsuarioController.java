package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.usuario.*;
import com.tienda.ropa.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador de usuarios
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    // Inyección del servicio
    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // Crea usuario
    @PostMapping
    public ResponseEntity<UsuarioResponse> create(
            @Valid @RequestBody UsuarioCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    // Obtiene usuario por id
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(service.getById(id));
    }

    // Lista usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAll() {

        return ResponseEntity.ok(service.list());
    }

    // Desactiva usuario
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UsuarioResponse> deactivate(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(service.deactivate(id));
    }

    // Busca usuarios por nombre
    @GetMapping("/search")
    public ResponseEntity<Page<UsuarioResponse>> search(
            @RequestParam String name,
            @RequestParam int page,
            @RequestParam int size
    ) {

        return ResponseEntity.ok(
                service.searchByName(name, page, size)
        );
    }

    // Actualiza usuario
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {

        return ResponseEntity.ok(
                service.update(id, request)
        );
    }
}