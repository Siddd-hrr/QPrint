import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, ShieldAlert } from 'lucide-react';
import toast from 'react-hot-toast';

import api from '../api/axios';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import { getErrorMessage } from '../utils/api';

export default function TransactionDetailPage() {
  const { id } = useParams();
  const [tx, setTx] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const items = useMemo(() => {
    if (!tx?.items) return [];
    if (Array.isArray(tx.items)) return tx.items;
    return [];
  }, [tx]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const { data } = await api.get(`/api/transactions/${id}`);
        setTx(data?.data);
      } catch (err) {
        const msg = getErrorMessage(err, 'Could not load transaction');
        setError(msg);
        toast.error(msg);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [id]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Loading transaction" />
      </div>
    );
  }

  if (!tx) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-white px-4">
        <div className="glass-card max-w-md w-full p-6 text-center border border-white/10 space-y-3">
          <ShieldAlert className="h-10 w-10 text-danger mx-auto" />
          <p className="font-semibold">Transaction not found</p>
          <Button variant="ghost" onClick={() => navigate('/transactions')}>Back to transactions</Button>
        </div>
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
            <p className="text-sm text-gray-400">Transaction</p>
            <h1 className="text-2xl font-semibold">Payment detail</h1>
          </div>
        </div>

        {error && (
          <div className="glass-card border border-danger/40 bg-danger/10 p-3 text-sm text-gray-100 flex items-start gap-2">
            <ShieldAlert className="h-5 w-5 text-danger" />
            <p>{error}</p>
          </div>
        )}

        <div className="glass-card border border-white/10 p-5 space-y-4">
          <div className="flex items-center justify-between rounded-lg border px-3 py-2 border-success/40 bg-success/10 text-success">
            <span className="font-semibold text-sm">COMPLETED</span>
            <span className="text-sm text-gray-200">{formatDate(tx.completedAt)}</span>
          </div>

          <div className="grid gap-3 text-sm text-gray-200 sm:grid-cols-2">
            <Row label="Transaction ID" value={tx.id} />
            <Row label="Order ID" value={tx.orderId} />
            <Row label="Shop" value={tx.shopId || '—'} />
            <Row label="Payment ID" value={tx.razorpayPaymentId || '—'} />
            <Row label="Amount" value={`₹${Number(tx.totalAmount || 0).toFixed(2)}`} />
            <Row label="Completed at" value={formatDate(tx.completedAt)} />
          </div>

          <div className="space-y-3">
            <p className="text-sm text-gray-300">Items</p>
            {items.length ? (
              <div className="grid gap-3">
                {items.map((item, idx) => (
                  <motion.div
                    key={item.objectId || idx}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: idx * 0.02 }}
                    className="rounded-lg border border-white/10 bg-white/5 p-3"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-semibold">{item.filename || item.name || 'Item'}</p>
                        <p className="text-xs text-gray-400">Pages: {item.pageCount || '—'} · Copies: {item.copies || '—'}</p>
                      </div>
                      <span className="font-semibold">₹{Number(item.price || 0).toFixed(2)}</span>
                    </div>
                    <div className="text-[11px] text-gray-300 mt-1 space-x-2">
                      {item.colorMode && <span>{item.colorMode}</span>}
                      {item.sides && <span>• {item.sides}</span>}
                      {item.paperSize && <span>• {item.paperSize}</span>}
                      {item.binding && <span>• {item.binding}</span>}
                      {item.pageRange && <span className="ml-2">Pages {item.pageRange}</span>}
                    </div>
                  </motion.div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-300">No item data available.</p>
            )}
          </div>

          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => navigate('/transactions')}>Back to transactions</Button>
            <Button variant="ghost" onClick={() => navigate('/orders')}>View orders</Button>
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

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
