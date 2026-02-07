package com.a3mart.app.ui.selisih;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.R;
import com.a3mart.app.databinding.ItemProdukRekapBinding;
import com.a3mart.app.databinding.ItemSelisihBinding;
import com.a3mart.app.ui.transaksi.Transaksi;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class SelisihAdapter extends RecyclerView.Adapter<SelisihAdapter.ViewHolder> {
    private List<Selisih> list;
    private final OnRekapActionListener listener;

    public interface OnRekapActionListener {
        void onLunasi(Selisih selisih);

        void onSimpanBayarSebagian(Selisih selisih, long nominalBayar);

        void onPdfClick(Selisih selisih);

        void onShareClick(Selisih selisih);
    }

    public SelisihAdapter(List<Selisih> list, OnRekapActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateData(List<Selisih> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public List<Selisih> getDataList() {
        return list != null ? list : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelisihBinding binding =
                ItemSelisihBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Selisih s = list.get(position);
        ItemSelisihBinding b = holder.binding;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("in", "ID"));
        symbols.setCurrencySymbol("Rp");
        DecimalFormat df = new DecimalFormat("Rp#,##0.00;-Rp#,##0.00", symbols);

        int warnaHutang = Color.parseColor("#F44336");
        int warnaDeposit = Color.parseColor("#FF9800");

        if (s.getTotalHarga() < 0) {
            b.btnLunasi.setVisibility(android.view.View.GONE);
        } else {
            b.btnLunasi.setVisibility(android.view.View.VISIBLE);
            b.btnLunasi.setOnClickListener(v -> listener.onLunasi(s));
        }

        b.tvNamaRekap.setText(s.getNamaKonsumen());
        b.tvTotalHargaRekap.setText(df.format(s.getTotalHarga()));

        if (s.getTotalHarga() < 0) {
            b.tvNamaRekap.setTextColor(warnaDeposit);
            b.tvTotalHargaRekap.setTextColor(warnaDeposit);
            b.ivStatusRekap.setImageResource(R.drawable.ic_deposit);
            b.ivStatusRekap.setColorFilter(warnaDeposit);
        } else {
            b.tvNamaRekap.setTextColor(warnaHutang);
            b.tvTotalHargaRekap.setTextColor(warnaHutang);
            b.ivStatusRekap.setImageResource(R.drawable.ic_hutang);
            b.ivStatusRekap.setColorFilter(warnaHutang);
        }

        b.containerProduk.removeAllViews();
        Map<String, Transaksi> gabungan = new LinkedHashMap<>();

        for (Transaksi t : s.getListTransaksi()) {
            if (t.getTotalHarga() > 0) {
                if (gabungan.containsKey(t.getNamaProduk())) {
                    Transaksi lama = gabungan.get(t.getNamaProduk());
                    lama.setJumlah(lama.getJumlah() + t.getJumlah());
                    lama.setTotalHarga(lama.getTotalHarga() + t.getTotalHarga());
                } else {
                    gabungan.put(t.getNamaProduk(), copyTransaksi(t));
                }
            } else {
                gabungan.put(t.getNamaProduk() + "_" + t.getId(), copyTransaksi(t));
            }
        }

        for (Transaksi t : gabungan.values()) {
            ItemProdukRekapBinding bp =
                    ItemProdukRekapBinding.inflate(
                            LayoutInflater.from(b.getRoot().getContext()),
                            b.containerProduk,
                            false);

            long hargaSatuan = t.getJumlah() > 0 ? t.getTotalHarga() / t.getJumlah() : 0;

            bp.tvDetailProduk.setText(
                    t.getNamaProduk() + " [" + df.format(hargaSatuan) + "] x" + t.getJumlah());
            bp.tvSubtotalItem.setText(df.format(t.getTotalHarga()));

            b.containerProduk.addView(bp.getRoot());
        }

        b.btnLunasi.setOnClickListener(v -> listener.onLunasi(s));

        b.btnItemPdf.setOnClickListener(
                v -> {
                    if (listener != null) listener.onPdfClick(s);
                });

        b.btnItemShare.setOnClickListener(
                v -> {
                    if (listener != null) listener.onShareClick(s);
                });
    }

    private Transaksi copyTransaksi(Transaksi t) {
        return new Transaksi(
                t.getId(),
                t.getNamaProduk(),
                t.getNamaKonsumen(),
                t.getJumlah(),
                t.getTotalHarga(),
                t.getTanggal(),
                t.getStatus());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSelisihBinding binding;

        ViewHolder(ItemSelisihBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
