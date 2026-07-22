const BASE = '/api';

async function request(path, options = {}) {
    const res = await fetch(`${BASE}${path}`, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
        },
    });

    if (res.status === 204) return null;

    const contentType = res.headers.get('content-type') || '';
    const body = await res.text();
    const data = !body
        ? null
        : contentType.includes('application/json')
            ? JSON.parse(body)
            : body;

    if (!res.ok) {
        // El GlobalExceptionHandler del backend devuelve { message, status, error }
        const msg = data?.message || data?.error || `Error ${res.status}`;
        throw new Error(msg);
    }

    return data;
}

const get = (path) => request(path);
const post = (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) });
const put = (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) });
const patch = (path, body) => request(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined });
const del = (path) => request(path, { method: 'DELETE' });

export const api = {
    // ── Auth ──────────────────────────────────────────────────────────
    auth: {
        login: (nombre, contrasena) => post('/auth/login', { nombre, contrasena }),
    },

    // ── Categorías ────────────────────────────────────────────────────
    categorias: {
        getAll: () => get('/categorias'),
        getById: (id) => get(`/categorias/${id}`),
        create: (data) => post('/categorias', data),      // { nombre }
        update: (id, data) => put(`/categorias/${id}`, data),
        delete: (id) => del(`/categorias/${id}`),
    },

    // ── Productos ─────────────────────────────────────────────────────
    productos: {
        getAll: () => get('/productos'),
        getById: (id) => get(`/productos/${id}`),
        search: (name, page = 0, size = 12) =>
            get(`/productos/search?name=${encodeURIComponent(name)}&page=${page}&size=${size}`),
        create: (data) => post('/productos', data),
        update: (id, data) => put(`/productos/${id}`, data),
        deactivate: (id) => patch(`/productos/${id}/deactivate`),
    },

    // ── Usuarios ──────────────────────────────────────────────────────
    usuarios: {
        getAll: () => get('/usuarios'),
        getById: (id) => get(`/usuarios/${id}`),
        search: (name, page = 0, size = 20) =>
            get(`/usuarios/search?name=${encodeURIComponent(name)}&page=${page}&size=${size}`),
        create: (data) => post('/usuarios', data),
        update: (id, data) => put(`/usuarios/${id}`, data),
        deactivate: (id) => patch(`/usuarios/${id}/deactivate`),
    },

    // ── Pedidos ───────────────────────────────────────────────────────
    pedidos: {
        getAll: () => get('/pedidos'),
        getById: (id) => get(`/pedidos/${id}`),
        create: (data) => post('/pedidos', data),
        updateEstado: (id, estado) => patch(`/pedidos/${id}/estado`, { estado }),
        getPromedio: () => get('/reactivo/pedidos/promedio'),
        procesarLotes: () => post('/reactivo/pedidos/procesar-lotes', {}),
    },
};

// ── Helpers de formato ────────────────────────────────────────────────────
export const fmt = {
    price: (n) => `$${parseFloat(n || 0).toFixed(2)}`,
    date: (d) => d ? new Date(d).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: 'numeric' }) : '—',
    estado: (e) => ({
        PENDIENTE: { label: 'Pendiente', cls: 'badge-warning' },
        APROBADO: { label: 'Aprobado', cls: 'badge-success' },
        RECHAZADO: { label: 'Rechazado', cls: 'badge-danger' },
        ENVIADO: { label: 'Enviado', cls: 'badge-info' },
        ENTREGADO: { label: 'Entregado', cls: 'badge-neutral' },
    }[e] || { label: e, cls: 'badge-neutral' }),
};
