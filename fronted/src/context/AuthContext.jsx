import React, { createContext, useContext, useState, useCallback } from 'react';
import { api } from '../services/api';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        try {
            const stored = localStorage.getItem('nova_user');
            return stored ? JSON.parse(stored) : null;
        } catch {
            return null;
        }
    });

    const login = useCallback(async (nombre, contrasena) => {
        // POST /api/auth/login — backend espera { nombre, contrasena }
        const userData = await api.auth.login(nombre, contrasena);
        setUser(userData);
        localStorage.setItem('nova_user', JSON.stringify(userData));
        return userData;
    }, []);

    const register = useCallback(async (data) => {
        // data: { nombre, correo, contrasena, rol }
        await api.usuarios.create(data);
        return true;
    }, []);

    const logout = useCallback(() => {
        setUser(null);
        localStorage.removeItem('nova_user');
    }, []);

    // Backend usa roles: ADMIN | CLIENTE
    const isAdmin   = user?.rol === 'ADMIN';
    const isCliente = user?.rol === 'CLIENTE';
    const isLoggedIn = !!user;

    return (
        <AuthContext.Provider value={{ user, login, logout, register, isAdmin, isCliente, isLoggedIn }}>
            {children}
        </AuthContext.Provider>
    );
}
