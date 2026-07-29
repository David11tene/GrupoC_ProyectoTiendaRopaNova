package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.dto.producto.ProductoPromedioVentaResponse;
import com.tienda.ropa.backend.service.reactive.PedidoReactiveService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Endpoints reactivos de "promedio de ventas" por producto (Lab 3 - Spring WebFlux).
 * Utilidad de negocio real: el panel admin de Productos consume estos endpoints para
 * mostrar un badge/mini-indicador "Ventas promedio" junto a cada fila de la tabla,
 * sin necesidad de recargar la página ni de un reporte aparte.
 */
@RestController
@RequestMapping("/api/reactivo/productos")
public class ProductoReactiveController {

    private final PedidoReactiveService pedidoReactiveService;

    public ProductoReactiveController(PedidoReactiveService pedidoReactiveService) {
        this.pedidoReactiveService = pedidoReactiveService;
    }

    // Promedio de venta de un producto específico, como valor puntual.
    @GetMapping("/{id}/promedio-ventas")
    public Mono<Double> promedioVentasPorProducto(@PathVariable Long id) {
        return pedidoReactiveService.promedioVentasPorProducto(id);
    }

    // Snapshot único de todos los productos con su promedio de venta actual.
    @GetMapping("/promedio-ventas")
    public Flux<ProductoPromedioVentaResponse> promedioVentasTodosLosProductos() {
        return pedidoReactiveService.promedioVentasTodosLosProductos();
    }

    // Stream SSE que refresca el promedio de venta de todos los productos cada
    // pocos segundos. Es lo que consume la tabla admin de Productos para mantener
    // el badge de "Ventas promedio" actualizado sin recargar la página.
    @GetMapping(value = "/promedio-ventas/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductoPromedioVentaResponse> streamPromedioVentasTodosLosProductos() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .flatMap(tick -> pedidoReactiveService.promedioVentasTodosLosProductos());
    }
}
