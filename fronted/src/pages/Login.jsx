import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';

export default function Login() {
    const [nombre,    setNombre]    = useState('');
    const [contrasena, setContrasena] = useState('');
    const [error,     setError]     = useState('');
    const [loading,   setLoading]   = useState(false);
    const { login } = useAuth();
    const navigate  = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            // api.auth.login envía { nombre, contrasena } al backend
            const user = await login(nombre, contrasena);
            // Backend devuelve rol: ADMIN | CLIENTE
            navigate(user.rol === 'ADMIN' ? '/admin/productos' : '/');
        } catch (err) {
            setError(err.message || 'Credenciales inválidas');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            {/* Panel izquierdo decorativo */}
            <div className="login-left">
                <p className="login-tagline">
                    Tu próxima<br />
                    prenda favorita<br />
                    te <em>espera</em>
                </p>
                <p style={{ color: 'rgba(245,240,232,0.45)', fontSize: '0.875rem', maxWidth: '300px', position: 'relative', zIndex: 1 }}>
                    Accede a tu cuenta para explorar el catálogo, gestionar tus pedidos y más.
                </p>
            </div>

            {/* Formulario */}
            <div className="login-right">
                <div className="login-box">
                    <h2>Bienvenido</h2>
                    <p>Ingresa tus credenciales para continuar</p>

                    {error && <div className="form-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label>Nombre de usuario</label>
                            <input
                                className="form-control"
                                type="text"
                                value={nombre}
                                onChange={e => setNombre(e.target.value)}
                                placeholder="Ej: admin"
                                required
                                autoFocus
                            />
                        </div>
                        <div className="form-group">
                            <label>Contraseña</label>
                            <input
                                className="form-control"
                                type="password"
                                value={contrasena}
                                onChange={e => setContrasena(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>
                        <button
                            type="submit"
                            className="btn btn-accent btn-full btn-lg"
                            disabled={loading}
                            style={{ marginTop: '0.5rem' }}
                        >
                            {loading ? 'Verificando...' : 'Ingresar'}
                        </button>
                    </form>

                    <div className="divider" />
                    <p style={{ textAlign: 'center', fontSize: '0.8rem', color: 'var(--muted)' }}>
                        ¿No tienes cuenta? <Link to="/register">Regístrate aquí</Link>
                    </p>
                </div>
            </div>
        </div>
    );
}
