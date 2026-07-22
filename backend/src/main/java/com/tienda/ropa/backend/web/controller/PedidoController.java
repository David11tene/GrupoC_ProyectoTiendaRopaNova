package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.service.PedidoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controlador de pedidos
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    // Inyección del servicio
    public PedidoController(PedidoService service) {
        this.service = service;
    }

    // Crea pedido
    @PostMapping
    public ResponseEntity<PedidoResponse> create(
            @Valid @RequestBody PedidoCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    // Obtiene pedido por id
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(service.getById(id));
    }

    // Lista pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> getAll() {

        return ResponseEntity.ok(service.list());
    }

    // Actualiza estado del pedido
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> updateEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {

        String nuevoEstado = body.get("estado");

        return ResponseEntity.ok(
            service.updateEstado(id, nuevoEstado)

        );
    }
}
