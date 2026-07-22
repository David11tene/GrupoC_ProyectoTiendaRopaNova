package com.tienda.ropa.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Maneja rutas de React
@RestController
public class SpaController {

    @GetMapping(value = {
            "/",
            "/login",
            "/cart",
            "/mis-pedidos",
            "/admin",
            "/admin/**"
    })
    public ResponseEntity<Resource> spa(HttpServletRequest request) throws Exception {

        // Devuelve index.html
        Resource index = new ClassPathResource("static/index.html");

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(index);
    }
}