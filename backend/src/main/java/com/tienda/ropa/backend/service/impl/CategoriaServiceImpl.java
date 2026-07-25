package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.dto.categoria.*;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.service.CategoriaService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaServiceImpl(CategoriaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Mono<CategoriaResponse> create(CategoriaCreateRequest request) {
        return Mono.fromCallable(() -> {
            if (repo.existsByNombre(request.getNombre())) {
                throw new ConflictException("La categoría ya existe");
            }
            Categoria c = new Categoria();
            c.setNombre(request.getNombre());
            return toResponse(repo.save(c));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<CategoriaResponse> getById(Long id) {
        return Mono.fromCallable(() -> {
            Categoria c = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
            return toResponse(c);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<CategoriaResponse> list() {
        return Mono.fromCallable(repo::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<CategoriaResponse> update(Long id, CategoriaUpdateRequest request) {
        return Mono.fromCallable(() -> {
            Categoria c = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

            if (request.getNombre() != null) {
                c.setNombre(request.getNombre());
            }

            return toResponse(repo.save(c));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(Long id) {
        return Mono.<Void>fromRunnable(() -> {
            Categoria c = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
            repo.delete(c);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private CategoriaResponse toResponse(Categoria c) {
        CategoriaResponse r = new CategoriaResponse();
        r.setId(c.getId());
        r.setNombre(c.getNombre());
        return r;
    }
}

