import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ShoppingCart, Trash2, ArrowLeft, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

import api from '../api/axios';
import Button from '../components/ui/Button';
import { useCartStore } from '../store/cartStore';
import Spinner from '../components/ui/Spinner';

export default function CartPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const setCount = useCartStore((s) => s.setItemCount);
  const navigate = useNavigate();

  const totals = useMemo(() => {
    const total = items.reduce((acc, item) => acc + Number(item.price || 0), 0);
    return {
      total,
      subtotal: total,
      convenience: 0,
    };
  }, [items]);

  useEffect(() => {
    loadCart();
  }, []);

  const loadCart = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/api/cart');
      const payload = data?.data || { items: [] };
      setItems(payload.items || []);
      setCount(payload.totalItems || 0);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Could not load cart');
    } finally {
      setLoading(false);
    }
  };

  const removeItem = async (objectId) => {
    setBusy(true);
    try {
      const { data } = await api.delete(`/api/cart/item/${objectId}`);
      const payload = data?.data || { items: [] };
      setItems(payload.items || []);
      setCount(payload.totalItems || 0);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to remove item');
    } finally {
      setBusy(false);
    }
  };

  const clearCart = async () => {
    setBusy(true);
    try {
      await api.delete('/api/cart');
      setItems([]);
      setCount(0);
    } catch (err) {
      toast.error(err?.response?.data?.message || 'Failed to clear cart');
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Loading your cart" />
      </div>
    );
  }

  if (!items.length) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-white px-4">
        <div className="glass-card max-w-md w-full p-8 text-center space-y-4 border border-white/10">
          <ShoppingCart className="h-10 w-10 text-primary mx-auto" />
          <h1 className="text-2xl font-semibold">Your cart is empty</h1>
          <p className="text-gray-300 text-sm">Upload documents to get started.</p>
          <div className="flex gap-3 justify-center">
            <Button variant="ghost" onClick={() => navigate('/upload')}>Upload & Print</Button>
            <Button variant="ghost" onClick={() => navigate('/home')}>Back to Home</Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-white px-4 py-6">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-white">
              <ArrowLeft />
            </button>
            <div>
              <p className="text-sm text-gray-400">Cart</p>
              <h1 className="text-2xl font-semibold">Review & checkout</h1>
            </div>
          </div>
          <Button variant="ghost" onClick={clearCart} disabled={busy}>Clear cart</Button>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="space-y-4">
            {items.map((item, idx) => (
              <motion.div
                key={item.objectId}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: idx * 0.03 }}
                className="glass-card border border-white/10 p-4 flex flex-col gap-3"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-lg font-semibold">{item.filename}</p>
                    <p className="text-sm text-gray-400">Pages: {item.pageCount} · Copies: {item.copies}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-lg font-semibold">₹{Number(item.price).toFixed(2)}</p>
                    <button
                      className="text-danger text-sm inline-flex items-center gap-1 hover:text-danger/80"
                      onClick={() => removeItem(item.objectId)}
                      disabled={busy}
                    >
                      <Trash2 className="h-4 w-4" /> Remove
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs text-gray-300">
                  <Info tag="Color" value={item.colorMode} />
                  <Info tag="Sides" value={item.sides} />
                  <Info tag="Paper" value={item.paperSize} />
                  <Info tag="Binding" value={item.binding} />
                  {item.pageRange && <Info tag="Pages" value={item.pageRange} />}
                </div>
              </motion.div>
            ))}
          </div>

          <div className="glass-card border border-white/10 p-5 space-y-4 h-fit">
            <h2 className="text-xl font-semibold">Order Summary</h2>
            <div className="space-y-2 text-sm text-gray-200">
              <Row label="Subtotal" value={`₹${totals.subtotal.toFixed(2)}`} />
              <Row label="Convenience fee" value="₹0.00" />
            </div>
            <div className="border-t border-white/10 pt-3 flex items-center justify-between">
              <span className="text-gray-300">Total</span>
              <span className="text-2xl font-semibold">₹{totals.total.toFixed(2)}</span>
            </div>
            <Button onClick={() => navigate('/checkout')} disabled={busy}>
              {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Proceed to Checkout'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="text-gray-300">{label}</span>
      <span className="font-semibold">{value}</span>
    </div>
  );
}

function Info({ tag, value }) {
  return (
    <div className="rounded-md bg-white/5 border border-white/10 px-3 py-2">
      <p className="text-[11px] uppercase tracking-wide text-gray-400">{tag}</p>
      <p className="text-sm text-white">{value}</p>
    </div>
  );
}
