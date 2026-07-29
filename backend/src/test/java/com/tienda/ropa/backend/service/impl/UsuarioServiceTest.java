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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UsuarioServiceImpl - Pruebas Unitarias (AAA + Mockito)")
public class UsuarioServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioReactiveService usuarioReactiveService;
    private UsuarioServiceImpl usuarioService;

    private UsuarioCreateRequest requestValido;

    @BeforeEach
    public void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioReactiveService = mock(UsuarioReactiveService.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository, usuarioReactiveService);

        requestValido = new UsuarioCreateRequest();
        requestValido.setNombre("Carlos Mendoza");
        requestValido.setCorreo("carlos.mendoza@example.com");
        requestValido.setContrasena("pass1234");
        requestValido.setRol("CLIENTE");
    }

    // --- TESTS DE DAVID (REACTIVOS) ---

    @Test
    void create_validData_shouldSaveAndReturnResponse() {
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        Usuario usuarioGuardado = buildUsuario(1L, requestValido);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponse response = usuarioService.create(requestValido).block();

        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals("Carlos Mendoza", response.getNombre());
        assertEquals("carlos.mendoza@example.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRol());
        assertTrue(response.getActive());

        verify(usuarioRepository).existsByCorreo(requestValido.getCorreo());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void create_emailAlreadyExists_shouldThrowException_andNotSave() {
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                usuarioService.create(requestValido).block());

        assertTrue(ex.getMessage().contains("ya está registrado") || ex.getMessage().contains("correo"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void update_validEmail_shouldUpdateAndReturnResponse() {
        Long usuarioId = 1L;
        Usuario existente = buildUsuario(usuarioId, requestValido);
        String nuevoCorreo = "carlos.nuevo@example.com";

        UsuarioUpdateRequest updateRequest = new UsuarioUpdateRequest();
        updateRequest.setCorreo(nuevoCorreo);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByCorreo(nuevoCorreo)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        UsuarioResponse response = usuarioService.update(usuarioId, updateRequest).block();

        assertNotNull(response);
        assertEquals(nuevoCorreo, response.getCorreo());
        verify(usuarioRepository).existsByCorreo(nuevoCorreo);
        verify(usuarioRepository).save(existente);
    }

    @Test
    void update_duplicateEmail_shouldThrowException() {
        Long usuarioId = 1L;
        Usuario existente = buildUsuario(usuarioId, requestValido);
        String correoDuplicado = "otro.usuario@example.com";

        UsuarioUpdateRequest updateRequest = new UsuarioUpdateRequest();
        updateRequest.setCorreo(correoDuplicado);

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByCorreo(correoDuplicado)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                usuarioService.update(usuarioId, updateRequest).block());

        assertTrue(ex.getMessage().contains("ya está registrado en otro usuario"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deactivate_validId_shouldDeactivateUser() {
        Usuario usuario = buildUsuario(1L, requestValido);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        UsuarioResponse response = usuarioService.deactivate(1L).block();

        assertFalse(response.getActive());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void getById_userFound_shouldReturnUserResponse() {
        Usuario usuario = buildUsuario(5L, requestValido);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        UsuarioResponse r = usuarioService.getById(5L).block();

        assertNotNull(r);
        assertEquals(5L, r.getId());
        assertEquals(requestValido.getCorreo(), r.getCorreo());
    }

    @Test
    void getById_userNotFound_shouldThrowException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                usuarioService.getById(99L).block());

        assertTrue(ex.getMessage().contains("Usuario") || ex.getMessage().contains("usuario"));
    }

    @Test
    void update_userNotFound_shouldThrowException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        UsuarioUpdateRequest upd = new UsuarioUpdateRequest();
        upd.setNombre("No Existe");

        assertThrows(NotFoundException.class, () ->
                usuarioService.update(99L, upd).block());
    }

    @Test
    void list_shouldReturnUserList() {
        Usuario u1 = buildUsuario(10L, requestValido);
        Usuario u2 = buildUsuario(11L, requestValido);
        u2.setNombre("Otro");

        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<UsuarioResponse> lista = usuarioService.list().collectList().block();

        assertNotNull(lista);
        assertEquals(2, lista.size());
        verify(usuarioRepository).findAll();
    }

    @Test
    void searchByName_shouldReturnPagedResults() {
        Usuario u1 = buildUsuario(20L, requestValido);
        u1.setNombre("Carlos One");
        Usuario u2 = buildUsuario(21L, requestValido);
        u2.setNombre("Carlos Two");

        when(usuarioRepository.findByNombreContainingIgnoreCase(
                eq("carlos"),
                any()
        )).thenReturn(new PageImpl<>(List.of(u1, u2)));

        Page<UsuarioResponse> page = usuarioService.searchByName("carlos", 0, 10).block();

        assertNotNull(page);
        assertEquals(2, page.getTotalElements());
    }

    // --- TUS TESTS ADICIONALES (ADAPTADOS A REACCIÓN) ---

    @Test
    @DisplayName("crearUsuario_datosValidos_guardaUsuario_ArgumentCaptor")
    void crearUsuario_datosValidos_guardaUsuario_ArgumentCaptor() {
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);

        usuarioService.create(requestValido).block();

        verify(usuarioRepository).save(captor.capture());
        Usuario usuarioGuardado = captor.getValue();
        assertEquals("Carlos Mendoza", usuarioGuardado.getNombre());
        assertEquals("carlos.mendoza@example.com", usuarioGuardado.getCorreo());
        assertEquals("CLIENTE", usuarioGuardado.getRol());
        assertTrue(usuarioGuardado.getActive());
    }

    @Test
    @DisplayName("crearUsuario_datosValidos_verificaOrdenEjecucion")
    void crearUsuario_datosValidos_verificaOrdenEjecucion() {
        when(usuarioRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.create(requestValido).block();

        InOrder inOrder = inOrder(usuarioRepository);
        inOrder.verify(usuarioRepository).existsByCorreo(requestValido.getCorreo());
        inOrder.verify(usuarioRepository).save(any(Usuario.class));
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