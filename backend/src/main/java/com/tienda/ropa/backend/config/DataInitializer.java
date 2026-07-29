package com.tienda.ropa.backend.config;

import com.tienda.ropa.backend.domain.*;
import com.tienda.ropa.backend.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

// Carga datos iniciales
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initData(
            UsuarioRepository usuarioRepo,
            CategoriaRepository categoriaRepo,
            ProductoRepository productoRepo,
            PedidoRepository pedidoRepo
    ) {
        return args -> {

            // Verifica si ya existen datos
            if (usuarioRepo.count() > 0) {
                log.info("Base de datos ya inicializada.");
                return;
            }

            log.info("Cargando datos iniciales...");

            // Usuarios
            Usuario admin = new Usuario();
            admin.setNombre("admin");
            admin.setCorreo("admin@novatienda.com");
            admin.setContrasena("admin123");
            admin.setRol("ADMIN");
            admin.setActive(true);
            usuarioRepo.save(admin);

            Usuario cliente = new Usuario();
            cliente.setNombre("maria");
            cliente.setCorreo("maria@gmail.com");
            cliente.setContrasena("cliente123");
            cliente.setRol("CLIENTE");
            cliente.setActive(true);
            usuarioRepo.save(cliente);

            // Categorías
            Categoria camisas = categoria(categoriaRepo, "Camisas");
            Categoria pantalones = categoria(categoriaRepo, "Pantalones");
            Categoria vestidos = categoria(categoriaRepo, "Vestidos");
            Categoria accesorios = categoria(categoriaRepo, "Accesorios");

            // Productos
            Producto p1 = producto(productoRepo, "Camisa Oxford Blanca", 29.99, 50, camisas,
                    "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=600&q=80");
            Producto p2 = producto(productoRepo, "Camisa Lino Azul Marino", 34.99, 30, camisas,
                    "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=600&q=80");
            Producto p3 = producto(productoRepo, "Camisa Cuadros Flannel", 27.99, 25, camisas,
                    "https://images.unsplash.com/photo-1620012253295-c15cc3e65df4?w=600&q=80");

            Producto p4 = producto(productoRepo, "Pantalón Chino Beige", 49.99, 40, pantalones,
                    "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=600&q=80");
            Producto p5 = producto(productoRepo, "Jeans Slim Fit Oscuro", 59.99, 35, pantalones,
                    "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=600&q=80");

            Producto p6 = producto(productoRepo, "Vestido Floral Verano", 44.99, 20, vestidos,
                    "https://images.unsplash.com/photo-1612336307429-8a898d10e223?w=600&q=80");
            Producto p7 = producto(productoRepo, "Vestido Midi Negro", 54.99, 15, vestidos,
                    "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=600&q=80");

            Producto p8 = producto(productoRepo, "Cinturón Cuero Café", 19.99, 5, accesorios,
                    "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&q=80");

            // Pedidos menores a $100 para demostrar filtro y backpressure.
            crearPedido(pedidoRepo, productoRepo, cliente, p1, 2); // $59.98
            crearPedido(pedidoRepo, productoRepo, cliente, p2, 2); // $69.98
            crearPedido(pedidoRepo, productoRepo, cliente, p6, 2); // $89.98
            crearPedido(pedidoRepo, productoRepo, cliente, p8, 1); // $19.99 (filtrado)

            log.info("Datos iniciales cargados correctamente.");
        };
    }

    // Crea categoría
    private Categoria categoria(CategoriaRepository repo, String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return repo.save(c);
    }

    // Crea producto
    private Producto producto(
            ProductoRepository repo,
            String nombre,
            double precio,
            int stock,
            Categoria categoria,
            String imagenUrl
    ) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setActive(true);
        p.setCategoria(categoria);
        p.setImagenUrl(imagenUrl);

        return repo.save(p);
    }

    private Pedido crearPedido(
            PedidoRepository pedidoRepo,
            ProductoRepository productoRepo,
            Usuario usuario,
            Producto producto,
            int cantidad
    ) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFecha(LocalDate.now());
        pedido.setEstado("PENDIENTE");

        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setSubtotal(producto.getPrecio() * cantidad);
        pedido.addDetalle(detalle);
        pedido.setTotal(detalle.getSubtotal());

        producto.setStock(producto.getStock() - cantidad);
        productoRepo.save(producto);

        return pedidoRepo.save(pedido);
    }
}
