package com.tienda.ropa.backend.dto.producto;

/**
 * Respuesta usada por los endpoints reactivos de "promedio de ventas".
 * Se muestra en el panel admin de Productos como badge/mini-indicador
 * junto a cada fila de la tabla (ver Lab 3 - Spring WebFlux).
 */
public class ProductoPromedioVentaResponse {

    private Long productoId;
    private String nombre;
    private Double promedioVenta;
    private Long cantidadVentas;

    public ProductoPromedioVentaResponse() {
    }

    public ProductoPromedioVentaResponse(Long productoId, String nombre, Double promedioVenta, Long cantidadVentas) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.promedioVenta = promedioVenta;
        this.cantidadVentas = cantidadVentas;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPromedioVenta() {
        return promedioVenta;
    }

    public void setPromedioVenta(Double promedioVenta) {
        this.promedioVenta = promedioVenta;
    }

    public Long getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(Long cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }
}
