package com.tienda.ropa.backend.dto.pedido;

import com.tienda.ropa.backend.dto.detallepedido.DetallePedidoResponse;
import java.time.LocalDate;
import java.util.List;

public class PedidoResponse {

    private Long id;
    private String usuario;
    private LocalDate fecha;
    private Double total;
    private String estado;
    private List<DetallePedidoResponse> detalles;

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<DetallePedidoResponse> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedidoResponse> detalles) {
        this.detalles = detalles;
    }
}
