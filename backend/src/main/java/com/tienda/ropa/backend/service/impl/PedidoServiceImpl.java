package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.DetallePedido;
import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.domain.Usuario;
import com.tienda.ropa.backend.dto.detallepedido.DetallePedidoResponse;
import com.tienda.ropa.backend.dto.pedido.PedidoCreateRequest;
import com.tienda.ropa.backend.dto.pedido.PedidoResponse;
import com.tienda.ropa.backend.repository.PedidoRepository;
import com.tienda.ropa.backend.repository.ProductoRepository;
import com.tienda.ropa.backend.repository.UsuarioRepository;
import com.tienda.ropa.backend.service.PedidoService;
import com.tienda.ropa.backend.service.reactive.PedidoReactiveService;
import com.tienda.ropa.backend.web.advice.ConflictException;
import com.tienda.ropa.backend.web.advice.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// Servicio reactivo de pedidos
@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo;
    private final PedidoReactiveService pedidoReactiveService;

    public PedidoServiceImpl(
            PedidoRepository pedidoRepo,
            UsuarioRepository usuarioRepo,
            ProductoRepository productoRepo,
            PedidoReactiveService pedidoReactiveService
    ) {
        this.pedidoRepo = pedidoRepo;
        this.usuarioRepo = usuarioRepo;
        this.productoRepo = productoRepo;
        this.pedidoReactiveService = pedidoReactiveService;
    }

    @Override
    @Transactional
    public Mono<PedidoResponse> create(PedidoCreateRequest request) {
        return Mono.fromCallable(() -> {
            Usuario usuario = usuarioRepo.findById(request.getIdUsuario())
                    .orElseThrow(() -> new NotFoundException(
                            "Usuario no encontrado con id: " + request.getIdUsuario()
                    ));

            if (!Boolean.TRUE.equals(usuario.getActive())) {
                throw new ConflictException(
                        "El usuario está desactivado y no puede hacer pedidos."
                );
            }

            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setFecha(LocalDate.now());
            pedido.setEstado("PENDIENTE");

            double total = 0.0;

            for (PedidoCreateRequest.ProductoItemRequest item : request.getProductos()) {
                Producto producto = productoRepo.findById(item.getIdProducto())
                        .orElseThrow(() -> new NotFoundException(
                                "Producto no encontrado con id: " + item.getIdProducto()
                        ));

                if (!Boolean.TRUE.equals(producto.getActive())) {
                    throw new ConflictException(
                            "El producto '" + producto.getNombre() + "' no está disponible."
                    );
                }

                if (producto.getStock() < item.getCantidad()) {
                    throw new ConflictException(
                            "Stock insuficiente para '" + producto.getNombre() +
                                    "'. Disponible: " + producto.getStock() +
                                    ", solicitado: " + item.getCantidad()
                    );
                }

                double subtotal = producto.getPrecio() * item.getCantidad();
                total += subtotal;

                DetallePedido detalle = new DetallePedido();
                detalle.setProducto(producto);
                detalle.setCantidad(item.getCantidad());
                detalle.setSubtotal(subtotal);

                pedido.addDetalle(detalle);

                producto.setStock(producto.getStock() - item.getCantidad());
                productoRepo.save(producto);
            }

            pedido.setTotal(total);

            PedidoResponse response = toResponse(pedidoRepo.save(pedido));
            if (pedidoReactiveService != null) {
                pedidoReactiveService.publishPedido(response);
            }
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PedidoResponse> getById(Long id) {
        return Mono.fromCallable(() -> toResponse(
                pedidoRepo.findById(id)
                        .orElseThrow(() -> new NotFoundException(
                                "Pedido no encontrado con id: " + id
                        ))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<PedidoResponse> list() {
        return Mono.fromCallable(pedidoRepo::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::toResponse)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<PedidoResponse> updateEstado(Long id, String nuevoEstado) {
        return Mono.fromCallable(() -> {
            List<String> estadosValidos = List.of(
                    "PENDIENTE",
                    "APROBADO",
                    "EN_PREPARACION",
                    "RECHAZADO",
                    "ENVIADO",
                    "DESPACHADO",
                    "ENTREGADO"
            );

            String estadoNormalizado = nuevoEstado.toUpperCase();

            if (!estadosValidos.contains(estadoNormalizado)) {
                throw new ConflictException(
                        "Estado inválido: '" + nuevoEstado +
                                "'. Valores permitidos: " + estadosValidos
                );
            }

            Pedido pedido = pedidoRepo.findById(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Pedido no encontrado con id: " + id
                    ));

            String estadoAnterior = pedido.getEstado();

            // Si pasa a RECHAZADO y antes no estaba rechazado, se reincorpora el stock al inventario
            if ("RECHAZADO".equals(estadoNormalizado) && !"RECHAZADO".equals(estadoAnterior)) {
                if (pedido.getDetalles() != null) {
                    for (DetallePedido detalle : pedido.getDetalles()) {
                        Producto producto = detalle.getProducto();
                        if (producto != null && detalle.getCantidad() != null) {
                            int stockActual = producto.getStock() != null ? producto.getStock() : 0;
                            producto.setStock(stockActual + detalle.getCantidad());
                            productoRepo.save(producto);
                        }
                    }
                }
            }

            pedido.setEstado(estadoNormalizado);

            PedidoResponse response = toResponse(pedidoRepo.save(pedido));
            if (pedidoReactiveService != null) {
                pedidoReactiveService.publishPedido(response);
            }
            return response;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PedidoResponse toResponse(Pedido p) {
        PedidoResponse r = new PedidoResponse();

        r.setId(p.getId());
        r.setUsuario(p.getUsuario() != null ? p.getUsuario().getNombre() : "Desconocido");
        r.setFecha(p.getFecha());
        r.setTotal(p.getTotal());
        r.setEstado(p.getEstado());

        if (p.getDetalles() != null) {
            List<DetallePedidoResponse> detallesResp = p.getDetalles().stream().map(d -> {
                DetallePedidoResponse dr = new DetallePedidoResponse();
                dr.setId(d.getId());
                dr.setProducto(d.getProducto() != null ? d.getProducto().getNombre() : "Producto sin nombre");
                dr.setCantidad(d.getCantidad());
                dr.setSubtotal(d.getSubtotal());
                if (d.getProducto() != null) {
                    dr.setStockDisponible(d.getProducto().getStock());
                    if (d.getProducto().getPrecio() != null) {
                        dr.setPrecioUnitario(d.getProducto().getPrecio());
                    }
                }
                if (dr.getPrecioUnitario() == null && d.getCantidad() != null && d.getCantidad() > 0 && d.getSubtotal() != null) {
                    dr.setPrecioUnitario(d.getSubtotal() / d.getCantidad());
                }
                return dr;
            }).collect(Collectors.toList());
            r.setDetalles(detallesResp);
        }

        return r;
    }
}


