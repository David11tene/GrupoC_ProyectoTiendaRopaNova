package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.dto.categoria.CategoriaCreateRequest;
import com.tienda.ropa.backend.dto.categoria.CategoriaResponse;
import com.tienda.ropa.backend.dto.categoria.CategoriaUpdateRequest;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CategoriaServiceImpl - Pruebas Unitarias (AAA + Mockito)")
public class CategoriaServiceTest {

    private CategoriaRepository categoriaRepository;
    private CategoriaServiceImpl categoriaService;

    @BeforeEach
    void setUp() {
        categoriaRepository = mock(CategoriaRepository.class);
        categoriaService = new CategoriaServiceImpl(categoriaRepository);
    }

    @Test
    @DisplayName("Debe crear una categoría correctamente si el nombre no existe")
    void create_nombreNuevo_creaCategoriaExitosamente() {
        // Arrange
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Deportiva");

        when(categoriaRepository.existsByNombre("Deportiva")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> {
            Categoria c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        // Act
        CategoriaResponse response = categoriaService.create(request).block();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Deportiva", response.getNombre());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si la categoría ya existe")
    void create_nombreDuplicado_lanzaConflictException() {
        // Arrange
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Deportiva");

        when(categoriaRepository.existsByNombre("Deportiva")).thenReturn(true);

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                categoriaService.create(request).block());
        assertEquals("La categoría ya existe", ex.getMessage());
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener categoría por ID exitosamente")
    void getById_idExistente_retornaCategoria() {
        // Arrange
        Categoria cat = new Categoria();
        cat.setId(5L);
        cat.setNombre("Accesorios");

        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(cat));

        // Act
        CategoriaResponse response = categoriaService.getById(5L).block();

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Accesorios", response.getNombre());
    }

    @Test
    @DisplayName("Debe lanzar NotFoundException cuando la categoría no existe por ID")
    void getById_idInexistente_lanzaNotFoundException() {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                categoriaService.getById(99L).block());
        assertEquals("Categoría no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Debe listar todas las categorías")
    void list_retornaListaCategorias() {
        // Arrange
        Categoria c1 = new Categoria();
        c1.setId(1L);
        c1.setNombre("Formal");

        Categoria c2 = new Categoria();
        c2.setId(2L);
        c2.setNombre("Informal");

        when(categoriaRepository.findAll()).thenReturn(List.of(c1, c2));

        // Act
        List<CategoriaResponse> list = categoriaService.list().collectList().block();

        // Assert
        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Debe actualizar categoría existente")
    void update_idExistente_actualizaCategoria() {
        // Arrange
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("Antiguo Nombre");

        CategoriaUpdateRequest request = new CategoriaUpdateRequest();
        request.setNombre("Nuevo Nombre");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(c));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoriaResponse response = categoriaService.update(1L, request).block();

        // Assert
        assertNotNull(response);
        assertEquals("Nuevo Nombre", response.getNombre());
        verify(categoriaRepository).save(c);
    }
}
