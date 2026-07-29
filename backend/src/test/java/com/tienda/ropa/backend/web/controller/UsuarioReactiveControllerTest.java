package com.tienda.ropa.backend.web.controller;

import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.repository.PedidoRepository;
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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("UsuarioReactiveController - WebTestClient")
class UsuarioReactiveControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Necesario para limpiar pedidos ANTES de borrar usuarios: si queda un
    // pedido apuntando a un usuario (por ejemplo, de otra clase de test que
    // corrió antes y comparte el mismo contexto/BD H2), el deleteAll() de
    // usuarios revienta con ConstraintViolationException por la FK.
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
        usuarioRepository.deleteAll();

        Usuario u1 = new Usuario();
        u1.setNombre("cliente-uno");
        u1.setCorreo("cliente-uno@novatienda.com");
        u1.setContrasena("clave");
        u1.setRol("CLIENTE");
        u1.setActive(true);
        usuarioRepository.save(u1);

        Usuario u2 = new Usuario();
        u2.setNombre("cliente-dos");
        u2.setCorreo("cliente-dos@novatienda.com");
        u2.setContrasena("clave");
        u2.setRol("CLIENTE");
        u2.setActive(true);
        usuarioRepository.save(u2);
    }

    @Test
    @DisplayName("GET /api/reactivo/usuarios/conteo devuelve la cantidad real de usuarios")
    void conteoUsuarios_devuelveCantidadCorrecta() {
        webTestClient.get()
                .uri("/api/reactivo/usuarios/conteo")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .value(conteo -> assertEquals(2L, conteo));
    }
}