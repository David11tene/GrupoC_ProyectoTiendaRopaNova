import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './context/ToastContext';

import Navbar from './components/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Cart from './pages/Cart';
import MisPedidos from './pages/MisPedidos';

import AdminLayout from './pages/admin/AdminLayout';
import Categorias from './pages/admin/Categorias';
import Productos from './pages/admin/Productos';
import Usuarios from './pages/admin/Usuarios';
import Pedidos from './pages/admin/Pedidos';

function ProtectedRoute({ children }) {
    const { isLoggedIn } = useAuth();
    return isLoggedIn ? children : <Navigate to="/login" replace />;
}

function ClienteRoute({ children }) {
    const { isLoggedIn, isAdmin } = useAuth();
    if (!isLoggedIn) return <Navigate to="/login" replace />;
    if (isAdmin) return <Navigate to="/admin/productos" replace />;
    return children;
}


function AdminRoute({ children }) {
    const { isLoggedIn, isAdmin } = useAuth();
    if (!isLoggedIn) return <Navigate to="/login" replace />;
    if (!isAdmin) return <Navigate to="/" replace />;
    return children;
}

function AppRoutes() {
    return (
        <>
            <Navbar />
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                <Route path="/cart" element={<ClienteRoute><Cart /></ClienteRoute>} />
                <Route path="/mis-pedidos" element={<ClienteRoute><MisPedidos /></ClienteRoute>} />

                <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
                    <Route index element={<Navigate to="productos" replace />} />
                    <Route path="categorias" element={<Categorias />} />
                    <Route path="productos" element={<Productos />} />
                    <Route path="usuarios" element={<Usuarios />} />
                    <Route path="pedidos" element={<Pedidos />} />
                </Route>

                {/*Fallback*/}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <CartProvider>
                <ToastProvider>
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </ToastProvider>
            </CartProvider>
        </AuthProvider>
    );
}
