package com.a3mart.app.ui.produk;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.databinding.ItemProdukBinding;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProdukAdapter extends RecyclerView.Adapter<ProdukAdapter.ProdukViewHolder> {
    private List<Produk> list;
    private OnItemLongClickListener listener;

    public interface OnItemLongClickListener {
        void onItemLongClick(Produk produk, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.listener = listener;
    }

    public ProdukAdapter(List<Produk> list) {
        this.list = list;
    }

    public void updateData(List<Produk> newList) {
        this.list = newList;
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

        NumberFormat nf = NumberFormat.getInstance(new Locale("in", "ID"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        b.tvNamaProduk.setText(p.getNama());
        b.tvHargaProduk.setText("Rp" + nf.format(p.getHarga()));

        if (p.getStok() <= 0) {
            b.tvStokProduk.setText("Stok Habis");
            int merah = Color.parseColor("#F44336");
            b.tvStokProduk.setTextColor(merah);
            b.tvNamaProduk.setTextColor(merah);
        } else {
            b.tvStokProduk.setText(String.valueOf(p.getStok()));
            int hijau = Color.parseColor("#4CAF50");
            b.tvStokProduk.setTextColor(hijau);
            b.tvNamaProduk.setTextColor(hijau);
        }

        b.getRoot()
                .setOnLongClickListener(
                        v -> {
                            if (listener != null) {
                                listener.onItemLongClick(p, position);
                            }
                            return true;
                        });
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
}
