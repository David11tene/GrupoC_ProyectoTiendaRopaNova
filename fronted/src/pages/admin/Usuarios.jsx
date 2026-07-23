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

    // Estados de sincronización en tiempo real
    const [realtimeStatus, setRealtimeStatus] = useState('conectando');
    const [conteoTotal, setConteoTotal] = useState(null);

    const { toast } = useToast();

    useEffect(() => {
        load();

        // Sincronización en tiempo real de nuevos usuarios registrados
        const userSource = new EventSource('/api/reactivo/usuarios/stream');
        userSource.onopen = () => setRealtimeStatus('conectado');
        userSource.onmessage = (event) => {
            const usuarioNuevo = JSON.parse(event.data);
            setUsuarios(prev => {
                const existe = prev.some(u => u.id === usuarioNuevo.id);
                return existe
                    ? prev.map(u => (u.id === usuarioNuevo.id ? usuarioNuevo : u))
                    : [usuarioNuevo, ...prev];
            });
            toast(`🔔 Nuevo cliente registrado: ${usuarioNuevo.nombre} (${usuarioNuevo.correo})`, 'info');
        };
        userSource.onerror = () => setRealtimeStatus('reconectando');

        // Contador total de clientes en vivo
        const conteoSource = new EventSource('/api/reactivo/usuarios/conteo-stream');
        conteoSource.onmessage = (event) => setConteoTotal(Number(event.data));

        return () => {
            userSource.close();
            conteoSource.close();
        };
    }, []);

    const load = () => {
        api.usuarios.getAll()
            .then(data => {
                setUsuarios(Array.isArray(data) ? data : []);
            })
            .catch(() => toast('Error al cargar la lista de usuarios', 'error'))
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
            const payload = { ...form };
            if (editingId && !payload.contrasena) delete payload.contrasena;

            if (editingId) {
                await api.usuarios.update(editingId, payload);
                toast('Usuario actualizado exitosamente', 'success');
            } else {
                await api.usuarios.create(payload);
                toast('Usuario registrado exitosamente', 'success');
            }
            reset();
            load();
        } catch (err) {
            toast(err.message || 'Error al guardar usuario', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleToggle = async (u) => {
        try {
            if (u.active) {
                await api.usuarios.deactivate(u.id);
                toast(`Cuenta de "${u.nombre}" desactivada`, 'success');
            } else {
                await api.usuarios.update(u.id, { active: true });
                toast(`Cuenta de "${u.nombre}" activada`, 'success');
            }
            load();
        } catch (err) {
            toast(err.message || 'Error al cambiar estado de la cuenta', 'error');
        }
    };

    return (
        <>
            {/* Encabezado Comercial */}
            <div className="admin-page-header">
                <div>
                    <h2>Gestión de Usuarios & Clientes</h2>
                    <p>Administra los accesos de clientes y personal con actualización en tiempo real</p>
                </div>
                <span className="badge badge-neutral">
                    {conteoTotal !== null ? conteoTotal : usuarios.length} registrados
                </span>
            </div>

            {/* Barra de Sincronización en Vivo */}
            <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
                <span className={`badge ${realtimeStatus === 'conectado' ? 'badge-success' : 'badge-neutral'}`} style={{ fontSize: '0.85rem', padding: '0.45rem 0.85rem', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                    🟢 Monitoreo de Registros: {realtimeStatus === 'conectado' ? 'En Tiempo Real' : 'Reconectando...'}
                </span>

                <span className="badge badge-info" style={{ fontSize: '0.85rem', padding: '0.45rem 0.85rem' }}>
                    👥 Clientes Registrados: <strong>{conteoTotal === null ? usuarios.length : conteoTotal}</strong>
                </span>
            </div>

            <div className="admin-split">
                {/* Formulario de Alta y Edición */}
                <div className="form-panel">
                    <div className="form-panel-header">
                        {editingId ? 'Editar usuario' : '+ Nuevo usuario'}
                    </div>
                    <div className="form-panel-body">
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Nombre completo</label>
                                <input className="form-control" name="nombre" value={form.nombre} onChange={handleChange} required minLength={3} maxLength={100} placeholder="Ej. Ana María López" />
                            </div>
                            <div className="form-group">
                                <label>Correo electrónico</label>
                                <input className="form-control" type="email" name="correo" value={form.correo} onChange={handleChange} required maxLength={100} placeholder="correo@ejemplo.com" />
                            </div>
                            <div className="form-group">
                                <label>Contraseña {editingId && <span style={{ fontWeight: 400 }}>(dejar en blanco para conservar)</span>}</label>
                                <input className="form-control" type="password" name="contrasena" value={form.contrasena} onChange={handleChange} required={!editingId} minLength={4} placeholder="••••••••" />
                            </div>
                            <div className="form-group">
                                <label>Tipo de Cuenta</label>
                                <select className="form-control" name="rol" value={form.rol} onChange={handleChange} required>
                                    <option value="CLIENTE">Cliente</option>
                                    <option value="ADMIN">Administrador</option>
                                </select>
                            </div>
                            <div className="action-bar">
                                <button type="submit" className="btn btn-accent" disabled={saving}>
                                    {saving ? 'Guardando...' : 'Guardar Usuario'}
                                </button>
                                {editingId && <button type="button" className="btn btn-outline" onClick={reset}>Cancelar</button>}
                            </div>
                        </form>
                    </div>
                </div>

                {/* Tabla de Usuarios */}
                {loading ? (
                    <div className="loading-center"><div className="loading-ring" /></div>
                ) : (
                    <div className="table-wrap">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Usuario</th>
                                    <th>Correo Electrónico</th>
                                    <th>Tipo de Cuenta</th>
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
                                                {u.rol === 'ADMIN' ? 'Administrador' : 'Cliente'}
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
                                    <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--muted)', padding: '2rem' }}>No existen usuarios registrados</td></tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </>
    );
}
