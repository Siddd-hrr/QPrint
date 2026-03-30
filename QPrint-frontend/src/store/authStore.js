import { create } from 'zustand';

export const useAuthStore = create((set) => ({
  accessToken: null,
  user: null,
  isAuthenticated: false,
  initialized: false,
  setAuth: (token, user) => set({ accessToken: token, user, isAuthenticated: Boolean(token) }),
  setUser: (user) => set((state) => ({ user, isAuthenticated: Boolean(state.accessToken) && Boolean(user) })),
  clearAuth: () => set({ accessToken: null, user: null, isAuthenticated: false }),
  setInitialized: (value) => set({ initialized: value }),
}));
