package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.DetallePedido;
import com.tienda.ropa.backend.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Long> {

    // Obtener detalles de un pedido
    List<DetallePedido> findByPedido(Pedido pedido);

    // Obtener todos los detalles asociados a un producto (para calcular ventas)
    List<DetallePedido> findByProductoId(Long productoId);

    // Promedio de venta (subtotal) de un producto específico. Null si nunca se ha vendido.
    @Query("SELECT AVG(d.subtotal) FROM DetallePedido d WHERE d.producto.id = :productoId")
    Double averageSubtotalByProductoId(@Param("productoId") Long productoId);

    // Promedio de venta agrupado por categoría. Null si la categoría no tiene ventas.
    @Query("SELECT AVG(d.subtotal) FROM DetallePedido d WHERE d.producto.categoria.id = :categoriaId")
    Double averageSubtotalByCategoriaId(@Param("categoriaId") Long categoriaId);
}
