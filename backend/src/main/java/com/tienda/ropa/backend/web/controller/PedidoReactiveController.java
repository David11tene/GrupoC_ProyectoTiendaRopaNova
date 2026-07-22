package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.service.reactive.PedidoReactiveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/reactivo/pedidos")
public class PedidoReactiveController {

    private final PedidoReactiveService pedidoReactiveService;

    public PedidoReactiveController(PedidoReactiveService pedidoReactiveService) {
        this.pedidoReactiveService = pedidoReactiveService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PedidoResponse> streamPedidos() {
        return pedidoReactiveService.streamPedidos();
    }

    @GetMapping("/promedio")
    public Mono<Double> promedioPedidos() {
        return pedidoReactiveService.promedioPedidosAsync();
    }

    @GetMapping(value = "/promedio-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Double> streamPromedioPedidos() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(3))
                .flatMap(tick -> pedidoReactiveService.promedioPedidosAsync());
    }

    @GetMapping(value = "/procesamiento-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamProcesamiento() {
        return pedidoReactiveService.streamProcessing();
    }

    @PostMapping("/procesar-lotes")
    public ResponseEntity<String> procesarPedidosPorLotes() {
        pedidoReactiveService.procesarPedidosPorLotes();

        return ResponseEntity.accepted().body("Procesamiento por lotes iniciado.");
    }
}
