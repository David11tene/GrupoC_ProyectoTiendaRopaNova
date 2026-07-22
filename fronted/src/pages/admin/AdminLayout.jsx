import React from 'react';
import { Outlet, NavLink } from 'react-router-dom';

const LINKS = [
    { to: '/admin/productos', icon: '👕 ', label: 'Productos' },
    { to: '/admin/categorias', icon: '🏷 ', label: 'Categorías' },
    { to: '/admin/usuarios', icon: '👤 ', label: 'Usuarios' },
    { to: '/admin/pedidos', icon: '📦 ', label: 'Pedidos' },
];

export default function AdminLayout() {
    return (
        <div className="admin-shell">
            <aside className="admin-sidebar">
                <div className="sidebar-section">
                    <div className="sidebar-label"> Panel de control </div>
                    {LINKS.map(l => (
                        <NavLink
                            key={l.to}
                            to={l.to}
                            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
                        >
                            <span>{l.icon}</span>
                            {l.label}
                        </NavLink>
                    ))}
                </div>
            </aside>

            <main className="admin-content">
                <Outlet />
            </main>
        </div>
    );
}
