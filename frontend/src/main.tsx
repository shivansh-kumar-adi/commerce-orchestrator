import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';

type Product = {id: string; name: string; description: string; price: number};
type CartLine = Product & {quantity: number};
type Tokens = {accessToken: string; refreshToken: string};
type Order = {id: string; status: string; total: number};

const API = import.meta.env.VITE_API_URL || '/api';

function subject(token: string): string | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(payload)).sub ?? null;
  } catch { return null; }
}

function App() {
  const [token, setToken] = useState(localStorage.getItem('accessToken') || '');
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [email, setEmail] = useState('customer@example.com');
  const [password, setPassword] = useState('password123');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [order, setOrder] = useState<Order | null>(null);

  useEffect(() => {
    fetch(API + '/products')
      .then(async response => {
        if (!response.ok) throw new Error(`Products failed (${response.status})`);
        return response.json();
      })
      .then(page => setProducts(page.content || []))
      .catch(error => setMessage(error instanceof Error ? error.message : 'Unable to load products'))
      .finally(() => setLoading(false));
  }, []);

  function saveTokens(tokens: Tokens) {
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    setToken(tokens.accessToken);
  }

  async function refreshAccessToken(): Promise<string | null> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return null;
    const response = await fetch(API + '/auth/refresh', {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({refreshToken})
    });
    if (!response.ok) {
      localStorage.removeItem('accessToken'); localStorage.removeItem('refreshToken'); setToken('');
      return null;
    }
    const tokens = await response.json() as Tokens;
    saveTokens(tokens);
    return tokens.accessToken;
  }

  async function authenticatedFetch(path: string, init: RequestInit): Promise<Response> {
    let accessToken = token;
    let response = await fetch(API + path, {...init, headers: {...init.headers, Authorization: `Bearer ${accessToken}`}});
    if (response.status === 401) {
      const refreshed = await refreshAccessToken();
      if (refreshed !== null) {
        accessToken = refreshed;
        response = await fetch(API + path, {...init, headers: {...init.headers, Authorization: `Bearer ${accessToken}`}});
      }
    }
    return response;
  }

  async function login() {
    setBusy(true); setMessage('');
    try {
      const response = await fetch(API + '/auth/login', {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({email, password})
      });
      if (!response.ok) throw new Error(response.status === 401 ? 'Invalid email or password' : `Login failed (${response.status})`);
      saveTokens(await response.json()); setMessage('Signed in');
    } catch (error) { setMessage(error instanceof Error ? error.message : 'Login failed'); }
    finally { setBusy(false); }
  }

  function add(product: Product) {
    setCart(current => {
      const existing = current.find(item => item.id === product.id);
      return existing
        ? current.map(item => item.id === product.id ? {...item, quantity: item.quantity + 1} : item)
        : [...current, {...product, quantity: 1}];
    });
  }

  async function checkout() {
    const userId = subject(token);
    if (!userId) { setMessage('Sign in again before checkout'); return; }
    setBusy(true); setMessage('');
    try {
      const response = await authenticatedFetch('/orders', {
        method: 'POST', headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({userId, items: cart.map(item => ({productId: item.id, quantity: item.quantity, unitPrice: item.price}))})
      });
      if (!response.ok) throw new Error(`Checkout failed (${response.status})`);
      const created = await response.json() as Order;
      setOrder(created); setCart([]); setMessage(`Order accepted: ${created.status}`);
    } catch (error) { setMessage(error instanceof Error ? error.message : 'Checkout failed'); }
    finally { setBusy(false); }
  }

  return <>
    <header><b>Commerce Orchestrator</b><span>Distributed order processing through coordinated services</span></header>
    <main>
      {!token && <section className="login"><h2>Sign in</h2>
        <input aria-label="Email" value={email} onChange={event => setEmail(event.target.value)}/>
        <input aria-label="Password" type="password" value={password} onChange={event => setPassword(event.target.value)}/>
        <button disabled={busy} onClick={login}>{busy ? 'Signing in…' : 'Continue'}</button>
      </section>}
      <section><div className="title"><h1>Products</h1><span>{cart.reduce((count, item) => count + item.quantity, 0)} items</span></div>
        {loading ? <p>Loading products…</p> : products.length === 0 ? <p>No active products are available.</p> :
          <div className="grid">{products.map(product => <article key={product.id}>
            <div className="image">{product.name.slice(0, 1)}</div><h3>{product.name}</h3><p>{product.description}</p>
            <strong>${Number(product.price).toFixed(2)}</strong><button onClick={() => add(product)}>Add to cart</button>
          </article>)}</div>}
      </section>
      <aside><h2>Your cart</h2>{cart.length === 0 ? <p>Your cart is empty.</p> : cart.map(item => <p key={item.id}>{item.name} × {item.quantity}</p>)}
        <strong>Total ${cart.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)}</strong>
        <button disabled={busy || !token || !cart.length} onClick={checkout}>{busy ? 'Working…' : 'Checkout'}</button>
        {order && <small>Latest order {order.id}: {order.status}</small>}
        <small>Payment is simulated; no card information is collected.</small>
      </aside>
      {message && <div className="toast" role="status">{message}</div>}
    </main><footer>Commerce Orchestrator • Distributed systems engineering project</footer>
  </>;
}

createRoot(document.getElementById('root')!).render(<App/>);
