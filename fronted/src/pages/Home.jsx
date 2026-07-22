import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, fmt } from '../services/api';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function Home() {
    const [productos, setProductos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [added, setAdded] = useState({});

    const { addToCart } = useCart();
    const { isLoggedIn, isAdmin } = useAuth();
    const { toast } = useToast();
    const navigate = useNavigate();

    useEffect(() => {
        if (isLoggedIn && isAdmin) {
            navigate('/admin/productos', { replace: true });
            return;
        }
        api.productos.getAll()
            .then(setProductos)
            .catch(() => toast('Error cargando productos', 'error'))
            .finally(() => setLoading(false));
    }, [isAdmin]);

    const handleAdd = (product) => {
        if (!isLoggedIn) {
            toast('Inicia sesión para agregar al carrito');
            navigate('/login');
            return;
        }
        addToCart(product);
        setAdded(a => ({ ...a, [product.id]: true }));
        toast(`"${product.nombre}" añadido al carrito`, 'success');
        setTimeout(() => setAdded(a => ({ ...a, [product.id]: false })), 1500);
    };

    const activos = productos
        .filter(p => p.active)
        .filter(p => !search || p.nombre.toLowerCase().includes(search.toLowerCase()));

    return (
        <>
            {/* ── HERO ─────────────────────────────────────────────────── */}
            <div className="hero">
                <div className="hero-inner">
                    <p className="hero-eyebrow">Colección 2026</p>
                    <h1>
                        Moda que habla<br />
                        <em>por ti</em>
                    </h1>
                    <p>
                        Descubre prendas pensadas para cada momento.
                        Calidad, estilo y autenticidad en cada hilo.
                    </p>
                    <div className="hero-cta">
                        <a href="#catalogo" className="btn btn-accent btn-lg">
                            Ver Catálogo
                        </a>
                        {!isLoggedIn && (
                            <button className="btn btn-outline btn-lg" style={{ color: 'var(--cream)', borderColor: 'rgba(245,240,232,0.2)' }} onClick={() => navigate('/login')}>
                                Iniciar Sesión
                            </button>
                        )}
                    </div>

                    <div className="hero-stats">
                        <div>
                            <div className="hero-stat-num">{productos.filter(p => p.active).length}+</div>
                            <div className="hero-stat-label">Productos</div>
                        </div>
                        <div>
                            <div className="hero-stat-num">100%</div>
                            <div className="hero-stat-label">Calidad garantizada</div>
                        </div>
                        <div>
                            <div className="hero-stat-num">24h</div>
                            <div className="hero-stat-label">Despacho rápido</div>
                        </div>
                    </div>
                </div>
            </div>

            {/* ── CATÁLOGO ─────────────────────────────────────────────── */}
            <div id="catalogo" className="container page-wrapper">
                <div className="section-title">
                    <h2>Catálogo</h2>
                    <div className="section-line" />
                    <span className="section-count">{activos.length} productos</span>
                </div>

                {/* Búsqueda */}
                <div style={{ marginBottom: '2rem', maxWidth: '400px' }}>
                    <input
                        className="form-control"
                        placeholder="Buscar producto..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                    />
                </div>

                {loading ? (
                    <div className="loading-center"><div className="loading-ring" /></div>
                ) : activos.length === 0 ? (
                    <div className="empty-state">
                        <div className="empty-icon">🔍</div>
                        <h3>Sin resultados</h3>
                        <p>No encontramos productos con ese nombre.</p>
                    </div>
                ) : (
                    <div className="products-grid">
                        {activos.map(p => (
                            <article key={p.id} className="product-card">
                                <div className="product-img">
                                    <span className="product-tag">{p.categoria}</span>
                                </div>
                                <div className="product-body">
                                    <div className="product-cat">{p.categoria}</div>
                                    <div className="product-name">{p.nombre}</div>
                                    {p.stock <= 5 && p.stock > 0 && (
                                        <div className="stock-low">Solo {p.stock} en stock</div>
                                    )}
                                    <div className="product-price">
                                        {fmt.price(p.precio)}
                                        <small>USD</small>
                                    </div>
                                    <button
                                        className={`btn btn-full ${added[p.id] ? 'btn-success' : 'btn-accent'}`}
                                        onClick={() => handleAdd(p)}
                                        disabled={p.stock === 0}
                                    >
                                        {p.stock === 0
                                            ? 'Sin stock'
                                            : added[p.id]
                                                ? '✓ Agregado'
                                                : 'Añadir al carrito'}
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </div>
        </>
    );
}
