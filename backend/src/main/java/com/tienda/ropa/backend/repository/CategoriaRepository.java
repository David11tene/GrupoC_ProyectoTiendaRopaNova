package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Buscar categoría por nombre
    Optional<Categoria> findByNombre(String nombre);

    // Validar si existe una categoría
    boolean existsByNombre(String nombre);
}
