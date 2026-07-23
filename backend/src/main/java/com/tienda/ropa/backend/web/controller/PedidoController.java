package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.service.PedidoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

// Controlador reactivo de pedidos
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PedidoResponse> create(
            @Valid @RequestBody PedidoCreateRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Mono<PedidoResponse> getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<PedidoResponse> getAll() {
        return service.list();
    }

    @PatchMapping("/{id}/estado")
    public Mono<PedidoResponse> updateEstado(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String nuevoEstado = body.get("estado");
        return service.updateEstado(id, nuevoEstado);
    }
}

