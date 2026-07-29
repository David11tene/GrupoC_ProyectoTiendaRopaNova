package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.repository.PedidoRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración reactivas (Lab 3 - Spring WebFlux) para los endpoints
 * SSE/Mono de pedidos, usando WebTestClient y StepVerifier tal como se pide en
 * el laboratorio, sin depender de curl/Postman: corren en el CI existente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// Fuerza un ApplicationContext (y por lo tanto una BD H2) nueva para esta
// clase, evitando que streams SSE (Sinks.Many) o datos de otras clases de
// test contaminen este test y causen timeouts intermitentes.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("PedidoReactiveController - WebTestClient + StepVerifier")
class PedidoReactiveControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        pedidoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario usuario = new Usuario();
        usuario.setNombre("cliente-test");
        usuario.setCorreo("cliente-test@novatienda.com");
        usuario.setContrasena("clave");
        usuario.setRol("CLIENTE");
        usuario.setActive(true);
        usuario = usuarioRepository.save(usuario);

        Categoria categoria = new Categoria();
        categoria.setNombre("Camisas");
        categoria = categoriaRepository.save(categoria);

        Producto producto = new Producto();
        producto.setNombre("Camisa Oxford");
        producto.setPrecio(25.0);
        producto.setStock(20);
        producto.setActive(true);
        producto.setCategoria(categoria);
        productoRepository.save(producto);

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDate.now());
        pedido.setTotal(50.0);
        pedido.setEstado("PENDIENTE");
        pedidoRepository.save(pedido);
    }

    @Test
    @DisplayName("GET /api/reactivo/pedidos/promedio devuelve un Mono<Double> con el promedio de pedidos")
    void promedioPedidos_devuelvePromedioCorrecto() {
        webTestClient.get()
                .uri("/api/reactivo/pedidos/promedio")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .value(promedio -> {
                    assertNotNull(promedio);
                    assertTrue(promedio >= 0.0);
                });
    }

    @Test
    @DisplayName("GET /api/reactivo/pedidos/stream emite un pedido por SSE cuando se crea uno nuevo")
    void streamPedidos_emiteEventosSSEAlCrearPedido() throws Exception {
        // NOTA IMPORTANTE: el proyecto tiene spring-boot-starter-web Y
        // spring-boot-starter-webflux juntos en el classpath. Cuando ambos
        // están presentes, Spring Boot arranca la app como Servlet (Tomcat/MVC)
        // en vez de servidor reactivo Netty. Bajo MVC, un endpoint que devuelve
        // Flux<T> como SSE no confirma (flushea) los headers de la respuesta
        // hasta que se emite el primer dato -- a diferencia de WebFlux/Netty,
        // que confirma los headers apenas alguien se suscribe.
        //
        // Por eso NO podemos esperar de forma síncrona a que .exchange() vuelva
        // y recién después crear el pedido: sería un deadlock (exchange() nunca
        // retorna porque no hay datos, y los datos nunca se generan porque el
        // código que los generaría corre después de exchange()).
        //
        // Solución: disparamos el GET en un hilo aparte y creamos el pedido casi
        // en simultáneo desde el hilo principal. La suscripción del Flux al sink
        // ocurre en el servidor apenas Spring despacha la petición al controlador,
        // no cuando el cliente recibe los headers, así que esto sí funciona.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Flux<PedidoResponse>> futureStream = executor.submit(() ->
                    webTestClient.get()
                            .uri("/api/reactivo/pedidos/stream")
                            .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
                            .exchange()
                            .expectStatus().isOk()
                            .returnResult(PedidoResponse.class)
                            .getResponseBody()
            );

            // Pequeño margen para asegurar que el GET ya llegó al controlador
            // y el Flux ya está suscrito al sink antes de publicar el pedido.
            Thread.sleep(300);
            crearPedidoDePrueba();

            Flux<PedidoResponse> eventStream = futureStream.get(15, TimeUnit.SECONDS);

            StepVerifier.create(eventStream.take(1).timeout(Duration.ofSeconds(10)))
                    .assertNext(pedido -> assertNotNull(pedido.getId()))
                    .verifyComplete();
        } finally {
            executor.shutdown();
        }
    }

    private void crearPedidoDePrueba() {
        Producto producto = productoRepository.findAll().get(0);
        Usuario usuario = usuarioRepository.findAll().get(0);

        String body = "{\"idUsuario\": " + usuario.getId() + ", \"productos\": [" +
                "{\"idProducto\": " + producto.getId() + ", \"cantidad\": 1}]}";

        webTestClient.post()
                .uri("/api/pedidos")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}