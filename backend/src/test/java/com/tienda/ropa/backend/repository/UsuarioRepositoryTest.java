package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Tests de UsuarioRepository
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UsuarioRepository - Tests JPA")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {

        // Limpia datos
        usuarioRepository.deleteAll();
    }

    // Guarda y recupera usuario
    @Test
    @DisplayName("Debe guardar y recuperar usuario")
    void debeGuardarYRecuperarUsuario() {

        Usuario u = new Usuario();

        u.setNombre("Juan Perez");
        u.setCorreo("juan@example.com");
        u.setContrasena("secreto");
        u.setRol("USER");
        u.setActive(true);

        Usuario guardado = usuarioRepository.save(u);

        Usuario encontrado = usuarioRepository
                .findById(guardado.getId())
                .orElse(null);

        assertThat(encontrado).isNotNull();

        assertThat(encontrado.getNombre())
                .isEqualTo("Juan Perez");

        assertThat(encontrado.getCorreo())
                .isEqualTo("juan@example.com");

        assertThat(encontrado.getRol())
                .isEqualTo("USER");

        assertThat(encontrado.getActive()).isTrue();
    }

    // Busca usuario por correo
    @Test
    @DisplayName("Debe buscar usuario por correo")
    void debeBuscarUsuarioPorCorreo() {

        crearUsuario("Ana", "ana@example.com");

        var opt = usuarioRepository.findByCorreo("ana@example.com");

        assertThat(opt).isPresent();

        assertThat(opt.orElseThrow().getNombre())
                .isEqualTo("Ana");
    }

    // Verifica existencia de correo
    @Test
    @DisplayName("Debe validar existencia de correo")
    void existsByCorreoIndicaExistencia() {

        crearUsuario("Pedro", "pedro@example.com");

        assertThat(
                usuarioRepository.existsByCorreo("pedro@example.com")
        ).isTrue();

        assertThat(
                usuarioRepository.existsByCorreo("noexiste@example.com")
        ).isFalse();
    }

    // Busca usuarios por nombre
    @Test
    @DisplayName("Debe buscar usuarios por nombre")
    void debeBuscarUsuariosPorNombreParcial() {

        crearUsuario("María Gomez", "maria@example.com");
        crearUsuario("Mariano López", "mariano@example.com");
        crearUsuario("Carlos Ruiz", "carlos@example.com");

        Page<Usuario> resultado = usuarioRepository
                .findByNombreContainingIgnoreCase(
                        "mar",
                        PageRequest.of(0, 10)
                );

        assertThat(resultado.getTotalElements())
                .isEqualTo(2);

        assertThat(resultado.getContent())
                .extracting(Usuario::getNombre)
                .containsExactlyInAnyOrder(
                        "María Gomez",
                        "Mariano López"
                );
    }

    // Busca usuario por nombre exacto
    @Test
    @DisplayName("Debe buscar usuario por nombre exacto")
    void debeBuscarUsuarioPorNombreExacto() {

        crearUsuario("Lucia", "lucia@example.com");

        var opt = usuarioRepository.findByNombre("Lucia");

        assertThat(opt).isPresent();

        assertThat(opt.orElseThrow().getCorreo())
                .isEqualTo("lucia@example.com");
    }

    // Elimina usuario por id
    @Test
    @DisplayName("Debe eliminar usuario por id")
    void eliminarPorIdRemueveUsuario() {

        Usuario u = new Usuario();

        u.setNombre("Borrar");
        u.setCorreo("borrar@example.com");
        u.setContrasena("pw");
        u.setRol("USER");
        u.setActive(true);

        Usuario guardado = usuarioRepository.save(u);

        usuarioRepository.deleteById(guardado.getId());

        assertThat(
                usuarioRepository.findById(guardado.getId())
        ).isEmpty();
    }

    // Valida correos únicos
    @Test
    @DisplayName("No debe permitir correos duplicados")
    void noPermiteCorreosDuplicados() {

        crearUsuario("Uno", "dup@example.com");

        Usuario u2 = new Usuario();

        u2.setNombre("Dos");
        u2.setCorreo("dup@example.com");
        u2.setContrasena("pw");
        u2.setRol("USER");

        org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(u2)
        );
    }

    // Verifica paginación
    @Test
    @DisplayName("Debe manejar paginación")
    void paginacionLimites() {

        crearUsuario("U1", "u1@example.com");
        crearUsuario("U2", "u2@example.com");
        crearUsuario("U3", "u3@example.com");

        var p0 = usuarioRepository.findByNombreContainingIgnoreCase(
                "u",
                PageRequest.of(0, 1)
        );

        var p1 = usuarioRepository.findByNombreContainingIgnoreCase(
                "u",
                PageRequest.of(1, 1)
        );

        var p2 = usuarioRepository.findByNombreContainingIgnoreCase(
                "u",
                PageRequest.of(2, 1)
        );

        var p3 = usuarioRepository.findByNombreContainingIgnoreCase(
                "u",
                PageRequest.of(3, 1)
        );

        assertThat(p0.getTotalElements()).isEqualTo(3);

        assertThat(p0.getContent()).hasSize(1);
        assertThat(p1.getContent()).hasSize(1);
        assertThat(p2.getContent()).hasSize(1);
        assertThat(p3.getContent()).isEmpty();
    }

    // Retorna vacío si no hay resultados
    @Test
    @DisplayName("Debe retornar vacío sin coincidencias")
    void buscarSinResultadosRetornaVacio() {

        crearUsuario("Persona", "p@example.com");

        PageImpl<Usuario> resultado =
                (PageImpl<Usuario>) usuarioRepository
                        .findByNombreContainingIgnoreCase(
                                "zzzz",
                                PageRequest.of(0, 10)
                        );

        assertThat(resultado.getTotalElements()).isZero();
        assertThat(resultado.getContent()).isEmpty();
    }

    // Verifica configuración H2
    @Test
    @DisplayName("Configuración H2 correcta")
    void configuracionH2EstaOk() {

        List<Usuario> inicial = usuarioRepository.findAll();

        assertThat(inicial).isEmpty();

        crearUsuario("Test Usuario", "test@example.com");

        List<Usuario> despues = usuarioRepository.findAll();

        assertThat(despues).hasSize(1);
    }

    // Crea usuario auxiliar
    private void crearUsuario(String nombre, String correo) {

        Usuario u = new Usuario();

        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setContrasena("pw");
        u.setRol("USER");
        u.setActive(true);

        usuarioRepository.save(u);
    }
}