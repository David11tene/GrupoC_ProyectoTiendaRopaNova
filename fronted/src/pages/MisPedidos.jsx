import React, { useEffect, useState } from 'react';
import { api, fmt } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';

export default function MisPedidos() {
    const [pedidos, setPedidos] = useState([]);
    const [loading, setLoading] = useState(true);
    const { user } = useAuth();

    useEffect(() => {
        api.pedidos.getAll()
            .then(data => {
                // El backend devuelve p.usuario = nombre del usuario
                setPedidos(data.filter(p => p.usuario === user.nombre));
            })
            .finally(() => setLoading(false));
    }, [user]);

    if (loading) return (
        <div className="container page-wrapper">
            <div className="loading-center"><div className="loading-ring" /></div>
        </div>
    );

    return (
        <div className="container page-wrapper">
            <div style={{ marginBottom: '2rem' }}>
                <h2>Mis Pedidos</h2>
                <p style={{ color: 'var(--muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                    Historial de compras de {user.nombre}
                </p>
            </div>

            {pedidos.length === 0 ? (
                <div className="empty-state">
                    <div className="empty-icon">📦</div>
                    <h3>Aún no tienes pedidos</h3>
                    <p>Visita la tienda y realiza tu primera compra.</p>
                    <Link to="/" className="btn btn-accent" style={{ marginTop: '1.5rem' }}>
                        Ir a la Tienda
                    </Link>
                </div>
            ) : (
                <div className="table-wrap">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Pedido</th>
                                <th>Fecha</th>
                                <th>Total</th>
                                <th>Estado</th>
                            </tr>
                        </thead>
                        <tbody>
                            {pedidos.map(p => {
                                const { label, cls } = fmt.estado(p.estado);
                                return (
                                    <tr key={p.id}>
                                        <td><strong>#{p.id}</strong></td>
                                        <td>{fmt.date(p.fecha)}</td>
                                        <td><strong>{fmt.price(p.total)}</strong></td>
                                        <td><span className={`badge ${cls}`}>{label}</span></td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
