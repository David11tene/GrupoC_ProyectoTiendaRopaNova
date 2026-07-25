package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PedidoRepository - Tests JPA")
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioPrueba = new Usuario();
        usuarioPrueba.setNombre("Carlos Andrade");
        usuarioPrueba.setCorreo("carlos@gmail.com");
        usuarioPrueba.setContrasena("123456");
        usuarioPrueba.setRol("CLIENTE");
        usuarioPrueba.setActive(true);
        usuarioPrueba = usuarioRepository.save(usuarioPrueba);
    }

    @Test
    @DisplayName("Debe guardar y buscar pedidos por usuario")
    void debeBuscarPedidosPorUsuario() {
        Pedido p1 = new Pedido();
        p1.setUsuario(usuarioPrueba);
        p1.setFecha(LocalDate.now());
        p1.setEstado("PENDIENTE");
        p1.setTotal(45.0);
        pedidoRepository.save(p1);

        List<Pedido> pedidos = pedidoRepository.findByUsuario(usuarioPrueba);

        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.get(0).getUsuario().getCorreo()).isEqualTo("carlos@gmail.com");
    }

    @Test
    @DisplayName("Debe buscar pedidos por estado")
    void debeBuscarPedidosPorEstado() {
        Pedido p1 = new Pedido();
        p1.setUsuario(usuarioPrueba);
        p1.setFecha(LocalDate.now());
        p1.setEstado("COMPLETADO");
        p1.setTotal(100.0);
        pedidoRepository.save(p1);

        List<Pedido> completados = pedidoRepository.findByEstado("COMPLETADO");
        List<Pedido> pendientes = pedidoRepository.findByEstado("PENDIENTE");

        assertThat(completados).hasSize(1);
        assertThat(pendientes).isEmpty();
    }
}
