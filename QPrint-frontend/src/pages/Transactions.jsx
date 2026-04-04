import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Receipt, ArrowLeftRight, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

import api from '../api/axios';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import Header from '../components/Header';
import { getErrorMessage } from '../utils/api';

const PAGE_SIZE = 10;

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [fetching, setFetching] = useState(false);
  const navigate = useNavigate();

  const load = async (pageNumber = 0) => {
    const targetPage = Math.max(pageNumber, 0);
    setFetching(true);
    try {
      const { data } = await api.get('/api/transactions', { params: { page: targetPage, size: PAGE_SIZE } });
      const body = data?.data;
      setTransactions(body?.content || []);
      setTotalPages(body?.totalPages || 0);
      setPage(body?.currentPage ?? targetPage);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not load transactions'));
    } finally {
      setLoading(false);
      setFetching(false);
    }
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Loading transactions" />
      </div>
    );
  }

  if (!transactions.length) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background text-white px-4">
        <div className="glass-card max-w-md w-full p-8 text-center space-y-4 border border-white/10">
          <Receipt className="h-10 w-10 text-primary mx-auto" />
          <h1 className="text-2xl font-semibold">No transactions yet</h1>
          <p className="text-gray-300 text-sm">Complete an order to see your history here.</p>
          <div className="flex gap-3 justify-center">
            <Button variant="ghost" onClick={() => navigate('/orders')}>View Orders</Button>
            <Button variant="ghost" onClick={() => navigate('/upload')}>Upload & Print</Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-white flex flex-col">
      <Header />
      <div className="flex-1 px-4 py-6">
        <div className="max-w-6xl mx-auto space-y-6">
          <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-gray-400">Transactions</p>
            <h1 className="text-2xl font-semibold">Payment history</h1>
          </div>
          {fetching && <Loader2 className="h-5 w-5 animate-spin text-primary" />}
        </div>

        <div className="grid gap-4">
          {transactions.map((tx, idx) => (
            <motion.div
              key={tx.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.02 }}
              className="glass-card border border-white/10 p-4 flex flex-col gap-3"
            >
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-lg font-semibold">₹{Number(tx.totalAmount || 0).toFixed(2)}</p>
                  <p className="text-sm text-gray-400">{formatDate(tx.completedAt)}</p>
                </div>
                <span className="text-xs font-semibold rounded-full px-3 py-1 border border-success/40 bg-success/10 text-success">COMPLETED</span>
              </div>
              <div className="text-sm text-gray-300 space-y-1">
                <p>Order ID: {tx.orderId}</p>
                <p>Payment ID: {tx.razorpayPaymentId || '—'}</p>
                <p>{getItemCount(tx.items)} item(s)</p>
              </div>
              <div className="flex justify-end">
                <Button size="sm" onClick={() => navigate(`/transactions/${tx.id}`)}>
                  View details
                </Button>
              </div>
            </motion.div>
          ))}
        </div>

        <Pagination
          page={page}
          totalPages={totalPages}
          onPrev={() => load(page - 1)}
          onNext={() => load(page + 1)}
          disablePrev={page <= 0 || fetching}
          disableNext={page >= totalPages - 1 || fetching}
        />
      </div>
    </div>
    </div>
  );
}

function Pagination({ page, totalPages, onPrev, onNext, disablePrev, disableNext }) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between text-sm text-gray-300">
      <Button variant="ghost" size="sm" onClick={onPrev} disabled={disablePrev}>
        <ArrowLeftRight className="h-4 w-4 mr-2" /> Prev
      </Button>
      <span>Page {page + 1} of {totalPages}</span>
      <Button variant="ghost" size="sm" onClick={onNext} disabled={disableNext}>
        Next <ArrowLeftRight className="h-4 w-4 ml-2 rotate-180" />
      </Button>
    </div>
  );
}

function getItemCount(items) {
  if (!items) return 0;
  if (Array.isArray(items)) return items.length;
  return 0;
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
