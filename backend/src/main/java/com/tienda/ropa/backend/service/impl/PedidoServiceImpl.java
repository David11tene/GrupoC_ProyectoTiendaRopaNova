package com.tienda.ropa.backend.service.impl;

import com.tienda.ropa.backend.domain.DetallePedido;
import com.tienda.ropa.backend.domain.Pedido;
import com.tienda.ropa.backend.domain.Producto;
import com.tienda.ropa.backend.domain.Usuario;
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

import java.time.LocalDate;
import java.util.List;

// Servicio de pedidos
@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo;
    private final PedidoReactiveService pedidoReactiveService;

    // Inyección de dependencias
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

    // Crea un pedido
    @Override
    @Transactional
    public PedidoResponse create(PedidoCreateRequest request) {

        // Valida usuario
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

        // Procesa productos
        for (PedidoCreateRequest.ProductoItemRequest item : request.getProductos()) {

            Producto producto = productoRepo.findById(item.getIdProducto())
                    .orElseThrow(() -> new NotFoundException(
                            "Producto no encontrado con id: " + item.getIdProducto()
                    ));

            // Valida producto activo
            if (!Boolean.TRUE.equals(producto.getActive())) {
                throw new ConflictException(
                        "El producto '" + producto.getNombre() + "' no está disponible."
                );
            }

            // Valida stock
            if (producto.getStock() < item.getCantidad()) {
                throw new ConflictException(
                        "Stock insuficiente para '" + producto.getNombre() +
                                "'. Disponible: " + producto.getStock() +
                                ", solicitado: " + item.getCantidad()
                );
            }

            // Calcula subtotal
            double subtotal = producto.getPrecio() * item.getCantidad();
            total += subtotal;

            // Crea detalle
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setSubtotal(subtotal);

            pedido.addDetalle(detalle);

            // Actualiza stock
            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepo.save(producto);
        }

        pedido.setTotal(total);


        PedidoResponse response = toResponse(pedidoRepo.save(pedido));
        pedidoReactiveService.publishPedido(response);
        return response;

    }

    // Obtiene pedido por id
    @Override
    public PedidoResponse getById(Long id) {

        return toResponse(
                pedidoRepo.findById(id)
                        .orElseThrow(() -> new NotFoundException(
                                "Pedido no encontrado con id: " + id
                        ))
        );
    }

    // Lista pedidos
    @Override
    public List<PedidoResponse> list() {

        return pedidoRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Actualiza estado
    @Override
    @Transactional
    public PedidoResponse updateEstado(Long id, String nuevoEstado) {

        List<String> estadosValidos = List.of(
                "PENDIENTE",
                "APROBADO",
                "RECHAZADO",
                "ENVIADO",
                "ENTREGADO"
        );

        if (!estadosValidos.contains(nuevoEstado.toUpperCase())) {

            throw new ConflictException(
                    "Estado inválido: '" + nuevoEstado +
                            "'. Valores permitidos: " + estadosValidos
            );
        }

        Pedido pedido = pedidoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Pedido no encontrado con id: " + id
                ));

        pedido.setEstado(nuevoEstado.toUpperCase());

                PedidoResponse response = toResponse(pedidoRepo.save(pedido));
        pedidoReactiveService.publishPedido(response);
                return response;
    }

    // Convierte entidad a DTO
    private PedidoResponse toResponse(Pedido p) {

        PedidoResponse r = new PedidoResponse();

        r.setId(p.getId());
        r.setUsuario(p.getUsuario().getNombre());
        r.setFecha(p.getFecha());
        r.setTotal(p.getTotal());
        r.setEstado(p.getEstado());

        return r;
    }
}
