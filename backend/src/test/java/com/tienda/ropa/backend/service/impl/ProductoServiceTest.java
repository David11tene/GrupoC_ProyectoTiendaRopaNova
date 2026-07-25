package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.dto.producto.ProductoCreateRequest;
import com.tienda.ropa.backend.dto.producto.ProductoResponse;
import com.tienda.ropa.backend.dto.producto.ProductoUpdateRequest;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import com.tienda.ropa.backend.web.advice.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ProductoServiceImpl - Pruebas Unitarias (AAA + Mockito)")
public class ProductoServiceTest {

    private ProductoRepository productoRepository;
    private CategoriaRepository categoriaRepository;
    private ProductoServiceImpl productoService;

    private Categoria categoriaValida;
    private Producto productoValido;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        categoriaRepository = mock(CategoriaRepository.class);

        productoService = new ProductoServiceImpl(
                productoRepository,
                categoriaRepository
        );

        categoriaValida = new Categoria();
        categoriaValida.setId(10L);
        categoriaValida.setNombre("Chaquetas");

        productoValido = new Producto();
        productoValido.setId(1L);
        productoValido.setNombre("Chaqueta Cuero");
        productoValido.setPrecio(99.99);
        productoValido.setStock(15);
        productoValido.setActive(true);
        productoValido.setCategoria(categoriaValida);
    }

    @Test
    @DisplayName("Debe crear un producto correctamente si la categoría existe")
    void create_categoriaExistente_creaProducto() {
        // Arrange
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setNombre("Chaqueta Cuero");
        request.setPrecio(99.99);
        request.setStock(15);
        request.setCategoriaId(10L);

        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoriaValida));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act
        ProductoResponse response = productoService.create(request).block();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Chaqueta Cuero", response.getNombre());
        assertEquals("Chaquetas", response.getCategoria());
        assertTrue(response.getActive());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException al crear producto con categoría inexistente")
    void create_categoriaInexistente_lanzaNotFoundException() {
        // Arrange
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(99L);

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                productoService.create(request).block());
        assertEquals("Categoría no encontrada", ex.getMessage());
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe desactivar un producto existente")
    void deactivate_productoExistente_cambiaEstadoAFalse() {
        // Arrange
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoValido));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductoResponse response = productoService.deactivate(1L).block();

        // Assert
        assertNotNull(response);
        assertFalse(response.getActive());
        verify(productoRepository).save(productoValido);
    }

    @Test
    @DisplayName("Debe actualizar precio y stock de producto existente")
    void update_datosValidos_actualizaProducto() {
        // Arrange
        ProductoUpdateRequest request = new ProductoUpdateRequest();
        request.setPrecio(89.99);
        request.setStock(20);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoValido));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductoResponse response = productoService.update(1L, request).block();

        // Assert
        assertNotNull(response);
        assertEquals(89.99, response.getPrecio());
        assertEquals(20, response.getStock());
    }
}
