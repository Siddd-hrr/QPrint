import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertCircle, ArrowLeft, CheckCircle2, Loader2, RefreshCw, ShieldAlert } from 'lucide-react';

import api from '../api/axios';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import { useCartStore } from '../store/cartStore';
import { getErrorMessage } from '../utils/api';

const MAX_POLL = 12;
const POLL_INTERVAL = 2000;

export default function CheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const checkoutId = searchParams.get('checkoutId');
  const paymentId = searchParams.get('paymentId');
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [polling, setPolling] = useState(true);
  const navigate = useNavigate();
  const attemptsRef = useRef(0);
  const timerRef = useRef(null);
  const clearedRef = useRef(false);
  const setCount = useCartStore((s) => s.setItemCount);

  const statusMeta = useMemo(() => {
    const status = summary?.status || 'PENDING';
    if (status === 'PAID') {
      return { label: 'Payment successful', tone: 'success' };
    }
    if (status === 'FAILED') {
      return { label: 'Payment failed', tone: 'danger' };
    }
    return { label: 'Waiting for confirmation', tone: 'warning' };
  }, [summary]);

  const clearCartOnce = useCallback(async () => {
    if (clearedRef.current) return;
    clearedRef.current = true;
    try {
      await api.delete('/api/cart');
      setCount(0);
    } catch {
      // Swallow errors; cart clearing is best-effort post-payment.
    }
  }, [setCount]);

  const fetchStatus = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get(`/api/checkout/${checkoutId}`);
      const payload = data?.data;
      setSummary(payload);

      if (payload?.status === 'PAID') {
        await clearCartOnce();
        setPolling(false);
        return;
      }

      if (payload?.status === 'FAILED') {
        setPolling(false);
        return;
      }

      if (attemptsRef.current >= MAX_POLL) {
        setPolling(false);
        return;
      }

      attemptsRef.current += 1;
      timerRef.current = setTimeout(fetchStatus, POLL_INTERVAL);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not fetch checkout status'));
      setPolling(false);
    } finally {
      setLoading(false);
    }
  }, [checkoutId, clearCartOnce]);

  useEffect(() => {
    if (!checkoutId) {
      setError('Missing checkout reference.');
      setLoading(false);
      return;
    }

    fetchStatus();

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [checkoutId, fetchStatus]);

  const refreshNow = () => {
    if (!checkoutId) return;
    attemptsRef.current = 0;
    setPolling(true);
    setError('');
    if (timerRef.current) clearTimeout(timerRef.current);
    fetchStatus();
  };

  if (!checkoutId) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-white px-4">
        <div className="glass-card max-w-md w-full p-6 text-center border border-white/10">
          <AlertCircle className="h-10 w-10 text-warning mx-auto" />
          <h1 className="text-lg font-semibold">No checkout found</h1>
          <p className="text-sm text-gray-300">Return to your cart and try again.</p>
          <div className="flex gap-2 justify-center mt-3">
            <Button variant="ghost" onClick={() => navigate('/cart')}>Back to Cart</Button>
            <Button variant="ghost" onClick={() => navigate('/upload')}>Upload</Button>
          </div>
        </div>
      </div>
    );
  }

  if (loading && !summary) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Checking payment status" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-white px-4 py-6">
      <div className="max-w-5xl mx-auto space-y-6">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-white">
            <ArrowLeft />
          </button>
          <div>
            <p className="text-sm text-gray-400">Checkout</p>
            <h1 className="text-2xl font-semibold">Payment status</h1>
          </div>
        </div>

        {error && (
          <div className="glass-card border border-danger/40 bg-danger/10 p-4 text-sm text-gray-100 flex items-start gap-3">
            <ShieldAlert className="h-5 w-5 text-danger" />
            <div>
              <p className="font-semibold mb-1">We could not verify the payment.</p>
              <p className="text-gray-200">{error}</p>
            </div>
          </div>
        )}

        <div className="glass-card border border-white/10 p-5 space-y-4">
          <StatusBadge meta={statusMeta} />
          <div className="grid gap-3 text-sm text-gray-200 sm:grid-cols-2">
            <Row label="Checkout ID" value={checkoutId} />
            <Row label="Order ID" value={summary?.razorpayOrderId || '—'} />
            <Row label="Payment ID" value={paymentId || summary?.razorpayPaymentId || '—'} />
            <Row label="Amount" value={`₹${Number(summary?.amount || 0).toFixed(2)}`} />
            <Row label="Currency" value={summary?.currency || 'INR'} />
          </div>

          {summary?.items?.length ? (
            <div className="space-y-3">
              <p className="text-sm text-gray-300">Items</p>
              <div className="grid gap-3">
                {summary.items.map((item, idx) => (
                  <motion.div
                    key={item.objectId}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.02 }}
                    className="rounded-lg border border-white/10 bg-white/5 p-3"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-semibold">{item.filename}</p>
                        <p className="text-xs text-gray-400">Pages: {item.pageCount} · Copies: {item.copies}</p>
                      </div>
                      <span className="font-semibold">₹{Number(item.price || 0).toFixed(2)}</span>
                    </div>
                    <div className="text-[11px] text-gray-300 mt-1 space-x-2">
                      <span>{item.colorMode}</span>
                      <span>•</span>
                      <span>{item.sides}</span>
                      <span>•</span>
                      <span>{item.paperSize}</span>
                      <span>•</span>
                      <span>{item.binding}</span>
                      {item.pageRange && <span className="ml-2">Pages {item.pageRange}</span>}
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-sm text-gray-300">No items found for this checkout.</p>
          )}

          <div className="flex flex-wrap gap-2 pt-2">
            <Button variant="secondary" onClick={() => navigate('/orders')}>View Orders</Button>
            <Button variant="ghost" onClick={() => navigate('/upload')}>Upload another</Button>
            <Button variant="ghost" onClick={() => navigate('/cart')}>Back to Cart</Button>
            <Button variant="ghost" onClick={refreshNow} disabled={polling}>
              {polling ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
              {polling ? 'Waiting...' : 'Refresh status'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between rounded-md border border-white/5 bg-white/5 px-3 py-2">
      <span className="text-gray-300 text-sm">{label}</span>
      <span className="text-sm font-semibold break-all">{value}</span>
    </div>
  );
}

function StatusBadge({ meta }) {
  const tones = {
    success: 'bg-success/10 border-success/30 text-success',
    danger: 'bg-danger/10 border-danger/30 text-danger',
    warning: 'bg-warning/10 border-warning/30 text-warning-foreground',
  };
  const tone = tones[meta.tone] || tones.warning;
  const Icon = meta.tone === 'success' ? CheckCircle2 : meta.tone === 'danger' ? ShieldAlert : AlertCircle;

  return (
    <div className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm ${tone}`}>
      <Icon className="h-5 w-5" />
      <span className="font-semibold">{meta.label}</span>
      {meta.tone === 'warning' && <span className="text-xs text-gray-200">Awaiting confirmation from Razorpay</span>}
    </div>
  );
}
