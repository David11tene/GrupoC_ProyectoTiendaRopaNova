package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.Producto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Tests de ProductoRepository
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductoRepository - Tests JPA")
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Categoria categoriaRopa;

    @BeforeEach
    void setUp() {

        // Limpia datos
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        // Crea categoría
        categoriaRopa = new Categoria();
        categoriaRopa.setNombre("Camisas");

        categoriaRopa = categoriaRepository.save(categoriaRopa);
    }

    // Guarda y recupera producto
    @Test
    @DisplayName("Debe guardar y recuperar producto")
    void debeGuardarYRecuperarProducto() {

        Producto producto = new Producto();

        producto.setNombre("Camisa Oxford Blanca");
        producto.setPrecio(29.99);
        producto.setStock(50);
        producto.setActive(true);
        producto.setCategoria(categoriaRopa);

        Producto guardado = productoRepository.save(producto);

        Producto encontrado = productoRepository
                .findById(guardado.getId())
                .orElse(null);

        assertThat(encontrado).isNotNull();
        assertThat(encontrado.getNombre())
                .isEqualTo("Camisa Oxford Blanca");

        assertThat(encontrado.getPrecio())
                .isEqualTo(29.99);

        assertThat(encontrado.getStock())
                .isEqualTo(50);

        assertThat(encontrado.getActive()).isTrue();

        assertThat(encontrado.getCategoria().getNombre())
                .isEqualTo("Camisas");
    }

    // Busca productos por nombre
    @Test
    @DisplayName("Debe buscar productos por nombre")
    void debeBuscarProductosPorNombreParcial() {

        crearProducto("Camisa Oxford Blanca", 29.99);
        crearProducto("Camisa Polo Azul", 24.99);
        crearProducto("Pantalón Chino Beige", 49.99);

        Page<Producto> resultado = productoRepository
                .findByNombreContainingIgnoreCase(
                        "camisa",
                        PageRequest.of(0, 10)
                );

        assertThat(resultado.getTotalElements())
                .isEqualTo(2);

        assertThat(resultado.getContent())
                .extracting(Producto::getNombre)
                .containsExactlyInAnyOrder(
                        "Camisa Oxford Blanca",
                        "Camisa Polo Azul"
                );
    }

    // Retorna vacío si no hay coincidencias
    @Test
    @DisplayName("Debe retornar vacío")
    void debeRetornarVacioSiNoHayCoincidencias() {

        crearProducto("Camisa Oxford Blanca", 29.99);

        Page<Producto> resultado = productoRepository
                .findByNombreContainingIgnoreCase(
                        "zapato",
                        PageRequest.of(0, 10)
                );

        assertThat(resultado.getTotalElements()).isZero();
    }

    // Verifica configuración H2
    @Test
    @DisplayName("Configuración H2 correcta")
    void configuracionH2EstaOk() {

        List<Producto> inicial = productoRepository.findAll();

        assertThat(inicial).isEmpty();

        crearProducto("Test Producto", 10.0);

        List<Producto> despues = productoRepository.findAll();

        assertThat(despues).hasSize(1);
    }

    // Crea producto auxiliar
    private void crearProducto(String nombre, double precio) {

        Producto p = new Producto();

        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(10);
        p.setActive(true);
        p.setCategoria(categoriaRopa);

        productoRepository.save(p);
    }
}