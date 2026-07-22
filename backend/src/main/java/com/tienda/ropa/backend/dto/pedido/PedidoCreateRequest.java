package com.tienda.ropa.backend.dto.pedido;

import jakarta.validation.constraints.*;
import java.util.List;

public class PedidoCreateRequest {

    @NotNull(message = "El idUsuario es obligatorio")
    private Long idUsuario;

    @NotEmpty(message = "La lista de productos no puede estar vacía")
    private List<ProductoItemRequest> productos;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public List<ProductoItemRequest> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoItemRequest> productos) {
        this.productos = productos;
    }

    public static class ProductoItemRequest {
        @NotNull(message = "El idProducto es obligatorio")
        private Long idProducto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;

        public Long getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Long idProducto) {
            this.idProducto = idProducto;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }
}
