import React, { useState, useEffect } from 'react';
import { api, fmt } from '../../services/api';
import { useToast } from '../../context/ToastContext';

const emptyForm = { nombre: '', precio: '', stock: '', categoriaId: '', imagenUrl: '' };

export default function Productos() {
    const [productos, setProductos] = useState([]);
    const [categorias, setCategorias] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [saving, setSaving] = useState(false);
    // Lab 3 - WebFlux: promedio de ventas por producto, actualizado en vivo por SSE.
    // Map: productoId -> { promedioVenta, cantidadVentas }
    const [ventasPromedio, setVentasPromedio] = useState({});
    const { toast } = useToast();

    useEffect(() => { load(); }, []);

    // Suscripción SSE al stream de "promedio de ventas" (Lab 3 - Spring WebFlux).
    // Cada evento trae el promedio actualizado de UN producto; se van fusionando
    // en el mapa para refrescar el badge de la tabla sin recargar la página.
    useEffect(() => {
        api.productos.getPromedioVentas()
            .then(lista => {
                const inicial = {};
                (lista || []).forEach(v => { inicial[v.productoId] = v; });
                setVentasPromedio(inicial);
            })
            .catch(() => { /* el badge simplemente no se muestra si falla */ });

        const source = new EventSource(api.productos.promedioVentasStreamUrl());
        source.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                setVentasPromedio(prev => ({ ...prev, [data.productoId]: data }));
            } catch { /* ignorar eventos malformados */ }
        };
        source.onerror = () => { /* EventSource reintenta automáticamente */ };

        return () => source.close();
    }, []);

    const load = () => {
        Promise.all([api.productos.getAll(), api.categorias.getAll()])
            .then(([p, c]) => { setProductos(p); setCategorias(c); })
            .catch(() => toast('Error cargando datos', 'error'))
            .finally(() => setLoading(false));
    };

    const reset = () => { setForm(emptyForm); setEditingId(null); };

    const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

    const handleEdit = (p) => {
        const cat = categorias.find(c => c.nombre === p.categoria);
        setEditingId(p.id);
        setForm({
            nombre: p.nombre,
            precio: p.precio,
            stock: p.stock,
            categoriaId: cat ? cat.id : '',
            imagenUrl: p.imagenUrl || '',
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            // ProductoCreateRequest / ProductoUpdateRequest del backend
            const payload = {
                nombre: form.nombre,
                precio: parseFloat(form.precio),
                stock: parseInt(form.stock),
                categoriaId: parseInt(form.categoriaId),
                imagenUrl: form.imagenUrl.trim() || undefined,
            };
            if (editingId) {
                await api.productos.update(editingId, payload);
                toast('Producto actualizado', 'success');
            } else {
                await api.productos.create(payload);
                toast('Producto creado', 'success');
            }
            reset();
            load();
        } catch (err) {
            toast(err.message || 'Error al guardar', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleToggle = async (p) => {
        try {
            if (p.active) {
                // PATCH /productos/{id}/deactivate
                await api.productos.deactivate(p.id);
                toast(`"${p.nombre}" desactivado`, 'success');
            } else {
                // PUT con active: true para reactivar
                await api.productos.update(p.id, { active: true });
                toast(`"${p.nombre}" activado`, 'success');
            }
            load();
        } catch (err) {
            toast(err.message || 'Error', 'error');
        }
    };

    return (
        <>
            <div className="admin-page-header">
                <div>
                    <h2>Productos</h2>
                    <p>Gestiona el catálogo de la tienda</p>
                </div>
                <span className="badge badge-neutral">{productos.length} total</span>
            </div>

            <div className="admin-split">
                {/*Formulario*/}
                <div className="form-panel">
                    <div className="form-panel-header">
                        {editingId ? 'Editar producto' : '+ Nuevo producto'}
                    </div>
                    <div className="form-panel-body">
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Categoría</label>
                                <select className="form-control" name="categoriaId" value={form.categoriaId} onChange={handleChange} required>
                                    <option value="">-- Seleccionar --</option>
                                    {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                                </select>
                            </div>
                            <div className="form-group">
                                <label>Nombre de la prenda</label>
                                <input className="form-control" name="nombre" value={form.nombre} onChange={handleChange} required minLength={3} maxLength={100} placeholder="Ej. Camisa Oxford Blanca" />
                            </div>
                            <div className="form-group">
                                <label>URL de la Imagen</label>
                                <input className="form-control" name="imagenUrl" value={form.imagenUrl} onChange={handleChange} placeholder="https://ejemplo.com/prenda.jpg" />
                            </div>
                            <div className="form-row">
                                <div className="form-group">
                                    <label>Precio ($)</label>
                                    <input className="form-control" type="number" step="0.01" min="0.01" name="precio" value={form.precio} onChange={handleChange} required />
                                </div>
                                <div className="form-group">
                                    <label>Stock</label>
                                    <input className="form-control" type="number" min="0" name="stock" value={form.stock} onChange={handleChange} required />
                                </div>
                            </div>
                            <div className="action-bar">
                                <button type="submit" className="btn btn-accent" disabled={saving}>
                                    {saving ? 'Guardando...' : 'Guardar'}
                                </button>
                                {editingId && <button type="button" className="btn btn-outline" onClick={reset}>Cancelar</button>}
                            </div>
                        </form>
                    </div>
                </div>

                {/*Tabla*/}
                {loading ? (
                    <div className="loading-center"><div className="loading-ring" /></div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Prenda / Foto</th>
                                    <th>Precio / Stock</th>
                                    <th>Ventas promedio</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {productos.map(p => (
                                    <tr key={p.id}>
                                        <td style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                            <img
                                                src={p.imagenUrl || 'https://images.unsplash.com/photo-1523381294911-8d3cead13475?w=500'}
                                                alt={p.nombre}
                                                style={{ width: '42px', height: '42px', borderRadius: '6px', objectFit: 'cover' }}
                                            />
                                            <div>
                                                <strong>{p.nombre}</strong>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>{p.categoria}</div>
                                            </div>
                                        </td>
                                        <td>
                                            <strong>{fmt.price(p.precio)}</strong>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>{p.stock} u.</div>
                                        </td>
                                        <td>
                                            {ventasPromedio[p.id] && ventasPromedio[p.id].cantidadVentas > 0 ? (
                                                <span className="badge badge-info" title={`${ventasPromedio[p.id].cantidadVentas} venta(s) registradas`}>
                                                    {fmt.price(ventasPromedio[p.id].promedioVenta)} prom.
                                                </span>
                                            ) : (
                                                <span className="badge badge-neutral">Sin ventas</span>
                                            )}
                                        </td>
                                        <td>
                                            <span className={`badge ${p.active ? 'badge-success' : 'badge-danger'}`}>
                                                {p.active ? 'Activo' : 'Inactivo'}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="action-bar">
                                                <button className="btn btn-sm btn-outline" onClick={() => handleEdit(p)}>Editar</button>
                                                <button
                                                    className={`btn btn-sm ${p.active ? 'btn-danger' : 'btn-success'}`}
                                                    onClick={() => handleToggle(p)}
                                                >
                                                    {p.active ? 'Desactivar' : 'Activar'}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {productos.length === 0 && (
                                    <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2rem' }}>Sin productos</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}
