import { useState } from 'react';
import { Lock, Save, ShieldCheck, User } from 'lucide-react';
import toast from 'react-hot-toast';

import api from '../api/axios';
import Button from '../components/ui/Button';
import Header from '../components/Header';
import { useAuthStore } from '../store/authStore';
import { getErrorMessage } from '../utils/api';

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);

  const [profile, setProfile] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
  });
  const [savingProfile, setSavingProfile] = useState(false);

  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [savingPassword, setSavingPassword] = useState(false);

  const handleProfileSave = async () => {
    setSavingProfile(true);
    try {
      const { data } = await api.put('/auth/profile', profile);
      setUser(data?.data);
      toast.success('Profile updated');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not update profile'));
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordChange = async () => {
    if (passwords.newPassword !== passwords.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    setSavingPassword(true);
    try {
      await api.put('/auth/change-password', passwords);
      toast.success('Password updated');
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Could not change password'));
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div className="min-h-screen bg-background text-white flex flex-col">
      <Header />
      <div className="flex-1 px-4 py-6">
        <div className="max-w-5xl mx-auto space-y-6">
          <div>
            <p className="text-sm text-gray-400">Settings</p>
            <h1 className="text-2xl font-semibold">Account settings</h1>
          </div>

        <div className="grid gap-4 lg:grid-cols-[2fr,1fr]">
          <section className="glass-card border border-white/10 p-6 space-y-4">
            <div className="flex items-center gap-2 text-primary">
              <User className="h-5 w-5" />
              <h2 className="text-lg font-semibold">Profile</h2>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="text-sm text-gray-300">
                First name
                <input
                  value={profile.firstName}
                  onChange={(e) => setProfile((p) => ({ ...p, firstName: e.target.value }))}
                  className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-white"
                />
              </label>
              <label className="text-sm text-gray-300">
                Last name
                <input
                  value={profile.lastName}
                  onChange={(e) => setProfile((p) => ({ ...p, lastName: e.target.value }))}
                  className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-white"
                />
              </label>
              <label className="text-sm text-gray-300 sm:col-span-2">
                Email
                <div className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-gray-400">
                  {user?.email || '—'}
                </div>
              </label>
            </div>
            <Button onClick={handleProfileSave} loading={savingProfile} className="max-w-[200px]">
              <Save className="h-4 w-4" /> Save changes
            </Button>
          </section>

          <section className="glass-card border border-white/10 p-6 space-y-4">
            <div className="flex items-center gap-2 text-primary">
              <ShieldCheck className="h-5 w-5" />
              <h2 className="text-lg font-semibold">Account info</h2>
            </div>
            <div className="space-y-2 text-sm text-gray-300">
              <p>Member since: {formatDate(user?.createdAt)}</p>
              <p>Status: <span className="text-success">ACTIVE</span></p>
              <p>Role: STUDENT</p>
            </div>
          </section>
        </div>

        <section className="glass-card border border-white/10 p-6 space-y-4">
          <div className="flex items-center gap-2 text-primary">
            <Lock className="h-5 w-5" />
            <h2 className="text-lg font-semibold">Security</h2>
          </div>
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="text-sm text-gray-300">
              Current password
              <input
                type="password"
                value={passwords.currentPassword}
                onChange={(e) => setPasswords((p) => ({ ...p, currentPassword: e.target.value }))}
                className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-white"
              />
            </label>
            <label className="text-sm text-gray-300">
              New password
              <input
                type="password"
                value={passwords.newPassword}
                onChange={(e) => setPasswords((p) => ({ ...p, newPassword: e.target.value }))}
                className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-white"
              />
            </label>
            <label className="text-sm text-gray-300">
              Confirm password
              <input
                type="password"
                value={passwords.confirmPassword}
                onChange={(e) => setPasswords((p) => ({ ...p, confirmPassword: e.target.value }))}
                className="mt-2 w-full rounded-lg bg-white/5 border border-white/10 px-3 py-2 text-white"
              />
            </label>
          </div>
          <Button onClick={handlePasswordChange} loading={savingPassword} className="max-w-[220px]">
            Update password
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
  return d.toLocaleDateString(undefined, { dateStyle: 'medium' });
}
