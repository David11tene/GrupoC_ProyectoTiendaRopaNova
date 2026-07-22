import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';
import { useToast } from '../../context/ToastContext';

const emptyForm = { nombre: '', correo: '', contrasena: '', rol: 'CLIENTE' };

export default function Usuarios() {
    const [usuarios, setUsuarios] = useState([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState(emptyForm);
    const [editingId, setEditingId] = useState(null);
    const [saving, setSaving] = useState(false);
    const { toast } = useToast();

    useEffect(() => { load(); }, []);

    const load = () => {
        api.usuarios.getAll()
            .then(setUsuarios)
            .catch(() => toast('Error cargando usuarios', 'error'))
            .finally(() => setLoading(false));
    };

    const reset = () => { setForm(emptyForm); setEditingId(null); };
    const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

    const handleEdit = (u) => {
        setEditingId(u.id);
        setForm({ nombre: u.nombre, correo: u.correo, contrasena: '', rol: u.rol || 'CLIENTE' });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            // UsuarioCreateRequest: { nombre, correo, contrasena, rol }
            // UsuarioUpdateRequest: los campos son opcionales
            const payload = { ...form };
            if (editingId && !payload.contrasena) delete payload.contrasena;

            if (editingId) {
                await api.usuarios.update(editingId, payload);
                toast('Usuario actualizado', 'success');
            } else {
                await api.usuarios.create(payload);
                toast('Usuario creado', 'success');
            }
            reset();
            load();
        } catch (err) {
            toast(err.message || 'Error al guardar', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleToggle = async (u) => {
        try {
            if (u.active) {
                await api.usuarios.deactivate(u.id);
                toast(`"${u.nombre}" desactivado`, 'success');
            } else {
                await api.usuarios.update(u.id, { active: true });
                toast(`"${u.nombre}" activado`, 'success');
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
                    <h2>Usuarios</h2>
                    <p>Gestiona clientes y administradores</p>
                </div>
                <span className="badge badge-neutral">{usuarios.length} total</span>
            </div>

            <div className="admin-split">
                {/* Formulario */}
                <div className="form-panel">
                    <div className="form-panel-header">
                        {editingId ? 'Editar usuario' : '+ Nuevo usuario'}
                    </div>
                    <div className="form-panel-body">
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Nombre</label>
                                <input className="form-control" name="nombre" value={form.nombre} onChange={handleChange} required minLength={3} maxLength={100} />
                            </div>
                            <div className="form-group">
                                <label>Correo</label>
                                <input className="form-control" type="email" name="correo" value={form.correo} onChange={handleChange} required maxLength={100} />
                            </div>
                            <div className="form-group">
                                <label>Contraseña {editingId && <span style={{ fontWeight: 400 }}>(dejar vacío para no cambiar)</span>}</label>
                                <input className="form-control" type="password" name="contrasena" value={form.contrasena} onChange={handleChange} required={!editingId} minLength={4} />
                            </div>
                            <div className="form-group">
                                <label>Rol</label>
                                {/* El backend acepta: ADMIN | CLIENTE */}
                                <select className="form-control" name="rol" value={form.rol} onChange={handleChange} required>
                                    <option value="CLIENTE">Cliente</option>
                                    <option value="ADMIN">Administrador</option>
                                </select>
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

                {/* Tabla */}
                {loading ? (
                    <div className="loading-center"><div className="loading-ring" /></div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Usuario</th>
                                    <th>Correo</th>
                                    <th>Rol</th>
                                    <th>Estado</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {usuarios.map(u => (
                                    <tr key={u.id}>
                                        <td><strong>{u.nombre}</strong></td>
                                        <td className="text-sm text-muted">{u.correo}</td>
                                        <td>
                                            <span className={`badge ${u.rol === 'ADMIN' ? 'badge-info' : 'badge-neutral'}`}>
                                                {u.rol}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`badge ${u.active ? 'badge-success' : 'badge-danger'}`}>
                                                {u.active ? 'Activo' : 'Inactivo'}
                                            </span>
                                        </td>
                                        <td>
                                            <div className="action-bar">
                                                <button className="btn btn-sm btn-outline" onClick={() => handleEdit(u)}>Editar</button>
                                                <button
                                                    className={`btn btn-sm ${u.active ? 'btn-danger' : 'btn-success'}`}
                                                    onClick={() => handleToggle(u)}
                                                >
                                                    {u.active ? 'Desactivar' : 'Activar'}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {usuarios.length === 0 && (
                                    <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2rem' }}>Sin usuarios</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}
