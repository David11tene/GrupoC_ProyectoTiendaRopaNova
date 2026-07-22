package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.dto.categoria.*;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.service.CategoriaService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaServiceImpl(CategoriaRepository repo) {
        this.repo = repo;
    }

    @Override
    public CategoriaResponse create(CategoriaCreateRequest request) {

        if(repo.existsByNombre(request.getNombre())) {
            throw new ConflictException("La categoría ya existe");
        }

        Categoria c = new Categoria();
        c.setNombre(request.getNombre());

        return toResponse(repo.save(c));
    }

    @Override
    public CategoriaResponse getById(Long id) {

        Categoria c = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Categoría no encontrada"));

        return toResponse(c);
    }

    @Override
    public List<CategoriaResponse> list() {
        return repo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoriaResponse update(Long id,
                                    CategoriaUpdateRequest request) {

        Categoria c = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Categoría no encontrada"));

        if(request.getNombre() != null)
            c.setNombre(request.getNombre());

        return toResponse(repo.save(c));
    }

    @Override
    public void delete(Long id) {

        Categoria c = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Categoría no encontrada"));

        repo.delete(c);
    }

    private CategoriaResponse toResponse(Categoria c) {

        CategoriaResponse r = new CategoriaResponse();

        r.setId(c.getId());
        r.setNombre(c.getNombre());

        return r;
    }
}
