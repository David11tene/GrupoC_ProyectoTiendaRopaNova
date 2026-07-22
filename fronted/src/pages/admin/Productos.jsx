import React, { useState, useEffect } from 'react';
import { api, fmt } from '../../services/api';
import { useToast } from '../../context/ToastContext';

const emptyForm = { nombre: '', precio: '', stock: '', categoriaId: '' };

export default function Productos() {
    const [productos, setProductos] = useState([]);
    const [categorias, setCategorias] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [saving, setSaving] = useState(false);
    const { toast } = useToast();

    useEffect(() => { load(); }, []);

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
                                <label>Nombre</label>
                                <input className="form-control" name="nombre" value={form.nombre} onChange={handleChange} required minLength={3} maxLength={100} />
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
                                    <th>Producto</th>
                                    <th>Precio / Stock</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {productos.map(p => (
                                    <tr key={p.id}>
                                        <td>
                                            <strong>{p.nombre}</strong>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>{p.categoria}</div>
                                        </td>
                                        <td>
                                            <strong>{fmt.price(p.precio)}</strong>
                                            <div style={{ fontSize: '0.75rem', color: 'var(--muted)' }}>{p.stock} u.</div>
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
                                    <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2rem' }}>Sin productos</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}
