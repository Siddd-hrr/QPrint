import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Clock,
  FileText,
  LogOut,
  ShoppingCart,
  Sparkles,
  User,
  Receipt,
} from 'lucide-react';
import toast from 'react-hot-toast';

import api from '../api/axios';
import Button from '../components/ui/Button';
import Spinner from '../components/ui/Spinner';
import Header from '../components/Header';
import { useAuthStore } from '../store/authStore';
import { useCartStore } from '../store/cartStore';
import { useShopStore } from '../store/shopStore';
import { getErrorMessage } from '../utils/api';

const quickActions = [
  {
    title: 'Upload & Print',
    subtitle: 'Start a new print job',
    icon: FileText,
    to: '/upload',
    tone: 'from-blue-500/20 via-blue-500/5 to-transparent',
  },
  {
    title: 'My Cart',
    subtitle: 'Review items before checkout',
    icon: ShoppingCart,
    to: '/cart',
    tone: 'from-amber-500/20 via-amber-500/5 to-transparent',
  },
  {
    title: 'Order Status',
    subtitle: 'Track live progress',
    icon: Clock,
    to: '/orders',
    tone: 'from-emerald-500/20 via-emerald-500/5 to-transparent',
  },
  {
    title: 'Transactions',
    subtitle: 'Payments & invoices',
    icon: Receipt,
    to: '/transactions',
    tone: 'from-purple-500/20 via-purple-500/5 to-transparent',
  },
];

export default function HomePage() {
  const navigate = useNavigate();
  const { user, clearAuth } = useAuthStore();
  const { itemCount, setItemCount } = useCartStore();
  const { selectedShop, setShop } = useShopStore();

  const [loading, setLoading] = useState(true);
  const [activeCount, setActiveCount] = useState(0);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [showMenu, setShowMenu] = useState(false);

  const greeting = useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }, []);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const [cart, orders, transactions, shops] = await Promise.all([
          api.get('/api/cart/count'),
          api.get('/api/orders/active'),
          api.get('/api/transactions', { params: { page: 0, size: 3 } }),
          api.get('/api/shops/nearby'),
        ]);

        setItemCount(cart?.data?.data ?? 0);
        setActiveCount((orders?.data?.data || []).length);
        setRecentTransactions(transactions?.data?.data?.content || []);
        const list = shops?.data?.data || [];
        if (list.length) setShop(list[0]);
      } catch (err) {
        toast.error(getErrorMessage(err, 'Could not load dashboard'));
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [setItemCount, setShop]);

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // ignore
    } finally {
      clearAuth();
      navigate('/login');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Spinner label="Loading dashboard" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-white flex flex-col">
      <Header />
      <div className="flex-1 px-4 py-6">
        <div className="max-w-6xl mx-auto space-y-6">
          <header className="flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-primary">
              <Sparkles className="h-5 w-5" />
              <span className="text-sm font-semibold tracking-wide">QPrint</span>
            </div>
            <h1 className="text-2xl font-semibold">
              {greeting}, {user?.firstName || 'Student'}!
            </h1>
            <p className="text-sm text-gray-400">{new Date().toLocaleDateString(undefined, { dateStyle: 'full' })}</p>
          </div>

          <div className="relative">
            <button
              type="button"
              onClick={() => setShowMenu((v) => !v)}
              className="h-11 w-11 rounded-full border border-white/10 bg-white/5 flex items-center justify-center text-sm font-semibold"
            >
              {user?.firstName?.[0] || 'U'}{user?.lastName?.[0] || ''}
            </button>
            {showMenu && (
              <div className="absolute right-0 mt-2 w-48 rounded-xl border border-white/10 bg-[#111827] shadow-lg z-20">
                <button
                  type="button"
                  className="w-full text-left px-4 py-2 text-sm text-gray-200 hover:bg-white/5 flex items-center gap-2"
                  onClick={() => navigate('/settings')}
                >
                  <User className="h-4 w-4" /> My Profile
                </button>
                <button
                  type="button"
                  className="w-full text-left px-4 py-2 text-sm text-gray-200 hover:bg-white/5 flex items-center gap-2"
                  onClick={() => navigate('/transactions')}
                >
                  <Receipt className="h-4 w-4" /> Order History
                </button>
                <button
                  type="button"
                  className="w-full text-left px-4 py-2 text-sm text-danger hover:bg-white/5 flex items-center gap-2"
                  onClick={handleLogout}
                >
                  <LogOut className="h-4 w-4" /> Logout
                </button>
              </div>
            )}
          </div>
        </header>

        <section className="grid gap-4 sm:grid-cols-2">
          {quickActions.map((action, idx) => (
            <motion.button
              type="button"
              key={action.title}
              onClick={() => navigate(action.to)}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.05 }}
              className={`text-left rounded-2xl border border-white/10 p-5 bg-gradient-to-br ${action.tone} hover:border-primary/40 transition`}
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-lg font-semibold">{action.title}</p>
                  <p className="text-sm text-gray-300">{action.subtitle}</p>
                </div>
                <div className="h-11 w-11 rounded-xl bg-white/5 flex items-center justify-center">
                  <action.icon className="h-5 w-5 text-primary" />
                </div>
              </div>
              {action.title === 'My Cart' && (
                <p className="mt-4 text-xs text-gray-300">{itemCount} item(s) in cart</p>
              )}
              {action.title === 'Order Status' && (
                <p className="mt-4 text-xs text-gray-300">{activeCount} active order(s)</p>
              )}
            </motion.button>
          ))}
        </section>

        <section className="grid gap-4 lg:grid-cols-[2fr,1fr]">
          <div className="glass-card border border-white/10 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-400">Recent activity</p>
                <h2 className="text-lg font-semibold">Latest transactions</h2>
              </div>
              <Button variant="ghost" onClick={() => navigate('/transactions')}>View all</Button>
            </div>
            {recentTransactions.length ? (
              <div className="space-y-3">
                {recentTransactions.map((tx) => (
                  <div key={tx.id} className="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 px-4 py-3">
                    <div>
                      <p className="text-sm font-semibold">₹{Number(tx.totalAmount || 0).toFixed(2)}</p>
                      <p className="text-xs text-gray-400">{tx.orderId}</p>
                    </div>
                    <span className="text-xs text-gray-300">{formatDate(tx.completedAt)}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-6 text-sm text-gray-300">
                No transactions yet. Place an order to see it here.
              </div>
            )}
          </div>

          <div className="glass-card border border-white/10 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-400">Selected shop</p>
                <h2 className="text-lg font-semibold">Campus Print Zone</h2>
              </div>
              <Clock className="h-5 w-5 text-primary" />
            </div>
            <div className="space-y-2 text-sm text-gray-300">
              <p>{selectedShop?.address || 'Near Gate 2, University Road'}</p>
              <p>Avg wait: {selectedShop?.avgWaitMinutes ?? 20} min</p>
              <p>Rating: {selectedShop?.rating ?? 4.5} · {selectedShop?.totalOrders ?? 1200} orders</p>
            </div>
            <Button variant="ghost" disabled title="Coming soon">
              Change shop
            </Button>
          </div>
        </section>

        <section className="glass-card border border-white/10 p-6 flex items-center justify-between">
          <div className="space-y-2">
            <h3 className="text-lg font-semibold">Need to print a new file?</h3>
            <p className="text-sm text-gray-300">Upload and get price estimates instantly.</p>
          </div>
          <Button onClick={() => navigate('/upload')} className="max-w-[160px]">
            Upload now
          </Button>
        </section>
      </div>
    </div>
    </div>
  );
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
