package com.tienda.ropa.backend.repository;

import com.tienda.ropa.backend.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por correo
    Optional<Usuario> findByCorreo(String correo);

    // Buscar usuario por nombre
    Optional<Usuario> findByNombre(String nombre);

    // Validar si ya existe el correo
    boolean existsByCorreo(String correo);

    // Buscar por nombre con paginación
    Page<Usuario> findByNombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );
}
