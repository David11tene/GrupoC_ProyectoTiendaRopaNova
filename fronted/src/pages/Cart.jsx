import React, { useState } from 'react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api, fmt } from '../services/api';
import { useNavigate, Link } from 'react-router-dom';

export default function Cart() {
    const { cart, removeFromCart, updateQuantity, totalPrice, clearCart } = useCart();
    const { user } = useAuth();
    const { toast } = useToast();
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleCheckout = async () => {
        setLoading(true);
        try {
            // Payload exacto que espera PedidoCreateRequest del backend
            const payload = {
                idUsuario: user.id,
                productos: cart.map(item => ({
                    idProducto: item.id,
                    cantidad: item.quantity,
                })),
            };
            await api.pedidos.create(payload);
            clearCart();
            toast('¡Pedido realizado con éxito!', 'success');
            navigate('/mis-pedidos');
        } catch (err) {
            toast(err.message || 'Error al procesar el pedido', 'error');
        } finally {
            setLoading(false);
        }
    };

    if (cart.length === 0) {
        return (
            <div className="container page-wrapper">
                <div className="empty-state" style={{ maxWidth: 480, margin: '4rem auto' }}>
                    <div className="empty-icon">🛍</div>
                    <h3>Tu carrito está vacío</h3>
                    <p>Explora el catálogo y elige tus prendas favoritas</p>
                    <Link to="/" className="btn btn-accent" style={{ marginTop: '1.5rem' }}>
                        Ver Tienda
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="container page-wrapper">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
                <h2>Carrito de Compras</h2>
                <button className="btn btn-ghost" onClick={clearCart}>Vaciar carrito</button>
            </div>

            <div className="cart-layout">
                {/* Items */}
                <div>
                    {cart.map(item => (
                        <div key={item.id} className="cart-item">
                            <div className="cart-thumb">👕</div>

                            <div className="cart-item-info">
                                <div className="cart-item-name">{item.nombre}</div>
                                <div className="cart-item-meta">{item.categoria} · {fmt.price(item.precio)} c/u</div>
                            </div>

                            <div className="qty-controls">
                                <button className="qty-btn" onClick={() => updateQuantity(item.id, item.quantity - 1)}>−</button>
                                <span className="qty-val">{item.quantity}</span>
                                <button className="qty-btn" onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
                            </div>

                            <div className="cart-item-price">
                                {fmt.price(parseFloat(item.precio) * item.quantity)}
                            </div>

                            <button className="btn btn-ghost btn-sm" onClick={() => removeFromCart(item.id)} title="Quitar">
                                ✕
                            </button>
                        </div>
                    ))}
                </div>

                {/*Resume*/}
                <div className="order-summary">
                    <h3>Resumen del pedido</h3>

                    {cart.map(item => (
                        <div key={item.id} className="summary-line">
                            <span>{item.nombre} ×{item.quantity}</span>
                            <span>{fmt.price(parseFloat(item.precio) * item.quantity)}</span>
                        </div>
                    ))}

                    <div className="summary-total">
                        <span>Total</span>
                        <span>{fmt.price(totalPrice)}</span>
                    </div>

                    <div style={{ fontSize: '0.8rem', color: 'rgba(245,240,232,0.4)', marginBottom: '1rem' }}>
                        Comprando como: <strong style={{ color: 'rgba(245,240,232,0.75)' }}>{user?.nombre}</strong>
                    </div>

                    <button
                        className="btn btn-accent btn-full btn-lg"
                        onClick={handleCheckout}
                        disabled={loading}
                    >
                        {loading ? 'Procesando...' : 'Confirmar Pedido'}
                    </button>

                    <p className="summary-note">
                        El estado del pedido será actualizado por el administrador
                    </p>
                </div>
            </div>
        </div>
    );
}
