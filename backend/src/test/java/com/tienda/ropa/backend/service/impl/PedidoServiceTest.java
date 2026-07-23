package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.repository.PedidoRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.service.reactive.PedidoReactiveService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Clase de pruebas unitarias para PedidoServiceImpl siguiendo el patrón AAA y Mockito
@DisplayName("PedidoServiceImpl - Pruebas Unitarias (AAA + Mockito)")
public class PedidoServiceTest {

    private PedidoRepository pedidoRepository;
    private UsuarioRepository usuarioRepository;
    private ProductoRepository productoRepository;
    private PedidoReactiveService pedidoReactiveService;
    private PedidoServiceImpl pedidoService;

    private Usuario usuarioValido;
    private Producto productoValido;
    private PedidoCreateRequest requestValido;

    // Configuración inicial: se mockean las dependencias antes de cada test
    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        productoRepository = mock(ProductoRepository.class);
        pedidoReactiveService = mock(PedidoReactiveService.class);

        pedidoService = new PedidoServiceImpl(
                pedidoRepository,
                usuarioRepository,
                productoRepository,
                pedidoReactiveService
        );

        // Usuario válido
        usuarioValido = new Usuario();
        usuarioValido.setId(1L);
        usuarioValido.setNombre("Maria Lopez");
        usuarioValido.setCorreo("maria@gmail.com");
        usuarioValido.setActive(true);

        // Producto válido
        Categoria cat = new Categoria();
        cat.setId(100L);
        cat.setNombre("Camisas");

        productoValido = new Producto();
        productoValido.setId(10L);
        productoValido.setNombre("Camisa Oxford");
        productoValido.setPrecio(29.99);
        productoValido.setStock(20);
        productoValido.setActive(true);
        productoValido.setCategoria(cat);

        // Request de pedido válido
        PedidoCreateRequest.ProductoItemRequest item = new PedidoCreateRequest.ProductoItemRequest();
        item.setIdProducto(10L);
        item.setCantidad(2);

        requestValido = new PedidoCreateRequest();
        requestValido.setIdUsuario(1L);
        requestValido.setProductos(List.of(item));
    }

    // Test 1: Crear pedido válido y retornar respuesta
    @Test
    @DisplayName("crearPedido_validaDatosYRetornaRespuesta")
    void crearPedido_validaDatosYRetornaRespuesta() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoValido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(50L);
            return p;
        });

        // Act
        PedidoResponse response = pedidoService.create(requestValido);

        // Assert
        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Maria Lopez", response.getUsuario());
        assertEquals("PENDIENTE", response.getEstado());
        assertEquals(59.98, response.getTotal(), 0.001);

        verify(pedidoRepository).save(any(Pedido.class));
        verify(pedidoReactiveService).publishPedido(any(PedidoResponse.class));
    }

    // Test 2: Usuario no encontrado lanza excepción
    @Test
    @DisplayName("crearPedido_usuarioNoEncontrado_lanzaExcepcion")
    void crearPedido_usuarioNoEncontrado_lanzaExcepcion() {
        // Arrange
        Long idInexistente = 99L;
        requestValido.setIdUsuario(idInexistente);
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                pedidoService.create(requestValido));
        assertEquals("Usuario no encontrado con id: 99", ex.getMessage());
        verify(pedidoRepository, never()).save(any());
    }

    // Test 3: Usuario desactivado lanza excepción y no ejecuta dependencias
    @Test
    @DisplayName("crearPedido_usuarioDesactivado_lanzaExcepcionYNoEjecutaDependencias")
    void crearPedido_usuarioDesactivado_lanzaExcepcionYNoEjecutaDependencias() {
        // Arrange
        usuarioValido.setActive(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                pedidoService.create(requestValido));
        assertEquals("El usuario está desactivado y no puede hacer pedidos.", ex.getMessage());
        verifyNoInteractions(productoRepository, pedidoReactiveService);
        verify(pedidoRepository, never()).save(any());
    }

    // Test 4: Producto desactivado lanza excepción y no guarda
    @Test
    @DisplayName("crearPedido_productoDesactivado_lanzaExcepcionYNoGuarda")
    void crearPedido_productoDesactivado_lanzaExcepcionYNoGuarda() {
        // Arrange
        productoValido.setActive(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoValido));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                pedidoService.create(requestValido));
        assertTrue(ex.getMessage().contains("no está disponible"));
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(pedidoReactiveService);
    }

    // Test 5: Stock insuficiente lanza excepción y no guarda
    @Test
    @DisplayName("crearPedido_stockInsuficiente_lanzaExcepcionYNoGuarda")
    void crearPedido_stockInsuficiente_lanzaExcepcionYNoGuarda() {
        // Arrange
        productoValido.setStock(1); // Solicitados: 2
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoValido));

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                pedidoService.create(requestValido));
        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(pedidoReactiveService);
    }

    // Test 6: Crear pedido con datos válidos y verificar con ArgumentCaptor
    @Test
    @DisplayName("crearPedido_datosValidos_guardaPedido_ArgumentCaptor")
    void crearPedido_datosValidos_guardaPedido_ArgumentCaptor() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoValido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);

        // Act
        pedidoService.create(requestValido);

        // Assert
        verify(pedidoRepository).save(captor.capture());
        Pedido pedidoGuardado = captor.getValue();
        assertEquals(usuarioValido, pedidoGuardado.getUsuario());
        assertEquals("PENDIENTE", pedidoGuardado.getEstado());
        assertEquals(59.98, pedidoGuardado.getTotal(), 0.001);
        assertEquals(1, pedidoGuardado.getDetalles().size());
        assertEquals(18, productoValido.getStock()); // Stock descontado de 20 a 18
    }

    // Test 7: Verificar el orden de ejecución con Mockito InOrder
    @Test
    @DisplayName("crearPedido_datosValidos_verificaOrdenEjecucion")
    void crearPedido_datosValidos_verificaOrdenEjecucion() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioValido));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoValido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pedidoService.create(requestValido);

        // Assert
        InOrder inOrder = Mockito.inOrder(usuarioRepository, productoRepository, pedidoRepository, pedidoReactiveService);
        inOrder.verify(usuarioRepository).findById(1L);
        inOrder.verify(productoRepository).findById(10L);
        inOrder.verify(productoRepository).save(productoValido);
        inOrder.verify(pedidoRepository).save(any(Pedido.class));
        inOrder.verify(pedidoReactiveService).publishPedido(any(PedidoResponse.class));
    }
}
