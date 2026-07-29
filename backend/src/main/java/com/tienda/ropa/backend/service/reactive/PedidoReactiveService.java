package com.tienda.ropa.backend.service.reactive;

import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.dto.detallepedido.DetallePedidoResponse;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.dto.producto.ProductoPromedioVentaResponse;
import com.tienda.ropa.backend.repository.DetallePedidoRepository;
import com.tienda.ropa.backend.repository.PedidoRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoReactiveService {

    private static final double IVA = 0.15;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final Sinks.Many<PedidoResponse> pedidoSink =
            Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> processingSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public PedidoReactiveService(PedidoRepository pedidoRepository,
                                  ProductoRepository productoRepository,
                                  DetallePedidoRepository detallePedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
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

    // NOTA DE DISEÑO: Spring Data JPA (a través de Hibernate/JDBC) es bloqueante por naturaleza.
    // No existe un driver reactivo para MySQL/H2 en este proyecto (usaríamos R2DBC para eso),
    // así que en lugar de bloquear un hilo del event-loop de Netty/WebFlux, envolvemos cada
    // llamada al repositorio con Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()).
    // boundedElastic() usa un pool de hilos dedicado y acotado para trabajo bloqueante, dejando
    // libres los hilos reactivos para el resto de la aplicación. Esto es una decisión de diseño
    // consciente (un "reactive wrapper" sobre una capa de persistencia servlet/JPA), no un error
    // ni un uso incorrecto de WebFlux.
    public Mono<Double> promedioPedidosAsync() {
        return Mono.fromCallable(() -> pedidoRepository.findAll().stream()
                        .mapToDouble(pedido -> pedido.getTotal() == null ? 0.0 : pedido.getTotal())
                        .average()
                        .orElse(0.0))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Promedio de venta (subtotal por línea de pedido) de un producto específico.
     * Útil de negocio: permite al admin ver qué tan bien vende un producto en tiempo real,
     * sin tener que abrir un reporte aparte. Devuelve 0.0 si el producto nunca se ha vendido.
     * Ver nota de diseño de Schedulers.boundedElastic() en promedioPedidosAsync().
     */
    public Mono<Double> promedioVentasPorProducto(Long productoId) {
        return Mono.fromCallable(() -> detallePedidoRepository.averageSubtotalByProductoId(productoId))
                .map(promedio -> promedio == null ? 0.0 : promedio)
                .defaultIfEmpty(0.0)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Promedio de venta de TODOS los productos activos, pensado para alimentar la tabla
     * admin de Productos (badge/mini-indicador "Ventas promedio" por fila) vía SSE o polling.
     * Los productos sin ventas registradas aparecen con promedio 0.0 y cantidadVentas 0
     * en lugar de quedar fuera de la lista.
     */
    public Flux<ProductoPromedioVentaResponse> promedioVentasTodosLosProductos() {
        return Mono.fromCallable(productoRepository::findAll)
                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toPromedioVentaResponse);
    }

    private ProductoPromedioVentaResponse toPromedioVentaResponse(Producto producto) {
        List<com.tienda.ropa.backend.domain.DetallePedido> ventas =
                detallePedidoRepository.findByProductoId(producto.getId());
        double promedio = ventas.stream()
                .mapToDouble(d -> d.getSubtotal() == null ? 0.0 : d.getSubtotal())
                .average()
                .orElse(0.0);
        return new ProductoPromedioVentaResponse(
                producto.getId(),
                producto.getNombre(),
                promedio,
                (long) ventas.size()
        );
    }

    public void procesarPedidosPorLotes() {
        procesarPedidosPorLotesConDemanda(2, 400);
    }

    public void procesarPedidosPorLotesConDemanda(int batchSize, long delayMs) {
        reportProcessing("[Backpressure] Configurando suscripción con demanda de batchSize=" + batchSize);

        Flux<PedidoResponse> flujoPedidos = Flux.fromIterable(pedidoRepository.findAll())
                .delayElements(Duration.ofMillis(Math.max(100, delayMs)))
                .limitRate(Math.max(1, batchSize))
                .filter(pedido -> pedido.getTotal() != null && pedido.getTotal() >= 10.00)
                .map(this::toResponse)
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

    public Flux<PedidoResponse> streamPedidosConBackpressure(int batchSize, long delayMs) {
        reportProcessing("[Backpressure] Iniciando SSE streaming con demanda client-side: batchSize = " + batchSize + ", delay = " + delayMs + "ms");

        return Mono.fromCallable(pedidoRepository::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse)
                .delayElements(Duration.ofMillis(Math.max(100, delayMs)))
                .limitRate(Math.max(1, batchSize))
                .doOnNext(p -> reportProcessing("[Backpressure SSE] Emitiendo pedido #" + p.getId() + " (Demand rate = " + batchSize + ")"))
                .doOnComplete(() -> reportProcessing("[Backpressure SSE] Transmisión completada."))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private PedidoResponse toResponse(Pedido p) {
        PedidoResponse r = new PedidoResponse();
        r.setId(p.getId());
        r.setUsuario(p.getUsuario() != null ? p.getUsuario().getNombre() : "Desconocido");
        r.setFecha(p.getFecha());
        r.setTotal(p.getTotal() != null ? p.getTotal() : 0.0);
        r.setEstado(p.getEstado());

        if (p.getDetalles() != null) {
            List<DetallePedidoResponse> detallesResp = p.getDetalles().stream().map(d -> {
                DetallePedidoResponse dr = new DetallePedidoResponse();
                dr.setId(d.getId());
                dr.setProducto(d.getProducto() != null ? d.getProducto().getNombre() : "Producto sin nombre");
                dr.setCantidad(d.getCantidad());
                dr.setSubtotal(d.getSubtotal());
                if (d.getProducto() != null && d.getProducto().getPrecio() != null) {
                    dr.setPrecioUnitario(d.getProducto().getPrecio());
                } else if (d.getCantidad() != null && d.getCantidad() > 0 && d.getSubtotal() != null) {
                    dr.setPrecioUnitario(d.getSubtotal() / d.getCantidad());
                }
                return dr;
            }).collect(Collectors.toList());
            r.setDetalles(detallesResp);
        }
        return r;
    }

    private void reportProcessing(String message) {
        System.out.println(message);
        publishProcessing(message);
    }
}