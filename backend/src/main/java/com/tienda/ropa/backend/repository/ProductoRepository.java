package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar productos por nombre
    Page<Producto> findByNombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );
}
