package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.dto.usuario.UsuarioCreateRequest;
import com.tienda.ropa.backend.dto.usuario.UsuarioResponse;
import com.tienda.ropa.backend.dto.usuario.UsuarioUpdateRequest;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Clase de pruebas unitarias para UsuarioServiceImpl siguiendo el patrón AAA y Mockito
@DisplayName("UsuarioServiceImpl - Pruebas Unitarias (AAA + Mockito)")
class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioServiceImpl usuarioService;

    private UsuarioCreateRequest requestValido;

    // Configuración inicial: se mockean las dependencias antes de cada test
    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository);

        requestValido = new UsuarioCreateRequest();
        requestValido.setNombre("Carlos Mendoza");
        requestValido.setCorreo("carlos.mendoza@example.com");
        requestValido.setContrasena("pass1234");
        requestValido.setRol("CLIENTE");
    }

    // Test 1: Crear usuario válido y retornar datos
    @Test
    @DisplayName("crearUsuario_validaDatosYRetornaRespuesta")
    void crearUsuario_validaDatosYRetornaRespuesta() {
        // Arrange
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        Usuario usuarioGuardado = buildUsuario(1L, requestValido);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        // Act
        UsuarioResponse response = usuarioService.create(requestValido);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Carlos Mendoza", response.getNombre());
        assertEquals("carlos.mendoza@example.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRol());
        assertTrue(response.getActive());

        verify(usuarioRepository).save(any(Usuario.class));
    }

    // Test 2: Correo duplicado lanza excepción y no guarda
    @Test
    @DisplayName("crearUsuario_correoExiste_lanzaExcepcionYNoGuarda")
    void crearUsuario_correoExiste_lanzaExcepcionYNoGuarda() {
        // Arrange
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(true);

        // Act & Assert
        ConflictException ex = assertThrows(ConflictException.class, () ->
                usuarioService.create(requestValido));
        assertTrue(ex.getMessage().contains("correo"));
        verify(usuarioRepository, never()).save(any());
    }

    // Test 3: Desactivar usuario cambia el estado activo a false
    @Test
    @DisplayName("desactivarUsuario_usuarioExiste_desactivaCorrectamente")
    void desactivarUsuario_usuarioExiste_desactivaCorrectamente() {
        // Arrange
        Usuario usuario = buildUsuario(1L, requestValido);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        UsuarioResponse response = usuarioService.deactivate(1L);

        // Assert
        assertNotNull(response);
        assertFalse(response.getActive());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    // Test 4: Obtener usuario por ID cuando existe
    @Test
    @DisplayName("obtenerUsuario_idExiste_retornaUsuario")
    void obtenerUsuario_idExiste_retornaUsuario() {
        // Arrange
        Usuario usuario = buildUsuario(5L, requestValido);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        // Act
        UsuarioResponse response = usuarioService.getById(5L);

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("carlos.mendoza@example.com", response.getCorreo());
    }

    // Test 5: Obtener usuario por ID no existente lanza NotFoundException
    @Test
    @DisplayName("obtenerUsuario_idNoExiste_lanzaNotFoundException")
    void obtenerUsuario_idNoExiste_lanzaNotFoundException() {
        // Arrange
        Long idInexistente = 99L;
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                usuarioService.getById(idInexistente));
        assertTrue(ex.getMessage().contains("usuario con ID 99 no existe") || ex.getMessage().contains("Usuario con ID 99 no existe"));
    }

    // Test 6: Crear usuario con datos válidos y verificar con ArgumentCaptor
    @Test
    @DisplayName("crearUsuario_datosValidos_guardaUsuario_ArgumentCaptor")
    void crearUsuario_datosValidos_guardaUsuario_ArgumentCaptor() {
        // Arrange
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

        // Act
        usuarioService.create(requestValido);

        // Assert
        verify(usuarioRepository).save(captor.capture());
        Usuario usuarioGuardado = captor.getValue();
        assertEquals("Carlos Mendoza", usuarioGuardado.getNombre());
        assertEquals("carlos.mendoza@example.com", usuarioGuardado.getCorreo());
        assertEquals("CLIENTE", usuarioGuardado.getRol());
        assertTrue(usuarioGuardado.getActive());
    }

    // Test 7: Verificar el orden de ejecución con Mockito InOrder
    @Test
    @DisplayName("crearUsuario_datosValidos_verificaOrdenEjecucion")
    void crearUsuario_datosValidos_verificaOrdenEjecucion() {
        // Arrange
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        usuarioService.create(requestValido);

        // Assert
        InOrder inOrder = Mockito.inOrder(usuarioRepository);
        inOrder.verify(usuarioRepository).existsByCorreo(requestValido.getCorreo());
        inOrder.verify(usuarioRepository).save(any(Usuario.class));
    }

    // Método auxiliar para construir entidades Usuario
    private Usuario buildUsuario(Long id, UsuarioCreateRequest req) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(req.getNombre());
        u.setCorreo(req.getCorreo());
        u.setContrasena(req.getContrasena());
        u.setRol(req.getRol());
        u.setActive(true);
        return u;
    }
}