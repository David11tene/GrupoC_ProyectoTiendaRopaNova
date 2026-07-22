import React, { createContext, useContext, useState } from 'react';

const CartContext = createContext(null);
export const useCart = () => useContext(CartContext);

export function CartProvider({ children }) {
    const [cart, setCart] = useState([]);

    const addToCart = (product) => {
        setCart(prev => {
            const existing = prev.find(i => i.id === product.id);
            if (existing) {
                return prev.map(i => i.id === product.id
                    ? { ...i, quantity: i.quantity + 1 }
                    : i
                );
            }
            return [...prev, { ...product, quantity: 1 }];
        });
    };

    const removeFromCart = (id) => setCart(prev => prev.filter(i => i.id !== id));

    const updateQuantity = (id, quantity) => {
        if (quantity < 1) return removeFromCart(id);
        setCart(prev => prev.map(i => i.id === id ? { ...i, quantity } : i));
    };

    const clearCart = () => setCart([]);

    const totalItems = cart.reduce((a, i) => a + i.quantity, 0);
    const totalPrice = cart.reduce((a, i) => a + parseFloat(i.precio) * i.quantity, 0);

    return (
        <CartContext.Provider value={{ cart, addToCart, removeFromCart, updateQuantity, clearCart, totalItems, totalPrice }}>
            {children}
        </CartContext.Provider>
    );
}
