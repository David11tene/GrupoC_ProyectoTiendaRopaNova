package com.tienda.ropa.backend.dto.usuario;

import jakarta.validation.constraints.*;

public class UsuarioUpdateRequest {

    @Size(min = 3, max = 100)
    private String nombre;

    @Email(message = "Correo inválido")
    @Size(max = 100)
    private String correo;

    @Size(min = 4, max = 255)
    private String contrasena;

    private String rol;

    private Boolean active;

    // GETTERS Y SETTERS

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
