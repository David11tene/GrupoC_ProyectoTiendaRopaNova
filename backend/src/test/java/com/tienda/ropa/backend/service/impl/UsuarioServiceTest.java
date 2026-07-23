package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.usuario.UsuarioCreateRequest;
import com.tienda.ropa.backend.dto.usuario.UsuarioResponse;
import com.tienda.ropa.backend.dto.usuario.UsuarioUpdateRequest;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.service.reactive.UsuarioReactiveService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioReactiveService usuarioReactiveService;
    private UsuarioServiceImpl usuarioService;

    private UsuarioCreateRequest requestValido;

    @BeforeEach
    public void setUp() {
        // Arrange general para cada prueba
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        usuarioReactiveService = Mockito.mock(UsuarioReactiveService.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository, usuarioReactiveService);

        requestValido = new UsuarioCreateRequest();
        requestValido.setNombre("Carlos Mendoza");
        requestValido.setCorreo("carlos.mendoza@example.com");
        requestValido.setContrasena("pass1234");
        requestValido.setRol("CLIENTE");
    }

    @Test
    void create_validData_shouldSaveAndReturnResponse() {
        // Arrange
        Mockito.when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        Usuario usuarioGuardado = buildUsuario(1L, requestValido);
        Mockito.when(usuarioRepository.save(ArgumentMatchers.any(Usuario.class))).thenReturn(usuarioGuardado);

        // Act
        UsuarioResponse response = usuarioService.create(requestValido).block();

        // Assert
        Assertions.assertNotNull(response, "La respuesta no debe ser nula");
        Assertions.assertEquals("Carlos Mendoza", response.getNombre());
        Assertions.assertEquals("carlos.mendoza@example.com", response.getCorreo());
        Assertions.assertEquals("CLIENTE", response.getRol());
        Assertions.assertTrue(response.getActive());

        Mockito.verify(usuarioRepository).existsByCorreo(requestValido.getCorreo());
        Mockito.verify(usuarioRepository).save(ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void create_emailAlreadyExists_shouldThrowException_andNotSave() {
        // Arrange
        Mockito.when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(true);

        // Act + Assert
        ConflictException ex = Assertions.assertThrows(ConflictException.class, () ->
                usuarioService.create(requestValido).block());

        Assertions.assertTrue(ex.getMessage().contains("ya está registrado"));
        Mockito.verify(usuarioRepository, Mockito.never()).save(ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void update_validEmail_shouldUpdateAndReturnResponse() {
        // Arrange
        Long usuarioId = 1L;
        Usuario existente = buildUsuario(usuarioId, requestValido);
        String nuevoCorreo = "carlos.nuevo@example.com";

        UsuarioUpdateRequest updateRequest = new UsuarioUpdateRequest();
        updateRequest.setCorreo(nuevoCorreo);

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
        Mockito.when(usuarioRepository.existsByCorreo(nuevoCorreo)).thenReturn(false);
        Mockito.when(usuarioRepository.save(ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // Act
        UsuarioResponse response = usuarioService.update(usuarioId, updateRequest).block();

        // Assert
        Assertions.assertNotNull(response);
        Assertions.assertEquals(nuevoCorreo, response.getCorreo());
        Mockito.verify(usuarioRepository).existsByCorreo(nuevoCorreo);
        Mockito.verify(usuarioRepository).save(existente);
    }

    @Test
    void update_duplicateEmail_shouldThrowException() {
        // Arrange
        Long usuarioId = 1L;
        Usuario existente = buildUsuario(usuarioId, requestValido);
        String correoDuplicado = "otro.usuario@example.com";

        UsuarioUpdateRequest updateRequest = new UsuarioUpdateRequest();
        updateRequest.setCorreo(correoDuplicado);

        Mockito.when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
        Mockito.when(usuarioRepository.existsByCorreo(correoDuplicado)).thenReturn(true);

        // Act + Assert
        ConflictException ex = Assertions.assertThrows(ConflictException.class, () ->
                usuarioService.update(usuarioId, updateRequest).block());

        Assertions.assertTrue(ex.getMessage().contains("ya está registrado en otro usuario"));
        Mockito.verify(usuarioRepository, Mockito.never()).save(ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void deactivate_validId_shouldDeactivateUser() {
        // Arrange
        Usuario usuario = buildUsuario(1L, requestValido);
        Mockito.when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        Mockito.when(usuarioRepository.save(ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        // Act
        UsuarioResponse response = usuarioService.deactivate(1L).block();

        // Assert
        Assertions.assertFalse(response.getActive());
        Mockito.verify(usuarioRepository).save(ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void getById_userFound_shouldReturnUserResponse() {
        // Arrange
        Usuario usuario = buildUsuario(5L, requestValido);
        Mockito.when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        // Act
        UsuarioResponse r = usuarioService.getById(5L).block();

        // Assert
        Assertions.assertNotNull(r);
        Assertions.assertEquals(5L, r.getId());
        Assertions.assertEquals(requestValido.getCorreo(), r.getCorreo());
    }

    @Test
    void getById_userNotFound_shouldThrowException() {
        // Arrange
        Mockito.when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        NotFoundException ex = Assertions.assertThrows(NotFoundException.class, () ->
                usuarioService.getById(99L).block());

        Assertions.assertTrue(ex.getMessage().contains("Usuario"));
    }

    @Test
    void update_userNotFound_shouldThrowException() {
        // Arrange
        Mockito.when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        UsuarioUpdateRequest upd = new UsuarioUpdateRequest();
        upd.setNombre("No Existe");

        // Act + Assert
        Assertions.assertThrows(NotFoundException.class, () ->
                usuarioService.update(99L, upd).block());
    }

    @Test
    void list_shouldReturnUserList() {
        // Arrange
        Usuario u1 = buildUsuario(10L, requestValido);
        Usuario u2 = buildUsuario(11L, requestValido);
        u2.setNombre("Otro");

        Mockito.when(usuarioRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        // Act
        List<UsuarioResponse> lista = usuarioService.list().collectList().block();

        // Assert
        Assertions.assertNotNull(lista);
        Assertions.assertEquals(2, lista.size());
        Mockito.verify(usuarioRepository).findAll();
    }

    @Test
    void searchByName_shouldReturnPagedResults() {
        // Arrange
        Usuario u1 = buildUsuario(20L, requestValido);
        u1.setNombre("Carlos One");
        Usuario u2 = buildUsuario(21L, requestValido);
        u2.setNombre("Carlos Two");

        Mockito.when(usuarioRepository.findByNombreContainingIgnoreCase(
                ArgumentMatchers.eq("carlos"),
                ArgumentMatchers.any()
        )).thenReturn(new PageImpl<>(List.of(u1, u2)));

        // Act
        Page<UsuarioResponse> page = usuarioService.searchByName("carlos", 0, 10).block();

        // Assert
        Assertions.assertNotNull(page);
        Assertions.assertEquals(2, page.getTotalElements());
    }

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