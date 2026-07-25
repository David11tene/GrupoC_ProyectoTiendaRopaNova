package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CategoriaRepository - Tests JPA")
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @BeforeEach
    void setUp() {
        categoriaRepository.deleteAll();
    }

    @Test
    @DisplayName("Debe guardar y buscar categoría por nombre")
    void debeGuardarYBuscarPorNombre() {
        Categoria c = new Categoria();
        c.setNombre("Pantalones");
        categoriaRepository.save(c);

        Optional<Categoria> encontrada = categoriaRepository.findByNombre("Pantalones");
        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNombre()).isEqualTo("Pantalones");
    }

    @Test
    @DisplayName("Debe verificar si existe categoría por nombre")
    void debeVerificarSiExistePorNombre() {
        Categoria c = new Categoria();
        c.setNombre("Zapatos");
        categoriaRepository.save(c);

        boolean existe = categoriaRepository.existsByNombre("Zapatos");
        boolean noExiste = categoriaRepository.existsByNombre("Accesorios");

        assertThat(existe).isTrue();
        assertThat(noExiste).isFalse();
    }
}
