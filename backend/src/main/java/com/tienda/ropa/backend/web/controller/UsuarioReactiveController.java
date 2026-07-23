package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.usuario.UsuarioResponse;
import com.tienda.ropa.backend.service.reactive.UsuarioReactiveService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/reactivo/usuarios")
public class UsuarioReactiveController {

    private final UsuarioReactiveService usuarioReactiveService;

    public UsuarioReactiveController(UsuarioReactiveService usuarioReactiveService) {
        this.usuarioReactiveService = usuarioReactiveService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UsuarioResponse> streamUsuarios() {
        return usuarioReactiveService.streamUsuarios();
    }

    @GetMapping("/conteo")
    public Mono<Long> conteoUsuarios() {
        return usuarioReactiveService.conteoUsuariosAsync();
    }

    @GetMapping(value = "/conteo-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Long> streamConteoUsuarios() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .flatMap(tick -> usuarioReactiveService.conteoUsuariosAsync());
    }
}
