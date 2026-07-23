package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.DetallePedido;
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PedidoServiceImplTest {

    // Dependencias simuladas (mocks)
    private PedidoRepository pedidoRepo;
    private UsuarioRepository usuarioRepo;
    private ProductoRepository productoRepo;
    private PedidoReactiveService pedidoReactiveService;

    // Clase real a probar
    private PedidoServiceImpl pedidoService;

    @BeforeEach
    void setUp() {
        // ARRANGE: Inicialización de mocks y servicio
        pedidoRepo = Mockito.mock(PedidoRepository.class);
        usuarioRepo = Mockito.mock(UsuarioRepository.class);
        productoRepo = Mockito.mock(ProductoRepository.class);
        pedidoReactiveService = Mockito.mock(PedidoReactiveService.class);

        pedidoService = new PedidoServiceImpl(
                pedidoRepo,
                usuarioRepo,
                productoRepo,
                pedidoReactiveService
        );
    }

    @Test
    @DisplayName("Debe crear un pedido correctamente y descontar el stock")
    void create_validData_shouldSavePedidoAndDeductStock() {
        // ARRANGE
        Long idUsuario = 1L;
        Long idProducto = 10L;

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setNombre("David Teneguznay");
        usuario.setActive(true);

        Producto producto = new Producto();
        producto.setId(idProducto);
        producto.setNombre("Camisa Elegante");
        producto.setPrecio(25.0);
        producto.setStock(10);
        producto.setActive(true);

        PedidoCreateRequest.ProductoItemRequest itemRequest = new PedidoCreateRequest.ProductoItemRequest();
        itemRequest.setIdProducto(idProducto);
        itemRequest.setCantidad(2);

        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setIdUsuario(idUsuario);
        request.setProductos(List.of(itemRequest));

        Mockito.when(usuarioRepo.findById(idUsuario)).thenReturn(Optional.of(usuario));
        Mockito.when(productoRepo.findById(idProducto)).thenReturn(Optional.of(producto));
        Mockito.when(pedidoRepo.save(Mockito.any(Pedido.class)))
                .thenAnswer(invocation -> {
                    Pedido p = invocation.getArgument(0);
                    p.setId(100L);
                    return p;
                });

        // ACT
        PedidoResponse response = pedidoService.create(request).block();

        // ASSERT
        Assertions.assertNotNull(response, "La respuesta del pedido no debe ser null");
        Assertions.assertEquals(100L, response.getId(), "El ID del pedido debe coincidir");
        Assertions.assertEquals("PENDIENTE", response.getEstado(), "El estado inicial debe ser PENDIENTE");
        Assertions.assertEquals(50.0, response.getTotal(), "El total calculado debe ser 50.0");
        Assertions.assertEquals(8, producto.getStock(), "El stock debe haberse descontado a 8");

        // VERIFY INTERACCIONES
        Mockito.verify(usuarioRepo).findById(idUsuario);
        Mockito.verify(productoRepo).findById(idProducto);
        Mockito.verify(productoRepo).save(producto);
        Mockito.verify(pedidoRepo).save(Mockito.any(Pedido.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException si el usuario no existe")
    void create_userNotFound_shouldThrowNotFoundException() {
        // ARRANGE
        Long idUsuarioInvalido = 99L;
        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setIdUsuario(idUsuarioInvalido);

        Mockito.when(usuarioRepo.findById(idUsuarioInvalido)).thenReturn(Optional.empty());

        // ACT & ASSERT
        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                () -> pedidoService.create(request).block(),
                "Debe lanzar NotFoundException cuando el usuario no existe"
        );

        Assertions.assertTrue(ex.getMessage().contains("Usuario no encontrado"));
        Mockito.verifyNoInteractions(productoRepo);
        Mockito.verify(pedidoRepo, Mockito.never()).save(Mockito.any(Pedido.class));
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si el stock es insuficiente")
    void create_insufficientStock_shouldThrowConflictException() {
        // ARRANGE
        Long idUsuario = 1L;
        Long idProducto = 10L;

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setActive(true);

        Producto producto = new Producto();
        producto.setId(idProducto);
        producto.setNombre("Pantalón Jeans");
        producto.setPrecio(40.0);
        producto.setStock(1);
        producto.setActive(true);

        PedidoCreateRequest.ProductoItemRequest itemRequest = new PedidoCreateRequest.ProductoItemRequest();
        itemRequest.setIdProducto(idProducto);
        itemRequest.setCantidad(5); // Solicitado > Stock

        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setIdUsuario(idUsuario);
        request.setProductos(List.of(itemRequest));

        Mockito.when(usuarioRepo.findById(idUsuario)).thenReturn(Optional.of(usuario));
        Mockito.when(productoRepo.findById(idProducto)).thenReturn(Optional.of(producto));

        // ACT & ASSERT
        ConflictException ex = Assertions.assertThrows(
                ConflictException.class,
                () -> pedidoService.create(request).block(),
                "Debe lanzar ConflictException por stock insuficiente"
        );

        Assertions.assertTrue(ex.getMessage().contains("Stock insuficiente"));
        Mockito.verify(pedidoRepo, Mockito.never()).save(Mockito.any(Pedido.class));
    }

    @Test
    @DisplayName("Debe actualizar el estado a APROBADO correctamente")
    void updateEstado_aprobar_shouldUpdateStatus() {
        // ARRANGE
        Long idPedido = 50L;
        Pedido pedido = new Pedido();
        pedido.setId(idPedido);
        pedido.setEstado("PENDIENTE");

        Mockito.when(pedidoRepo.findById(idPedido)).thenReturn(Optional.of(pedido));
        Mockito.when(pedidoRepo.save(Mockito.any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        PedidoResponse response = pedidoService.updateEstado(idPedido, "APROBADO").block();

        // ASSERT
        Assertions.assertNotNull(response);
        Assertions.assertEquals("APROBADO", response.getEstado());
        Mockito.verify(pedidoRepo).save(pedido);
    }

    @Test
    @DisplayName("Debe reinstalar / reincorporar el stock cuando el pedido es RECHAZADO")
    void updateEstado_rechazar_shouldRestoreStockAndReturnResponse() {
        // ARRANGE
        Long idPedido = 50L;
        Producto producto = new Producto();
        producto.setId(5L);
        producto.setStock(10);

        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(3);

        Pedido pedido = new Pedido();
        pedido.setId(idPedido);
        pedido.setEstado("PENDIENTE");
        pedido.getDetalles().add(detalle);

        Mockito.when(pedidoRepo.findById(idPedido)).thenReturn(Optional.of(pedido));
        Mockito.when(pedidoRepo.save(Mockito.any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        PedidoResponse response = pedidoService.updateEstado(idPedido, "RECHAZADO").block();

        // ASSERT
        Assertions.assertNotNull(response);
        Assertions.assertEquals("RECHAZADO", response.getEstado());
        Assertions.assertEquals(13, producto.getStock(), "El stock debe haberse reintegrado a 13");
        Mockito.verify(productoRepo).save(producto);
        Mockito.verify(pedidoRepo).save(pedido);
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si se ingresa un estado inválido")
    void updateEstado_invalidStatus_shouldThrowConflictException() {
        // ARRANGE
        Long idPedido = 50L;

        // ACT & ASSERT
        ConflictException ex = Assertions.assertThrows(
                ConflictException.class,
                () -> pedidoService.updateEstado(idPedido, "ESTADO_INEXISTENTE").block(),
                "Debe lanzar ConflictException por estado inválido"
        );

        Assertions.assertTrue(ex.getMessage().contains("Estado inválido"));
        Mockito.verifyNoInteractions(pedidoRepo);
    }
}
