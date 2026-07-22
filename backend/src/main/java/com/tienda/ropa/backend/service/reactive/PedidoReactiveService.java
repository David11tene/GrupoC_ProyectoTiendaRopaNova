package com.tienda.ropa.backend.service.reactive;

import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class PedidoReactiveService {

    private static final double IVA = 0.15;

    private final PedidoRepository pedidoRepository;
    private final Sinks.Many<PedidoResponse> pedidoSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> processingSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public PedidoReactiveService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public void publishPedido(PedidoResponse pedidoResponse) {
        pedidoSink.tryEmitNext(pedidoResponse);
    }

    public Flux<PedidoResponse> streamPedidos() {
        return pedidoSink.asFlux();
    }

    public void publishProcessing(String message) {
        processingSink.tryEmitNext(message);
    }

    public Flux<String> streamProcessing() {
        return processingSink.asFlux();
    }

    public Mono<Double> promedioPedidosAsync() {
        return Mono.fromCallable(() -> pedidoRepository.findAll().stream()
                        .mapToDouble(pedido -> pedido.getTotal() == null ? 0.0 : pedido.getTotal())
                        .average()
                        .orElse(0.0))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public void procesarPedidosPorLotes() {
        Flux<PedidoResponse> flujoPedidos = Flux.fromIterable(pedidoRepository.findAll())
                .delayElements(Duration.ofMillis(400))
                .filter(pedido -> pedido.getTotal() >= 50.00)
                .map(pedido -> {
                    PedidoResponse response = new PedidoResponse();
                    response.setId(pedido.getId());
                    response.setUsuario(pedido.getUsuario().getNombre());
                    response.setFecha(pedido.getFecha());
                    response.setEstado(pedido.getEstado());
                    response.setTotal(pedido.getTotal());
                    return response;
                })
                .map(response -> {
                    if (response.getTotal() > 100.00) {
                        throw new RuntimeException(
                                "Pedido sospechoso, total mayor a $100: $" + response.getTotal()
                        );
                    }
                    return response;
                })
                .map(response -> {
                    double totalConIva = response.getTotal() * (1 + IVA);
                    response.setTotal(Math.round(totalConIva * 100.0) / 100.0);
                    return response;
                })
                .doOnError(error ->
                        reportProcessing("[Pedidos] Error detectado: " + error.getMessage())
                )
                .onErrorResume(error -> {
                    reportProcessing("[Pedidos] Recuperando el flujo con un pedido de respaldo...");

                    PedidoResponse respaldo = new PedidoResponse();
                    respaldo.setId(-1L);
                    respaldo.setUsuario("RECUPERACION");
                    respaldo.setEstado("ERROR_RECUPERADO");
                    respaldo.setTotal(0.0);

                    return Flux.just(respaldo);
                });

        flujoPedidos.subscribe(new PedidoBackpressureSubscriber(this::publishProcessing));
    }

    private void reportProcessing(String message) {
        System.out.println(message);
        publishProcessing(message);
    }
}
