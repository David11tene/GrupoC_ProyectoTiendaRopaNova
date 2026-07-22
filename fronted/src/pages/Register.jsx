import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Register() {
    const [nombre, setNombre] = useState('');
    const [correo, setCorreo] = useState('');
    const [contrasena, setContrasena] = useState('');
    const [confirm, setConfirm] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { register, login } = useAuth();
    const navigate = useNavigate();

    const passwordsMatch = confirm.length > 0 && contrasena === confirm;
    const passwordIsValid = contrasena.length >= 4;

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError('');

        if (nombre.trim().length < 3) {
            setError('El nombre de usuario debe tener al menos 3 caracteres.');
            return;
        }
        if (!correo.trim() || !passwordIsValid) {
            setError('Completa los campos y usa una contraseña de al menos 4 caracteres.');
            return;
        }
        if (contrasena !== confirm) {
            setError('Las contraseñas no coinciden.');
            return;
        }

        setLoading(true);
        try {
            const cleanName = nombre.trim();
            await register({ nombre: cleanName, correo: correo.trim(), contrasena, rol: 'CLIENTE' });
            const user = await login(cleanName, contrasena);
            navigate(user.rol === 'ADMIN' ? '/admin/productos' : '/');
        } catch (err) {
            setError(err.message || 'No pudimos crear tu cuenta. Inténtalo nuevamente.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="register-page">
            <aside className="register-aside">
                <div className="register-aside-content">
                    <span className="register-kicker">NOVA CLUB</span>
                    <h1>Tu estilo,<br />ahora más <em>cerca.</em></h1>
                    <p>Crea tu cuenta y convierte cada visita en una experiencia hecha a tu medida.</p>

                    <div className="register-benefits">
                        <div><span>01</span><p>Compra y revisa tus pedidos fácilmente.</p></div>
                        <div><span>02</span><p>Descubre prendas para cada momento.</p></div>
                        <div><span>03</span><p>Recibe actualizaciones de tus compras.</p></div>
                    </div>
                </div>
                <small>Moda que se siente propia.</small>
            </aside>

            <main className="register-main">
                <section className="register-card">
                    <header className="register-header">
                        <span>EMPIEZA AQUÍ</span>
                        <h2>Crear una cuenta</h2>
                        <p>Solo te tomará un minuto.</p>
                    </header>

                    {error && <div className="form-error" role="alert">{error}</div>}

                    <form onSubmit={handleSubmit} noValidate>
                        <div className="register-form-row">
                            <div className="form-group">
                                <label htmlFor="register-name">Nombre de usuario</label>
                                <input id="register-name" className="form-control" value={nombre}
                                       onChange={event => setNombre(event.target.value)} placeholder="Ej: david"
                                       autoComplete="username" minLength={3} required autoFocus />
                            </div>
                            <div className="form-group">
                                <label htmlFor="register-email">Correo electrónico</label>
                                <input id="register-email" className="form-control" type="email" value={correo}
                                       onChange={event => setCorreo(event.target.value)} placeholder="tu@correo.com"
                                       autoComplete="email" required />
                            </div>
                        </div>

                        <div className="register-form-row">
                            <div className="form-group">
                                <label htmlFor="register-password">Contraseña</label>
                                <div className="password-field">
                                    <input id="register-password" className="form-control"
                                           type={showPassword ? 'text' : 'password'} value={contrasena}
                                           onChange={event => setContrasena(event.target.value)}
                                           placeholder="Mínimo 4 caracteres" autoComplete="new-password"
                                           minLength={4} required />
                                    <button type="button" className="password-toggle"
                                            onClick={() => setShowPassword(value => !value)}
                                            aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}>
                                        {showPassword ? 'Ocultar' : 'Ver'}
                                    </button>
                                </div>
                            </div>
                            <div className="form-group">
                                <label htmlFor="register-confirm">Confirmar contraseña</label>
                                <input id="register-confirm" className="form-control"
                                       type={showPassword ? 'text' : 'password'} value={confirm}
                                       onChange={event => setConfirm(event.target.value)}
                                       placeholder="Repite tu contraseña" autoComplete="new-password" required />
                            </div>
                        </div>

                        <div className="register-validation" aria-live="polite">
                            <span className={passwordIsValid ? 'is-valid' : ''}><i /> 4 caracteres como mínimo</span>
                            <span className={passwordsMatch ? 'is-valid' : ''}><i /> Las contraseñas coinciden</span>
                        </div>

                        <button className="btn btn-accent btn-full btn-lg register-submit"
                                type="submit" disabled={loading}>
                            {loading ? 'Creando tu cuenta...' : 'Crear mi cuenta'}
                        </button>
                    </form>

                    <p className="register-login-link">
                        ¿Ya tienes una cuenta? <Link to="/login">Inicia sesión</Link>
                    </p>
                </section>
            </main>
        </div>
    );
}
