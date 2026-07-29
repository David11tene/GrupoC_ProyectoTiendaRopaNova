import React from 'react';
import { Outlet, NavLink } from 'react-router-dom';

const LINKS = [
    { to: '/admin/productos', label: 'Productos' },
    { to: '/admin/categorias', label: 'Categorías' },
    { to: '/admin/usuarios', label: 'Usuarios' },
    { to: '/admin/pedidos', label: 'Pedidos' },
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
