package com.tienda.ropa.backend.service;

import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;

import java.util.List;

// Servicio de pedidos
public interface PedidoService {

    // Crea un pedido
    PedidoResponse create(PedidoCreateRequest request);

    // Obtiene pedido por id
    PedidoResponse getById(Long id);

    // Lista pedidos
    List<PedidoResponse> list();

    // Actualiza estado del pedido
    PedidoResponse updateEstado(Long id, String nuevoEstado);
}