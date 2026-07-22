package com.tienda.ropa.backend.dto.producto;

import jakarta.validation.constraints.*;

public class ProductoUpdateRequest {

    @Size(min = 3, max = 100)
    private String nombre;

    @Positive(message = "El precio debe ser mayor a 0")
    private Double precio;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    private Boolean active;

    private Long categoriaId;

    // GETTERS Y SETTERS

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}
