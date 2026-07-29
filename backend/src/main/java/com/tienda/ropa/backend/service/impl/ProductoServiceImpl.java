package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.Categoria;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.dto.producto.*;
import com.tienda.ropa.backend.repository.CategoriaRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import com.tienda.ropa.backend.service.ProductoService;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;

    public ProductoServiceImpl(
            ProductoRepository repo,
            CategoriaRepository categoriaRepo) {
        this.repo = repo;
        this.categoriaRepo = categoriaRepo;
    }

    @Override
    public Mono<ProductoResponse> create(ProductoCreateRequest request) {
        return Mono.fromCallable(() -> {
            Categoria categoria = categoriaRepo.findById(
                    request.getCategoriaId()
            ).orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

            Producto p = new Producto();
            p.setNombre(request.getNombre());
            p.setPrecio(request.getPrecio());
            p.setStock(request.getStock());
            p.setImagenUrl(request.getImagenUrl());
            p.setCategoria(categoria);
            p.setActive(true);
            p.setImagenUrl(request.getImagenUrl());

            return toResponse(repo.save(p));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ProductoResponse> getById(Long id) {
        return Mono.fromCallable(() -> {
            Producto p = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
            return toResponse(p);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<ProductoResponse> list() {
        return Mono.fromCallable(repo::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ProductoResponse> deactivate(Long id) {
        return Mono.fromCallable(() -> {
            Producto p = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

            p.setActive(false);
            return toResponse(repo.save(p));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ProductoResponse> update(Long id, ProductoUpdateRequest request) {
        return Mono.fromCallable(() -> {
            Producto p = repo.findById(id)
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

            if (request.getNombre() != null)
                p.setNombre(request.getNombre());

            if (request.getPrecio() != null)
                p.setPrecio(request.getPrecio());

            if (request.getStock() != null)
                p.setStock(request.getStock());

            if (request.getImagenUrl() != null)
                p.setImagenUrl(request.getImagenUrl());

            if (request.getActive() != null)
                p.setActive(request.getActive());

            if (request.getImagenUrl() != null)
                p.setImagenUrl(request.getImagenUrl());

            if (request.getCategoriaId() != null) {
                Categoria categoria = categoriaRepo.findById(
                        request.getCategoriaId()
                ).orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

                p.setCategoria(categoria);
            }

            return toResponse(repo.save(p));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Page<ProductoResponse>> searchByName(
            String name,
            int page,
            int size) {

        return Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page, size);
            return repo.findByNombreContainingIgnoreCase(name, pageable)
                    .map(this::toResponse);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ProductoResponse toResponse(Producto p) {
        ProductoResponse r = new ProductoResponse();

        r.setId(p.getId());
        r.setNombre(p.getNombre());
        r.setPrecio(p.getPrecio());
        r.setStock(p.getStock());
        r.setImagenUrl(p.getImagenUrl());
        r.setActive(p.getActive());
        r.setCategoria(p.getCategoria().getNombre());
        r.setImagenUrl(p.getImagenUrl());

        return r;
    }
}

