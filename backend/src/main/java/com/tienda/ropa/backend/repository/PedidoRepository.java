package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Obtener pedidos de un usuario
    List<Pedido> findByUsuario(Usuario usuario);

    // Buscar pedidos por estado
    List<Pedido> findByEstado(String estado);
}
