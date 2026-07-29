package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.DetallePedido;
import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.producto.ProductoPromedioVentaResponse;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.repository.DetallePedidoRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del nuevo endpoint reactivo de "promedio de ventas por producto" (Lab 3),
 * el mismo que alimenta el badge de "Ventas promedio" en el panel admin de Productos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
// Evita compartir la BD H2 en memoria con las otras clases de test reactivas.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("ProductoReactiveController - WebTestClient + StepVerifier")
class ProductoReactiveControllerTest {

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
    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    private WebTestClient webTestClient;
    private Producto productoConVentas;
    private Producto productoSinVentas;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        detallePedidoRepository.deleteAll();
        pedidoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNombre("Pantalones");
        categoria = categoriaRepository.save(categoria);

        productoConVentas = new Producto();
        productoConVentas.setNombre("Jean Slim");
        productoConVentas.setPrecio(30.0);
        productoConVentas.setStock(15);
        productoConVentas.setActive(true);
        productoConVentas.setCategoria(categoria);
        productoConVentas = productoRepository.save(productoConVentas);

        productoSinVentas = new Producto();
        productoSinVentas.setNombre("Jean Recto");
        productoSinVentas.setPrecio(28.0);
        productoSinVentas.setStock(10);
        productoSinVentas.setActive(true);
        productoSinVentas.setCategoria(categoria);
        productoSinVentas = productoRepository.save(productoSinVentas);

        Usuario usuario = new Usuario();
        usuario.setNombre("cliente-ventas");
        usuario.setCorreo("cliente-ventas@novatienda.com");
        usuario.setContrasena("clave");
        usuario.setRol("CLIENTE");
        usuario.setActive(true);
        usuario = usuarioRepository.save(usuario);

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDate.now());
        pedido.setTotal(90.0);
        pedido.setEstado("APROBADO");
        pedido = pedidoRepository.save(pedido);

        // Dos ventas del mismo producto: subtotales 30.0 y 60.0 -> promedio 45.0
        DetallePedido d1 = new DetallePedido();
        d1.setPedido(pedido);
        d1.setProducto(productoConVentas);
        d1.setCantidad(1);
        d1.setSubtotal(30.0);
        detallePedidoRepository.save(d1);

        DetallePedido d2 = new DetallePedido();
        d2.setPedido(pedido);
        d2.setProducto(productoConVentas);
        d2.setCantidad(2);
        d2.setSubtotal(60.0);
        detallePedidoRepository.save(d2);
    }

    @Test
    @DisplayName("GET /promedio-ventas/{id} calcula correctamente el promedio de un producto vendido")
    void promedioVentasPorProducto_conVentas_calculaPromedioCorrecto() {
        webTestClient.get()
                .uri("/api/reactivo/productos/{id}/promedio-ventas", productoConVentas.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .value(promedio -> assertEquals(45.0, promedio, 0.001));
    }

    @Test
    @DisplayName("GET /promedio-ventas/{id} devuelve 0.0 para un producto sin ventas registradas")
    void promedioVentasPorProducto_sinVentas_devuelveCero() {
        webTestClient.get()
                .uri("/api/reactivo/productos/{id}/promedio-ventas", productoSinVentas.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .value(promedio -> assertEquals(0.0, promedio, 0.001));
    }

    @Test
    @DisplayName("GET /promedio-ventas incluye a todos los productos, con y sin ventas")
    void promedioVentasTodosLosProductos_incluyeTodos() {
        Flux<ProductoPromedioVentaResponse> flujo = webTestClient.get()
                .uri("/api/reactivo/productos/promedio-ventas")
                .exchange()
                .expectStatus().isOk()
                .returnResult(ProductoPromedioVentaResponse.class)
                .getResponseBody();

        StepVerifier.create(flujo.collectList())
                .assertNext(lista -> {
                    assertEquals(2, lista.size());
                    List<Long> ids = lista.stream().map(ProductoPromedioVentaResponse::getProductoId).toList();
                    assertTrue(ids.contains(productoConVentas.getId()));
                    assertTrue(ids.contains(productoSinVentas.getId()));
                })
                .verifyComplete();
    }
}