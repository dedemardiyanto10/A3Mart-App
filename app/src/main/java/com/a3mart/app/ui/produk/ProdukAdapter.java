package com.a3mart.app.ui.produk;

import android.graphics.Color;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.databinding.ItemProdukBinding;

import com.a3mart.app.utils.FormatterUtils;
import java.util.ArrayList;
import java.util.List;

public class ProdukAdapter extends RecyclerView.Adapter<ProdukAdapter.ProdukViewHolder> {
    private List<Produk> list;
    private List<Produk> listFull;
    private OnItemLongClickListener listener;

    private OnQuickEditListener quickEditListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Produk produk, int position);
    }

    public interface OnQuickEditListener {
        void onStokChanged(Produk produk, int newStok);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.listener = listener;
    }

    public void setOnQuickEditListener(OnQuickEditListener listener) {
        this.quickEditListener = listener;
    }

    public ProdukAdapter(List<Produk> list) {
        this.list = list;
        this.listFull = new ArrayList<>(list);
    }

    public void updateData(List<Produk> newList) {

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new ProdukDiffCallback(this.list, newList));
        this.list.clear();
        this.list.addAll(newList);

        this.listFull = new ArrayList<>(newList);

        diffResult.dispatchUpdatesTo(this);
    }

    public void filter(String query) {
        String pattern = query.toLowerCase().trim();
        list.clear();

        if (pattern.isEmpty()) {
            list.addAll(listFull);
        } else {
            for (Produk p : listFull) {
                if (p.getNama().toLowerCase().contains(pattern)
                        || (p.getBarcode() != null
                                && p.getBarcode().toLowerCase().contains(pattern))) {
                    list.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProdukViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProdukBinding binding =
                ItemProdukBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProdukViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProdukViewHolder holder, int position) {
        Produk p = list.get(position);
        ItemProdukBinding b = holder.binding;

        b.tvNamaProduk.setText(p.getNama());
        b.tvHargaProduk.setText(FormatterUtils.formatRupiah(p.getHarga()));

        updateStokUI(b, p.getStok());

        if (p.getBarcode() != null && !p.getBarcode().isEmpty()) {
            b.tvBarcodeProduk.setVisibility(View.VISIBLE);
            b.tvBarcodeProduk.setText(p.getBarcode());
        } else {
            b.tvBarcodeProduk.setVisibility(View.GONE);
        }

        b.btnTambahStok.setOnClickListener(
                v -> {
                    int newStok = p.getStok() + 1;
                    p.setStok(newStok);
                    updateStokUI(b, newStok);

                    if (quickEditListener != null) {
                        quickEditListener.onStokChanged(p, newStok);
                    }
                });

        b.btnKurangStok.setOnClickListener(
                v -> {
                    if (p.getStok() > 0) {
                        int newStok = p.getStok() - 1;
                        p.setStok(newStok);
                        updateStokUI(b, newStok);

                        if (quickEditListener != null) {
                            quickEditListener.onStokChanged(p, newStok);
                        }
                    }
                });

        b.getRoot()
                .setOnLongClickListener(
                        v -> {
                            if (listener != null) {
                                listener.onItemLongClick(p, position);
                            }
                            return true;
                        });
    }

    private void updateStokUI(ItemProdukBinding b, int stok) {
        int warna;
        if (stok <= 0) {
            warna = Color.parseColor("#F44336");
            b.tvStokProduk.setText("Habis");
            b.btnKurangStok.setEnabled(false);
            b.btnKurangStok.setAlpha(0.3f);
        } else if (stok <= 5) {
            warna = Color.parseColor("#FF9800");
            b.tvStokProduk.setText(String.valueOf(stok));
            b.btnKurangStok.setEnabled(true);
            b.btnKurangStok.setAlpha(1.0f);
        } else {
            warna = Color.parseColor("#4CAF50");
            b.tvStokProduk.setText(String.valueOf(stok));
            b.btnKurangStok.setEnabled(true);
            b.btnKurangStok.setAlpha(1.0f);
        }

        b.tvStokProduk.setTextColor(warna);
        b.tvNamaProduk.setTextColor(warna);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ProdukViewHolder extends RecyclerView.ViewHolder {
        private final ItemProdukBinding binding;

        ProdukViewHolder(ItemProdukBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ProdukDiffCallback extends DiffUtil.Callback {
        private final List<Produk> oldList;
        private final List<Produk> newList;

        public ProdukDiffCallback(List<Produk> oldList, List<Produk> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Produk oldP = oldList.get(oldPos);
            Produk newP = newList.get(newPos);

            return oldP.getStok() == newP.getStok()
                    && oldP.getHarga() == newP.getHarga()
                    && oldP.getNama().equals(newP.getNama())
                    && oldP.getBarcode().equals(newP.getBarcode());
        }
    }
}
