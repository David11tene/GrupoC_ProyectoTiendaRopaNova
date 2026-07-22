package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.DetallePedido;
import com.tienda.ropa.backend.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Long> {

    // Obtener detalles de un pedido
    List<DetallePedido> findByPedido(Pedido pedido);
}
