import React, { useState, useEffect, useRef } from 'react';
import { api, fmt } from '../../services/api';
import { useToast } from '../../context/ToastContext';

// Flujo comercial de estados: PENDIENTE -> EN_PREPARACION -> DESPACHADO -> ENTREGADO (o RECHAZADO)
const NEXT_ESTADOS = {
    PENDIENTE: [
        { key: 'EN_PREPARACION', label: 'Iniciar Preparación y Empaquetado', cls: 'btn-success' },
        { key: 'RECHAZADO', label: 'Rechazar Pedido', cls: 'btn-danger' }
    ],
    APROBADO: [
        { key: 'EN_PREPARACION', label: 'Iniciar Preparación y Empaquetado', cls: 'btn-success' },
        { key: 'DESPACHADO', label: 'Marcar como Despachado', cls: 'btn-accent' },
        { key: 'RECHAZADO', label: 'Cancelar Pedido', cls: 'btn-outline' }
    ],
    EN_PREPARACION: [
        { key: 'DESPACHADO', label: 'Marcar como Despachado y en Ruta', cls: 'btn-accent' },
        { key: 'RECHAZADO', label: 'Cancelar Pedido', cls: 'btn-outline' }
    ],
    ENVIADO: [
        { key: 'ENTREGADO', label: 'Confirmar Entrega al Cliente', cls: 'btn-success' }
    ],
    DESPACHADO: [
        { key: 'ENTREGADO', label: 'Confirmar Entrega al Cliente', cls: 'btn-success' }
    ],
    RECHAZADO: [],
    ENTREGADO: [],
};

export default function Pedidos() {
    const [pedidos, setPedidos] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('TODOS');
    const [searchQuery, setSearchQuery] = useState('');
    const [promedio, setPromedio] = useState(null);
    const [realtimeStatus, setRealtimeStatus] = useState('conectando');

    // Módulo de Logística y Envíos Masivos
    const [capacidadLote, setCapacidadLote] = useState(3);
    const [despachando, setDespachando] = useState(false);
    const [despachoItems, setDespachoItems] = useState([]);
    const [loteActual, setLoteActual] = useState(0);
    const [procesandoBodega, setProcesandoBodega] = useState(false);
    const [expandedId, setExpandedId] = useState(null);
    const [confirmModal, setConfirmModal] = useState(null); // { id, nuevoEstado, accionTexto }

    const sseRef = useRef(null);
    const { toast } = useToast();

    useEffect(() => {
        load();

        // Recepción en tiempo real de nuevas compras de clientes
        const source = new EventSource('/api/reactivo/pedidos/stream');
        source.onopen = () => setRealtimeStatus('conectado');
        source.onmessage = (event) => {
            const pedidoNuevo = JSON.parse(event.data);
            setPedidos(prev => {
                const existe = prev.some(p => p.id === pedidoNuevo.id);
                return existe
                    ? prev.map(p => (p.id === pedidoNuevo.id ? pedidoNuevo : p))
                    : [pedidoNuevo, ...prev];
            });
            toast(`¡Nueva compra recibida! Pedido #${pedidoNuevo.id}`, 'info');
        };
        source.onerror = () => setRealtimeStatus('reconectando');

        // Recepción en vivo del promedio de ventas
        const promedioSource = new EventSource('/api/reactivo/pedidos/promedio-stream');
        promedioSource.onmessage = (event) => setPromedio(Number(event.data));

        return () => {
            source.close();
            promedioSource.close();
            if (sseRef.current) sseRef.current.close();
        };
    }, []);

    const load = () => {
        api.pedidos.getAll()
            .then(data => setPedidos(Array.isArray(data) ? data : []))
            .catch(() => toast('Error al cargar la lista de pedidos', 'error'))
            .finally(() => setLoading(false));
    };

    // Agrupación y despacho en bloques para bodega
    const handleIniciarCargaLotes = () => {
        if (sseRef.current) sseRef.current.close();

        setDespachoItems([]);
        setLoteActual(1);
        setDespachando(true);

        const url = `/api/reactivo/pedidos/stream/lotes?batchSize=${capacidadLote}&delayMs=500`;
        const source = new EventSource(url);
        sseRef.current = source;

        let rec = 0;
        source.onmessage = (event) => {
            const item = JSON.parse(event.data);
            rec++;
            const numLote = Math.ceil(rec / capacidadLote);
            setLoteActual(numLote);
            setDespachoItems(prev => [...prev, { ...item, numLote }]);
        };

        source.onerror = () => {
            source.close();
            setDespachando(false);
            toast('Organización por paquetes de entrega completada.', 'info');
        };
    };

    const handleDetenerCarga = () => {
        if (sseRef.current) {
            sseRef.current.close();
            sseRef.current = null;
        }
        setDespachando(false);
    };

    const handleProcesarDespachosMasivos = async () => {
        setProcesandoBodega(true);
        try {
            await api.pedidos.procesarLotes(capacidadLote, 400);
            toast(`Envíos autorizados y transferidos a Bodega (Paquetes de ${capacidadLote} órdenes)`, 'success');
        } catch (err) {
            toast(err.message || 'Error al enviar a Bodega', 'error');
        } finally {
            setProcesandoBodega(false);
        }
    };

    const solicitarCambioEstado = (id, nuevoEstado, label) => {
        if (nuevoEstado === 'RECHAZADO') {
            setConfirmModal({ id, nuevoEstado, accionTexto: 'rechazar este pedido' });
        } else {
            ejecutarCambioEstado(id, nuevoEstado);
        }
    };

    const ejecutarCambioEstado = async (id, nuevoEstado) => {
        try {
            await api.pedidos.updateEstado(id, nuevoEstado);
            const statusLabels = {
                APROBADO: 'Pedido Aprobado exitosamente',
                EN_PREPARACION: 'Pedido en Preparación y Empaquetado',
                RECHAZADO: 'Pedido Rechazado y Stock Reincorporado',
                ENVIADO: 'Pedido marcado como Despachado y en Ruta',
                DESPACHADO: 'Pedido marcado como Despachado y en Ruta',
                ENTREGADO: 'Entrega de Pedido confirmada',
            };
            toast(`${statusLabels[nuevoEstado] || 'Pedido actualizado'} (Orden #${id})`, 'success');
            setConfirmModal(null);
            load();
        } catch (err) {
            toast(err.message || 'Error al cambiar el estado del pedido', 'error');
        }
    };

    // Métricas para tarjetas superiores
    const totalPendientes = pedidos.filter(p => p.estado === 'PENDIENTE').length;
    const totalPreparacion = pedidos.filter(p => p.estado === 'EN_PREPARACION' || p.estado === 'APROBADO').length;
    const totalDespachados = pedidos.filter(p => p.estado === 'DESPACHADO' || p.estado === 'ENVIADO').length;

    // Filtros comerciales
    const FILTROS = ['TODOS', 'PENDIENTE', 'EN_PREPARACION', 'DESPACHADO', 'ENTREGADO', 'RECHAZADO'];
    
    let listaBase = despachoItems.length > 0 ? despachoItems : pedidos;
    if (filter !== 'TODOS') {
        listaBase = listaBase.filter(p => p.estado === filter);
    }
    if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase().trim();
        listaBase = listaBase.filter(p => 
            p.id.toString().includes(q) || 
            (p.usuario && p.usuario.toLowerCase().includes(q))
        );
    }

    return (
        <>
            {/* Encabezado Comercial Principal */}
            <div className="admin-page-header">
                <div>
                    <h2>Gestión de Pedidos y Logística</h2>
                    <p>Revisa las solicitudes de compra, aprueba o rechaza pedidos y coordina el envío de prendas</p>
                </div>
            </div>

            {/* Modal de Confirmación para Rechazo */}
            {confirmModal && (
                <div style={{
                    position: 'fixed',
                    top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 1000
                }}>
                    <div style={{
                        background: 'var(--card)',
                        border: '1px solid var(--border)',
                        borderRadius: '12px',
                        padding: '1.75rem',
                        maxWidth: '420px',
                        width: '90%',
                        boxShadow: 'var(--shadow-lg)'
                    }}>
                        <h3 style={{ margin: '0 0 0.75rem 0', color: 'var(--danger)' }}>Confirmar Acción</h3>
                        <p style={{ fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                            ¿Estás seguro de que deseas <strong>{confirmModal.accionTexto}</strong> (# {confirmModal.id})? Esta acción cambiará el estado de la compra.
                        </p>
                        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                            <button
                                className="btn btn-outline btn-sm"
                                onClick={() => setConfirmModal(null)}
                            >
                                Cancelar
                            </button>
                            <button
                                className="btn btn-danger btn-sm"
                                onClick={() => ejecutarCambioEstado(confirmModal.id, confirmModal.nuevoEstado)}
                            >
                                Sí, Rechazar Pedido
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Tarjetas KPI de Resumen Comercial */}
            <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
                marginBottom: '1.5rem'
            }}>
                <div style={{ background: 'var(--card)', border: '1px solid var(--border)', padding: '1rem 1.25rem', borderRadius: '10px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Total de Pedidos</span>
                    <h3 style={{ margin: '0.2rem 0 0 0', fontSize: '1.6rem' }}>{pedidos.length}</h3>
                </div>

                <div style={{ background: 'var(--card)', border: '1px solid var(--border)', padding: '1rem 1.25rem', borderRadius: '10px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Por Aprobar (Pendientes)</span>
                    <h3 style={{ margin: '0.2rem 0 0 0', fontSize: '1.6rem', color: totalPendientes > 0 ? '#eab308' : 'inherit' }}>
                        {totalPendientes}
                    </h3>
                </div>

                <div style={{ background: 'var(--card)', border: '1px solid var(--border)', padding: '1rem 1.25rem', borderRadius: '10px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>En Preparación y Empaquetado</span>
                    <h3 style={{ margin: '0.2rem 0 0 0', fontSize: '1.6rem', color: 'var(--success)' }}>
                        {totalPreparacion}
                    </h3>
                </div>

                <div style={{ background: 'var(--card)', border: '1px solid var(--border)', padding: '1rem 1.25rem', borderRadius: '10px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Despachados y en Ruta</span>
                    <h3 style={{ margin: '0.2rem 0 0 0', fontSize: '1.6rem', color: '#3b82f6' }}>
                        {totalDespachados}
                    </h3>
                </div>

                <div style={{ background: 'var(--card)', border: '1px solid var(--border)', padding: '1rem 1.25rem', borderRadius: '10px' }}>
                    <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Valor Promedio por Compra</span>
                    <h3 style={{ margin: '0.2rem 0 0 0', fontSize: '1.6rem', color: 'var(--accent)' }}>
                        {promedio === null ? 'Calculando...' : `$${parseFloat(promedio).toFixed(2)}`}
                    </h3>
                </div>
            </div>

            {/* Módulo Logístico de Preparación y Despacho en Bloques */}
            <div style={{
                background: 'var(--card)',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                padding: '1.25rem',
                marginBottom: '1.5rem',
                boxShadow: '0 2px 8px rgba(0,0,0,0.04)'
            }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div>
                        <h4 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                            Centro de Organización y Empaquetado de Envíos
                        </h4>
                        <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.8rem', color: 'var(--muted)' }}>
                            Organiza y agrupa los pedidos confirmados en paquetes de distribución para agilizar las entregas
                        </p>
                    </div>
                    {despachando && (
                        <span className="badge badge-success">
                            Organizando Paquete #{loteActual}
                        </span>
                    )}
                </div>

                <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
                    <div style={{ width: '240px' }}>
                        <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                            Tamaño del Paquete de Envío:
                        </label>
                        <select
                            className="form-select btn-sm"
                            style={{ width: '100%', padding: '0.45rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border)' }}
                            value={capacidadLote}
                            onChange={(e) => setCapacidadLote(Number(e.target.value))}
                            disabled={despachando}
                        >
                            <option value={2}>2 pedidos por paquete</option>
                            <option value={3}>3 pedidos por paquete</option>
                            <option value={5}>5 pedidos por paquete</option>
                            <option value={10}>10 pedidos por paquete</option>
                        </select>
                    </div>

                    <div style={{ display: 'flex', gap: '0.6rem', marginTop: '1.2rem', flexWrap: 'wrap' }}>
                        {!despachando ? (
                            <button className="btn btn-accent btn-sm" onClick={handleIniciarCargaLotes}>
                                Agrupar Envíos en Bloques de {capacidadLote}
                            </button>
                        ) : (
                            <button className="btn btn-danger btn-sm" onClick={handleDetenerCarga}>
                                Pausar Agrupación
                            </button>
                        )}

                        <button
                            className="btn btn-outline btn-sm"
                            onClick={handleProcesarDespachosMasivos}
                            disabled={procesandoBodega}
                        >
                            {procesandoBodega ? 'Procesando Envíos...' : 'Transferir Paquetes a Distribución'}
                        </button>

                        {despachoItems.length > 0 && (
                            <button className="btn btn-outline btn-sm" onClick={() => setDespachoItems([])}>
                                Limpiar Filtro de Envíos
                            </button>
                        )}
                    </div>
                </div>

                {/* Avance de Carga del Paquete */}
                {despachando && (
                    <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.4rem' }}>
                            <span>Paquete de Envío Activo: <strong>#{loteActual}</strong></span>
                            <span>Órdenes organizadas: <strong>{despachoItems.length}</strong> (Bloques de {capacidadLote})</span>
                        </div>
                        <div style={{ width: '100%', height: '8px', background: 'var(--border)', borderRadius: '4px', overflow: 'hidden' }}>
                            <div style={{
                                width: `${Math.min(100, ((despachoItems.length % capacidadLote) || capacidadLote) * (100 / capacidadLote))}%`,
                                height: '100%',
                                background: 'var(--accent)',
                                transition: 'width 0.3s ease'
                            }} />
                        </div>
                    </div>
                )}
            </div>

            {/* Buscador y Filtros por Estado Comercial */}
            <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '1.25rem',
                flexWrap: 'wrap',
                gap: '1rem'
            }}>
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600, marginRight: '4px' }}>Estado de Pedido:</span>
                    {FILTROS.map(f => {
                        const count = pedidos.filter(p => p.estado === f).length;
                        return (
                            <button
                                key={f}
                                className={`btn btn-sm ${filter === f ? 'btn-accent' : 'btn-outline'}`}
                                onClick={() => { setFilter(f); setDespachoItems([]); }}
                                style={{
                                    borderWidth: filter === f ? '2px' : '1px',
                                    fontWeight: filter === f ? 700 : 500
                                }}
                            >
                                {f === 'TODOS' ? 'Todos' : fmt.estado(f).label}
                                {f !== 'TODOS' && (
                                    <span style={{ marginLeft: '4px', opacity: 0.85 }}>
                                        ({count})
                                    </span>
                                )}
                            </button>
                        );
                    })}
                </div>

                <div style={{ width: '260px' }}>
                    <input
                        type="text"
                        className="form-control"
                        placeholder="Buscar por # Pedido o Cliente..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        style={{ fontSize: '0.82rem', padding: '0.45rem 0.75rem' }}
                    />
                </div>
            </div>

            {/* Tabla Principal de Pedidos */}
            {loading ? (
                <div className="loading-center"><div className="loading-ring" /></div>
            ) : (
                <div className="table-wrap">
                    <table className="data-table">
                        <thead>
                        <tr>
                            <th>Pedido #</th>
                            <th>Grupo de Envío</th>
                            <th>Fecha</th>
                            <th>Cliente</th>
                            <th>Prendas Solicitadas</th>
                            <th>Monto Total</th>
                            <th>Estado Actual</th>
                            <th>Acciones de Aprobación y Despacho</th>
                        </tr>
                        </thead>
                        <tbody>
                        {listaBase.map((p, idx) => {
                            const { label, cls } = fmt.estado(p.estado);
                            const acciones = NEXT_ESTADOS[p.estado] || [];
                            const itemsCount = p.detalles ? p.detalles.reduce((acc, item) => acc + (item.cantidad || 1), 0) : 0;
                            const isExpanded = expandedId === p.id;
                            const esPendiente = p.estado === 'PENDIENTE';

                            return (
                                <React.Fragment key={`${p.id}-${idx}`}>
                                    <tr style={{
                                        background: esPendiente ? 'rgba(234, 179, 8, 0.06)' : isExpanded ? 'rgba(59, 130, 246, 0.05)' : undefined
                                    }}>
                                        <td>
                                            <strong>#{p.id}</strong>
                                            {esPendiente && (
                                                <span style={{ display: 'block', fontSize: '0.70rem', color: '#d97706', fontWeight: 600 }}>
                                                    Requiere Aprobación
                                                </span>
                                            )}
                                        </td>
                                        <td>
                                            {p.numLote ? (
                                                <span className="badge badge-info" style={{ fontWeight: 600 }}>
                                                    Paquete #{p.numLote}
                                                </span>
                                            ) : (
                                                <span style={{ fontSize: '0.75rem', opacity: 0.6 }}>Estándar</span>
                                            )}
                                        </td>
                                        <td className="text-sm">{fmt.date(p.fecha)}</td>
                                        <td><strong>{p.usuario}</strong></td>
                                        <td>
                                            <button
                                                className={`btn btn-sm ${isExpanded ? 'btn-accent' : 'btn-outline'}`}
                                                style={{ fontSize: '0.78rem', padding: '0.3rem 0.65rem', borderRadius: '6px' }}
                                                onClick={() => setExpandedId(isExpanded ? null : p.id)}
                                            >
                                                {isExpanded ? 'Ocultar Detalles' : `Ver Prendas ${itemsCount > 0 ? `(${itemsCount})` : ''}`}
                                            </button>
                                        </td>
                                        <td><strong style={{ color: 'var(--accent)', fontSize: '0.95rem' }}>{fmt.price(p.total)}</strong></td>
                                        <td><span className={`badge ${cls}`}>{label}</span></td>
                                        <td>
                                            {acciones.length > 0 ? (
                                                <div className="action-bar" style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                                                    {acciones.map(a => (
                                                        <button
                                                            key={a.key}
                                                            className={`btn btn-sm ${a.cls}`}
                                                            onClick={() => solicitarCambioEstado(p.id, a.key, a.label)}
                                                        >
                                                            {a.label}
                                                        </button>
                                                    ))}
                                                </div>
                                            ) : (
                                                <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 500 }}>
                                                    {p.estado === 'ENTREGADO' ? 'Pedido Completado' : 'Pedido Cancelado/Rechazado'}
                                                </span>
                                            )}
                                        </td>
                                    </tr>

                                    {/* Ficha Desglosada de Prendas Solicitadas */}
                                    {isExpanded && (
                                        <tr style={{ background: 'rgba(59, 130, 246, 0.03)' }}>
                                            <td colSpan={8} style={{ padding: '0.85rem 1.25rem' }}>
                                                <div style={{
                                                    background: 'var(--card)',
                                                    border: '1px solid var(--border)',
                                                    borderRadius: '10px',
                                                    padding: '1rem 1.25rem',
                                                    boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
                                                }}>
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
                                                        <strong style={{ fontSize: '0.9rem', color: 'var(--accent)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                                            Desglose de Prendas Compradas — Pedido #{p.id}
                                                        </strong>
                                                        <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
                                                            Cliente: {p.usuario} | Fecha de Compra: {fmt.date(p.fecha)}
                                                        </span>
                                                    </div>

                                                    {p.detalles && p.detalles.length > 0 ? (
                                                        <>
                                                            <table className="data-table" style={{ width: '100%', fontSize: '0.85rem', margin: 0 }}>
                                                                <thead>
                                                                <tr style={{ background: 'rgba(0,0,0,0.02)' }}>
                                                                    <th style={{ padding: '8px 12px' }}>Prenda / Producto Seleccionado</th>
                                                                    <th style={{ padding: '8px 12px' }}>Cantidad Solicitada</th>
                                                                    <th style={{ padding: '8px 12px' }}>Stock Actual en Inventario</th>
                                                                    <th style={{ padding: '8px 12px' }}>Precio Unitario</th>
                                                                    <th style={{ padding: '8px 12px', textAlign: 'right' }}>Subtotal</th>
                                                                </tr>
                                                                </thead>
                                                                <tbody>
                                                                {p.detalles.map((d, dIdx) => {
                                                                    const unitPrice = d.precioUnitario || (d.cantidad ? d.subtotal / d.cantidad : d.subtotal);
                                                                    const tieneStock = d.stockDisponible === undefined || d.stockDisponible === null || d.stockDisponible >= 0;
                                                                    return (
                                                                        <tr key={d.id || dIdx}>
                                                                            <td style={{ padding: '8px 12px' }}>
                                                                                <strong>{d.producto}</strong>
                                                                            </td>
                                                                            <td style={{ padding: '8px 12px' }}>
                                                                                <span className="badge badge-neutral" style={{ fontSize: '0.78rem' }}>
                                                                                    {d.cantidad} {d.cantidad === 1 ? 'unidad' : 'unidades'}
                                                                                </span>
                                                                            </td>
                                                                            <td style={{ padding: '8px 12px' }}>
                                                                                {d.stockDisponible !== undefined && d.stockDisponible !== null ? (
                                                                                    <span className={`badge ${d.stockDisponible > 0 ? 'badge-success' : 'badge-danger'}`} style={{ fontSize: '0.78rem' }}>
                                                                                        {d.stockDisponible > 0 ? `${d.stockDisponible} u. Disponibles` : 'Agotado'}
                                                                                    </span>
                                                                                ) : (
                                                                                    <span className="badge badge-success" style={{ fontSize: '0.78rem' }}>
                                                                                        Stock Verificado
                                                                                    </span>
                                                                                )}
                                                                            </td>
                                                                            <td style={{ padding: '8px 12px' }}>{fmt.price(unitPrice)}</td>
                                                                            <td style={{ padding: '8px 12px', textAlign: 'right' }}>
                                                                                <strong>{fmt.price(d.subtotal)}</strong>
                                                                            </td>
                                                                        </tr>
                                                                    );
                                                                })}
                                                                <tr style={{ borderTop: '2px solid var(--border)', background: 'rgba(0,0,0,0.02)' }}>
                                                                    <td colSpan={4} style={{ padding: '10px 12px', textAlign: 'right' }}>
                                                                        <strong style={{ fontSize: '0.9rem' }}>Monto Total del Pedido:</strong>
                                                                    </td>
                                                                    <td style={{ padding: '10px 12px', textAlign: 'right', color: 'var(--accent)', fontSize: '1.05rem' }}>
                                                                        <strong>{fmt.price(p.total)}</strong>
                                                                    </td>
                                                                </tr>
                                                                </tbody>
                                                            </table>

                                                            {/* Nota de Lógica de Negocio Comercial */}
                                                            <div style={{
                                                                marginTop: '0.75rem',
                                                                padding: '0.65rem 0.85rem',
                                                                background: 'rgba(59, 130, 246, 0.08)',
                                                                borderLeft: '4px solid #3b82f6',
                                                                borderRadius: '4px',
                                                                fontSize: '0.78rem',
                                                                color: 'var(--ink-soft)'
                                                            }}>
                                                                <strong>Estado y Validación del Inventario:</strong>
                                                                <ul style={{ margin: '0.2rem 0 0 1.2rem', padding: 0 }}>
                                                                    <li><strong>Iniciar Preparación y Empaquetado:</strong> Confirma la asignación de prendas en almacén y la elaboración del paquete de envío.</li>
                                                                    <li><strong>Rechazar o Cancelar Pedido:</strong> Cancela la orden e incrementa de forma automática las unidades de vuelta al inventario disponible.</li>
                                                                </ul>
                                                            </div>
                                                        </>
                                                    ) : (
                                                        <div style={{ fontSize: '0.85rem', color: 'var(--muted)', padding: '0.75rem 0', textAlign: 'center' }}>
                                                            Compra registrada con monto total de ({fmt.price(p.total)}).
                                                        </div>
                                                    )}

                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </React.Fragment>
                            );
                        })}
                        {listaBase.length === 0 && (
                            <tr>
                                <td colSpan={8} style={{ textAlign: 'center', color: 'var(--muted)', padding: '3rem' }}>
                                    {searchQuery 
                                        ? `No se encontraron pedidos que coincidan con "${searchQuery}"`
                                        : `No hay pedidos registrados con el filtro "${filter}"`}
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
