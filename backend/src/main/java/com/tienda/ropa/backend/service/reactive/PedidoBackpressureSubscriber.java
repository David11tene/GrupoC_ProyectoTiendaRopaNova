package com.tienda.ropa.backend.service.reactive;

import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Consumer;

public class PedidoBackpressureSubscriber implements Subscriber<PedidoResponse> {

    private static final int BATCH_SIZE = 2;
    private final Consumer<String> eventConsumer;
    private Subscription subscription;
    private int processed;

    public PedidoBackpressureSubscriber() {
        this(message -> { });
    }

    public PedidoBackpressureSubscriber(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    private void report(String message) {
        System.out.println(message);
        eventConsumer.accept(message);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        this.processed = 0;
        report("[Pedidos] Suscripción iniciada. Solicitando " + BATCH_SIZE + " elementos...");
        this.subscription.request(BATCH_SIZE);
    }

    @Override
    public void onNext(PedidoResponse pedidoResponse) {
        processed++;
        report("[Pedidos] Procesando pedido #" + pedidoResponse.getId()
                + " - Total con IVA: " + pedidoResponse.getTotal());
        if (processed % BATCH_SIZE == 0) {
            report("[Pedidos] Solicitando " + BATCH_SIZE + " pedidos más...");
            subscription.request(BATCH_SIZE);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        report("[Pedidos] Error en el flujo: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        report("[Pedidos] Flujo completado.");
    }
}
