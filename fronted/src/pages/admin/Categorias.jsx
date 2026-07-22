import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../context/ToastContext';

export default function Categorias() {
    const [categorias, setCategorias] = useState([]);
    const [loading, setLoading] = useState(true);
    const [nombre, setNombre] = useState('');
    const [editingId, setEditingId] = useState(null);
    const [saving, setSaving] = useState(false);
    const { toast } = useToast();

    useEffect(() => { load(); }, []);

    const load = () => {
        api.categorias.getAll()
            .then(setCategorias)
            .catch(() => toast('Error cargando categorías', 'error'))
            .finally(() => setLoading(false));
    };

    const reset = () => { setNombre(''); setEditingId(null); };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            if (editingId) {
                await api.categorias.update(editingId, { nombre });
                toast('Categoría actualizada', 'success');
            } else {
                await api.categorias.create({ nombre });
                toast('Categoría creada', 'success');
            }
            reset();
            load();
        } catch (err) {
            toast(err.message || 'Error al guardar', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm('¿Eliminar esta categoría?')) return;
        try {
            await api.categorias.delete(id);
            toast('Categoría eliminada', 'success');
            load();
        } catch (err) {
            toast(err.message || 'No se puede eliminar (tiene productos asociados)', 'error');
        }
    };

    return (
        <>
            <div className="admin-page-header">
                <div>
                    <h2>Categorías</h2>
                    <p>Organiza el catálogo por categoría</p>
                </div>
                <span className="badge badge-neutral">{categorias.length} total</span>
            </div>

            <div className="admin-split">
                {/* Formulario */}
                <div className="form-panel">
                    <div className="form-panel-header">
                        {editingId ? 'Editar categoría' : '+ Nueva categoría'}
                    </div>
                    <div className="form-panel-body">
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Nombre</label>
                                <input
                                    className="form-control"
                                    value={nombre}
                                    onChange={e => setNombre(e.target.value)}
                                    placeholder="Ej: Camisas, pantalones..."
                                    required
                                    minLength={3}
                                    maxLength={100}
                                />
                            </div>
                            <div className="action-bar">
                                <button type="submit" className="btn btn-accent" disabled={saving}>
                                    {saving ? 'Guardando...' : 'Guardar'}
                                </button>
                                {editingId && (
                                    <button type="button" className="btn btn-outline" onClick={reset}>
                                        Cancelar
                                    </button>
                                )}
                            </div>
                        </form>
                    </div>
                </div>

                {/* Tabla */}
                {loading ? (
                    <div className="loading-center"><div className="loading-ring" /></div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>ID </th>
                                    <th>Nombre </th>
                                    <th>Acciones </th>
                                </tr>
                            </thead>
                            <tbody>
                                {categorias.map(c => (
                                    <tr key={c.id}>
                                        <td className="text-muted text-sm">#{c.id}</td>
                                        <td><strong>{c.nombre}</strong></td>
                                        <td>
                                            <div className="action-bar">
                                                <button className="btn btn-sm btn-outline" onClick={() => { setEditingId(c.id); setNombre(c.nombre); }}>Editar </button>
                                                <button className="btn btn-sm btn-danger" onClick={() => handleDelete(c.id)}>Eliminar</button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {categorias.length === 0 && (
                                    <tr><td colSpan={3} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2rem' }}>Sin categorías aún</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}
