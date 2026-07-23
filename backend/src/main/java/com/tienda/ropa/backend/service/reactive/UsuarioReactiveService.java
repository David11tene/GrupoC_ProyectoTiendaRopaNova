package com.tienda.ropa.backend.service.reactive;

import com.tienda.ropa.backend.dto.usuario.UsuarioResponse;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
public class UsuarioReactiveService {

    private final UsuarioRepository usuarioRepository;

    // Sink multicanal para transmitir en tiempo real nuevos usuarios / actualizaciones
    private final Sinks.Many<UsuarioResponse> usuarioSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public UsuarioReactiveService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void publishUsuario(UsuarioResponse usuarioResponse) {
        usuarioSink.tryEmitNext(usuarioResponse);
    }

    public Flux<UsuarioResponse> streamUsuarios() {
        return usuarioSink.asFlux();
    }

    public Mono<Long> conteoUsuariosAsync() {
        return Mono.fromCallable(usuarioRepository::count)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
