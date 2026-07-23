package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// Servicio reactivo de pedidos
public interface PedidoService {

    // Crea un pedido
    Mono<PedidoResponse> create(PedidoCreateRequest request);

    // Obtiene pedido por id
    Mono<PedidoResponse> getById(Long id);

    // Lista pedidos
    Flux<PedidoResponse> list();

    // Actualiza estado del pedido
    Mono<PedidoResponse> updateEstado(Long id, String nuevoEstado);
}