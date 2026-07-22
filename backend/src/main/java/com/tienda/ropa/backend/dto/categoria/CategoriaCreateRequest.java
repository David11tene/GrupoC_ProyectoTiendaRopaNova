package com.tienda.ropa.backend.dto.categoria;

import jakarta.validation.constraints.*;

public class CategoriaCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100)
    private String nombre;

    // GETTERS Y SETTERS

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
