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

import java.util.List;

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
    public ProductoResponse create(ProductoCreateRequest request) {

        Categoria categoria = categoriaRepo.findById(
                request.getCategoriaId()
        ).orElseThrow(() ->
                new NotFoundException("Categoría no encontrada"));

        Producto p = new Producto();

        p.setNombre(request.getNombre());
        p.setPrecio(request.getPrecio());
        p.setStock(request.getStock());
        p.setCategoria(categoria);
        p.setActive(true);

        return toResponse(repo.save(p));
    }

    @Override
    public ProductoResponse getById(Long id) {

        Producto p = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Producto no encontrado"));

        return toResponse(p);
    }

    @Override
    public List<ProductoResponse> list() {
        return repo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductoResponse deactivate(Long id) {

        Producto p = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Producto no encontrado"));

        p.setActive(false);

        return toResponse(repo.save(p));
    }

    @Override
    public ProductoResponse update(Long id,
                                   ProductoUpdateRequest request) {

        Producto p = repo.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Producto no encontrado"));

        if(request.getNombre() != null)
            p.setNombre(request.getNombre());

        if(request.getPrecio() != null)
            p.setPrecio(request.getPrecio());

        if(request.getStock() != null)
            p.setStock(request.getStock());

        if(request.getActive() != null)
            p.setActive(request.getActive());

        if(request.getCategoriaId() != null) {

            Categoria categoria = categoriaRepo.findById(
                    request.getCategoriaId()
            ).orElseThrow(() ->
                    new NotFoundException("Categoría no encontrada"));

            p.setCategoria(categoria);
        }

        return toResponse(repo.save(p));
    }

    @Override
    public Page<ProductoResponse> searchByName(
            String name,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        return repo.findByNombreContainingIgnoreCase(name, pageable)
                .map(this::toResponse);
    }

    private ProductoResponse toResponse(Producto p) {

        ProductoResponse r = new ProductoResponse();

        r.setId(p.getId());
        r.setNombre(p.getNombre());
        r.setPrecio(p.getPrecio());
        r.setStock(p.getStock());
        r.setActive(p.getActive());
        r.setCategoria(p.getCategoria().getNombre());

        return r;
    }
}
