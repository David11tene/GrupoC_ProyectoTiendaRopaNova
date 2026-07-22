package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Controlador de autenticación
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    // Inyección del repositorio
    public AuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Login de usuario
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> credentials
    ) {

        String nombre = credentials.get("nombre");
        String contrasena = credentials.get("contrasena");

        // Valida campos
        if (
                nombre == null || nombre.isBlank() ||
                        contrasena == null || contrasena.isBlank()
        ) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error",
                            "Los campos 'nombre' y 'contrasena' son obligatorios."
                    ));
        }

        // Busca usuario
        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElse(null);

        // Valida credenciales
        if (
                usuario == null ||
                        !usuario.getContrasena().equals(contrasena)
        ) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Credenciales inválidas."
                    ));
        }

        // Valida estado
        if (!Boolean.TRUE.equals(usuario.getActive())) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Cuenta desactivada. Contacta al administrador."
                    ));
        }

        // Respuesta exitosa
        Map<String, Object> response = Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol(),
                "active", usuario.getActive()
        );

        return ResponseEntity.ok(response);
    }
}