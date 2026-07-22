import React, { useState, useEffect } from 'react';
import { api, fmt } from '../../services/api';
import { useToast } from '../../context/ToastContext';

// Estados válidos en el backend: PENDIENTE | APROBADO | RECHAZADO | ENVIADO | ENTREGADO
const NEXT_ESTADOS = {
    PENDIENTE: ['APROBADO', 'RECHAZADO'],
    APROBADO:  ['ENVIADO', 'RECHAZADO'],
    ENVIADO:   ['ENTREGADO'],
    RECHAZADO: [],
    ENTREGADO: [],
};

export default function Pedidos() {
    const [pedidos,  setPedidos]  = useState([]);
    const [loading,  setLoading]  = useState(true);
    const [filter,   setFilter]   = useState('TODOS');
    const [promedio, setPromedio] = useState(null);
    const [streamStatus, setStreamStatus] = useState('conectando');
    const [processingMessages, setProcessingMessages] = useState([]);
    const [processing, setProcessing] = useState(false);
    const { toast } = useToast();

    useEffect(() => {
        load();

        const source = new EventSource('/api/reactivo/pedidos/stream');
        source.onopen = () => setStreamStatus('conectado');
        source.onmessage = (event) => {
            const pedidoNuevo = JSON.parse(event.data);
            setPedidos(prev => {
                const existe = prev.some(p => p.id === pedidoNuevo.id);
                return existe
                    ? prev.map(p => (p.id === pedidoNuevo.id ? pedidoNuevo : p))
                    : [pedidoNuevo, ...prev];
            });
        };
        source.onerror = () => setStreamStatus('reconectando');

        const promedioSource = new EventSource('/api/reactivo/pedidos/promedio-stream');
        promedioSource.onmessage = (event) => setPromedio(Number(event.data));

        const processingSource = new EventSource('/api/reactivo/pedidos/procesamiento-stream');
        processingSource.onmessage = (event) => {
            setProcessingMessages(prev => [...prev.slice(-19), event.data]);
            if (event.data.includes('Flujo completado') || event.data.includes('Error en el flujo')) {
                setProcessing(false);
            }
        };

        return () => {
            source.close();
            promedioSource.close();
            processingSource.close();
        };
    }, []);

    const load = () => {
        api.pedidos.getAll()
            .then(setPedidos)
            .catch(() => toast('Error cargando pedidos', 'error'))
            .finally(() => setLoading(false));
    };

    const handleProcesarLotes = async () => {
        setProcessingMessages([]);
        setProcessing(true);
        try {
            await api.pedidos.procesarLotes();
        } catch (err) {
            setProcessing(false);
            toast(err.message || 'Error iniciando el procesamiento', 'error');
        }
    };

    const handleEstado = async (id, nuevoEstado) => {
        try {
            // PATCH /api/pedidos/{id}/estado  body: { estado }
            await api.pedidos.updateEstado(id, nuevoEstado);
            toast(`Pedido #${id} → ${nuevoEstado}`, 'success');
            load();
        } catch (err) {
            toast(err.message || 'Error al actualizar estado', 'error');
        }
    };

    const FILTROS = ['TODOS', 'PENDIENTE', 'APROBADO', 'ENVIADO', 'ENTREGADO', 'RECHAZADO'];
    const visibles = filter === 'TODOS' ? pedidos : pedidos.filter(p => p.estado === filter);

    return (
        <>
            <div className="admin-page-header">
                <div>
                    <h2>Pedidos</h2>
                    <p>Gestiona y actualiza el estado de cada pedido</p>
                </div>
                <span className="badge badge-neutral">{pedidos.length} total</span>
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
                <span className="badge badge-info">
                    Promedio reactivo: {promedio === null ? '...' : `$${parseFloat(promedio).toFixed(2)}`}
                </span>
                <span className={`badge ${streamStatus === 'conectado' ? 'badge-success' : 'badge-neutral'}`}>
                    SSE: {streamStatus}
                </span>
                <button
                    className="btn btn-outline btn-sm"
                    onClick={handleProcesarLotes}
                    disabled={processing}
                >
                    {processing ? 'Procesando...' : 'Ejecutar backpressure'}
                </button>
            </div>

            {processingMessages.length > 0 && (
                <div style={{
                    background: 'var(--card)',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                    padding: '0.75rem 1rem',
                    marginBottom: '1rem',
                    fontFamily: 'monospace',
                    fontSize: '0.8rem'
                }}>
                    {processingMessages.map((message, index) => (
                        <div key={`${index}-${message}`}>{message}</div>
                    ))}
                </div>
            )}

            {/* Filtros */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
                {FILTROS.map(f => (
                    <button
                        key={f}
                        className={`btn btn-sm ${filter === f ? 'btn-accent' : 'btn-outline'}`}
                        onClick={() => setFilter(f)}
                    >
                        {f === 'TODOS' ? 'Todos' : fmt.estado(f).label}
                        {f !== 'TODOS' && (
                            <span style={{ marginLeft: '4px', opacity: 0.7 }}>
                ({pedidos.filter(p => p.estado === f).length})
              </span>
                        )}
                    </button>
                ))}
            </div>

            {loading ? (
                <div className="loading-center"><div className="loading-ring" /></div>
            ) : (
                <div className="table-wrap">
                    <table className="data-table">
                        <thead>
                        <tr>
                            <th>Pedido</th>
                            <th>Fecha</th>
                            <th>Cliente</th>
                            <th>Total</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                        </thead>
                        <tbody>
                        {visibles.map(p => {
                            const { label, cls } = fmt.estado(p.estado);
                            const acciones = NEXT_ESTADOS[p.estado] || [];
                            return (
                                <tr key={p.id}>
                                    <td><strong>#{p.id}</strong></td>
                                    <td className="text-sm">{fmt.date(p.fecha)}</td>
                                    <td>{p.usuario}</td>
                                    <td><strong>{fmt.price(p.total)}</strong></td>
                                    <td><span className={`badge ${cls}`}>{label}</span></td>
                                    <td>
                                        {acciones.length > 0 ? (
                                            <div className="action-bar">
                                                {acciones.map(a => {
                                                    const { label: aLabel } = fmt.estado(a);
                                                    const cls = a === 'RECHAZADO' ? 'btn-danger'
                                                        : a === 'APROBADO' ? 'btn-success'
                                                            : 'btn-outline';
                                                    return (
                                                        <button
                                                            key={a}
                                                            className={`btn btn-sm ${cls}`}
                                                            onClick={() => handleEstado(p.id, a)}
                                                        >
                                                            {aLabel}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        ) : (
                                            <span style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>Finalizado</span>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                        {visibles.length === 0 && (
                            <tr>
                                <td colSpan={6} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2.5rem' }}>
                                    No hay pedidos con estado "{filter}"
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            )}
        </>
    );
}
