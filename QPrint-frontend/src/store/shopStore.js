import { create } from 'zustand';

export const useShopStore = create((set) => ({
  selectedShop: null,
  setShop: (shop) => set({ selectedShop: shop }),
}));
