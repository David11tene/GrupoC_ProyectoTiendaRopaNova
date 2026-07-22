package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.dto.usuario.UsuarioCreateRequest;
import com.tienda.ropa.backend.dto.usuario.UsuarioResponse;
import com.tienda.ropa.backend.dto.usuario.UsuarioUpdateRequest;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.web.advice.ConflictException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Arrays;

// Tests de UsuarioServiceImpl
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl - Tests")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private UsuarioCreateRequest requestValido;

    @BeforeEach
    void setUp() {

        requestValido = new UsuarioCreateRequest();
        requestValido.setNombre("Carlos Mendoza");
        requestValido.setCorreo("carlos.mendoza@example.com");
        requestValido.setContrasena("pass1234");
        requestValido.setRol("CLIENTE");
    }

    // Crea usuario correctamente
    @Test
    @DisplayName("Debe crear un usuario")
    void debeCrearUsuarioCuandoCorreoNoExiste() {

        when(usuarioRepository.existsByCorreo(requestValido.getCorreo()))
                .thenReturn(false);

        Usuario usuarioGuardado = buildUsuario(1L, requestValido);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuarioGuardado);

        UsuarioResponse response =
                usuarioService.create(requestValido);

        assertThat(response).isNotNull();

        assertThat(response.getNombre())
                .isEqualTo("Carlos Mendoza");

        assertThat(response.getCorreo())
                .isEqualTo("carlos.mendoza@example.com");

        assertThat(response.getRol())
                .isEqualTo("CLIENTE");

        assertThat(response.getActive()).isTrue();

        verify(usuarioRepository, times(1))
                .save(any(Usuario.class));
    }

    // Valida correo duplicado
    @Test
    @DisplayName("Debe lanzar ConflictException")
    void debeLanzarConflictExceptionSiCorreoYaExiste() {

        when(usuarioRepository.existsByCorreo(requestValido.getCorreo()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                usuarioService.create(requestValido)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("correo");

        verify(usuarioRepository, never())
                .save(any(Usuario.class));
    }

    // Desactiva usuario
    @Test
    @DisplayName("Debe desactivar usuario")
    void debeDesactivarUsuario() {

        Usuario usuario = buildUsuario(1L, requestValido);

        when(usuarioRepository.findById(1L))
                .thenReturn(java.util.Optional.of(usuario));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse response =
                usuarioService.deactivate(1L);

        assertThat(response.getActive()).isFalse();

        verify(usuarioRepository, times(1))
                .save(any(Usuario.class));
    }

    // Obtiene usuario por id
    @Test
    @DisplayName("Debe obtener usuario por id")
    void debeObtenerUsuarioPorId() {

        Usuario usuario = buildUsuario(5L, requestValido);

        when(usuarioRepository.findById(5L))
                .thenReturn(java.util.Optional.of(usuario));

        UsuarioResponse r =
                usuarioService.getById(5L);

        assertThat(r).isNotNull();

        assertThat(r.getId()).isEqualTo(5L);

        assertThat(r.getCorreo())
                .isEqualTo(requestValido.getCorreo());
    }

    // Actualiza usuario
    @Test
    @DisplayName("Debe actualizar usuario")
    void debeActualizarUsuarioCuandoExiste() {

        Usuario existente = buildUsuario(2L, requestValido);

        when(usuarioRepository.findById(2L))
                .thenReturn(java.util.Optional.of(existente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UsuarioUpdateRequest upd =
                new UsuarioUpdateRequest();

        upd.setNombre("Carlos Updated");

        UsuarioResponse res =
                usuarioService.update(2L, upd);

        assertThat(res.getNombre())
                .isEqualTo("Carlos Updated");

        verify(usuarioRepository, times(1))
                .save(any(Usuario.class));
    }

    // Usuario no encontrado al actualizar
    @Test
    @DisplayName("Debe lanzar NotFoundException")
    void debeLanzarNotFoundAlActualizarNoExistente() {

        when(usuarioRepository.findById(99L))
                .thenReturn(java.util.Optional.empty());

        UsuarioUpdateRequest upd =
                new UsuarioUpdateRequest();

        upd.setNombre("No Existe");

        assertThatThrownBy(() ->
                usuarioService.update(99L, upd)
        )
                .isInstanceOf(
                        com.tienda.ropa.backend.web.advice.NotFoundException.class
                );
    }

    // Lista usuarios
    @Test
    @DisplayName("Debe listar usuarios")
    void listRetornaUsuarios() {

        Usuario u1 = buildUsuario(10L, requestValido);

        Usuario u2 = buildUsuario(11L, requestValido);
        u2.setNombre("Otro");

        when(usuarioRepository.findAll())
                .thenReturn(Arrays.asList(u1, u2));

        List<UsuarioResponse> lista =
                usuarioService.list();

        assertThat(lista).hasSize(2);

        assertThat(lista)
                .extracting(UsuarioResponse::getId)
                .contains(10L, 11L);
    }

    // Busca usuarios por nombre
    @Test
    @DisplayName("Debe buscar usuarios")
    void searchByNamePaginado() {

        Usuario u1 = buildUsuario(20L, requestValido);
        u1.setNombre("Carlos One");

        Usuario u2 = buildUsuario(21L, requestValido);
        u2.setNombre("Carlos Two");

        when(usuarioRepository.findByNombreContainingIgnoreCase(
                eq("carlos"),
                any()
        ))
                .thenReturn(new PageImpl<>(List.of(u1, u2)));

        var page = usuarioService.searchByName(
                "carlos",
                0,
                10
        );

        assertThat(page.getTotalElements())
                .isEqualTo(2);

        assertThat(page.getContent())
                .extracting(UsuarioResponse::getNombre)
                .containsExactlyInAnyOrder(
                        "Carlos One",
                        "Carlos Two"
                );
    }

    // Usuario no encontrado
    @Test
    @DisplayName("Debe lanzar NotFoundException si no existe")
    void debeLanzarNotFoundSiUsuarioNoExiste() {

        when(usuarioRepository.findById(99L))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() ->
                usuarioService.getById(99L)
        )
                .isInstanceOf(
                        com.tienda.ropa.backend.web.advice.NotFoundException.class
                )
                .hasMessageContaining("Usuario");
    }

    // Crea usuario auxiliar
    private Usuario buildUsuario(
            Long id,
            UsuarioCreateRequest req
    ) {

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