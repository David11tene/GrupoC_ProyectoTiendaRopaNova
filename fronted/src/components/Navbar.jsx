import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

export default function Navbar() {
    const { user, isLoggedIn, isAdmin, logout } = useAuth();
    const { totalItems } = useCart();
    const location = useLocation();
    const navigate = useNavigate();
    const p = location.pathname;

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <nav className="navbar">
            <div className="navbar-inner">
                {/* Brand */}
                <Link to={isAdmin ? '/admin/productos' : '/'} className="navbar-brand">
                    Nova<span>.</span>
                </Link>

                {/* Links */}
                <div className="navbar-links">
                    {!isAdmin && (
                        <Link to="/" className={`nav-link ${p === '/' ? 'active' : ''}`}>
                            Tienda
                        </Link>
                    )}

                    {isLoggedIn && !isAdmin && (
                        <>
                            <Link to="/cart" className={`cart-pill ${p === '/cart' ? 'active' : ''}`}>
                                🛍 Carrito
                                {totalItems > 0 && <span className="cart-count">{totalItems}</span>}
                            </Link>
                            <Link to="/mis-pedidos" className={`nav-link ${p === '/mis-pedidos' ? 'active' : ''}`}>
                                Mis Pedidos
                            </Link>
                        </>
                    )}

                    {isLoggedIn && isAdmin && (
                        <Link to="/admin/productos" className={`nav-link ${p.startsWith('/admin') ? 'active' : ''}`}>
                            Panel Admin
                        </Link>
                    )}
                </div>

                {/* User area */}
                <div className="navbar-user">
                    {isLoggedIn ? (
                        <>
              <span className="user-chip">
                {isAdmin ? '⚡ ' : ''}
                  <strong>{user.nombre}</strong>
              </span>
                            <button className="btn btn-logout" onClick={handleLogout}>
                                Salir
                            </button>
                        </>
                    ) : (
                        <Link to="/login" className="btn btn-accent btn-sm">
                            Ingresar
                        </Link>
                    )}
                </div>
            </div>
        </nav>
    );
}
