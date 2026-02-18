package com.a3mart.app.ui.selisih;

import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.R;
import com.a3mart.app.databinding.ItemProdukRekapBinding;
import com.a3mart.app.databinding.ItemSelisihBinding;
import com.a3mart.app.ui.transaksi.Transaksi;

import com.a3mart.app.utils.FormatterUtils;
import java.util.*;

public class SelisihAdapter extends RecyclerView.Adapter<SelisihAdapter.ViewHolder> {
    private List<Selisih> list;
    private final OnRekapActionListener listener;

    public interface OnRekapActionListener {
        void onLunasi(Selisih selisih);

        void onSimpanBayarSebagian(Selisih selisih, long nominalBayar);

        void onPdfClick(Selisih selisih);

        void onShareClick(Selisih selisih);

        void onStrukClick(Selisih selisih);
    }

    public interface OnSelisihLongClickListener {
        void onLongClick(Selisih selisih);
    }

    private OnSelisihLongClickListener longClickListener;

    public void setOnLongClickListener(OnSelisihLongClickListener listener) {
        this.longClickListener = listener;
    }

    public SelisihAdapter(List<Selisih> list, OnRekapActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateData(List<Selisih> newList) {

        if (newList != null) {
            Collections.sort(
                    newList,
                    (s1, s2) -> {
                        long lastId1 = 0;
                        long lastId2 = 0;

                        if (!s1.getListTransaksi().isEmpty()) {
                            for (Transaksi t : s1.getListTransaksi())
                                lastId1 = Math.max(lastId1, Long.parseLong(t.getId()));
                        }
                        if (!s2.getListTransaksi().isEmpty()) {
                            for (Transaksi t : s2.getListTransaksi())
                                lastId2 = Math.max(lastId2, Long.parseLong(t.getId()));
                        }

                        return Long.compare(lastId2, lastId1);
                    });
        }

        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult =
                androidx.recyclerview.widget.DiffUtil.calculateDiff(
                        new SelisihDiffCallback(this.list, newList));

        this.list = new ArrayList<>(newList);

        diffResult.dispatchUpdatesTo(this);
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

        android.content.SharedPreferences pref =
                holder.itemView
                        .getContext()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        boolean isStrukAktif = pref.getBoolean("show_struk_btn", false);

        if (isStrukAktif) {
            holder.binding.btnStruk.setVisibility(View.VISIBLE);
        } else {
            holder.binding.btnStruk.setVisibility(View.GONE);
        }

        int warnaHutang = Color.parseColor("#F44336");
        int warnaDeposit = Color.parseColor("#FF9800");

        if (s.getListTransaksi() != null && !s.getListTransaksi().isEmpty()) {
            String tanggalMentah = "";
            long maxId = 0;
            for (Transaksi t : s.getListTransaksi()) {
                try {
                    long currentId = Long.parseLong(t.getId());
                    if (currentId > maxId) {
                        maxId = currentId;
                        tanggalMentah = t.getTanggal();
                    }
                } catch (Exception e) {
                    if (tanggalMentah.isEmpty()) tanggalMentah = t.getTanggal();
                }
            }
            updateTanggalMenarik(b.tvTanggalUpdateRekap, tanggalMentah);
        }

        b.tvNamaRekap.setText(s.getNamaKonsumen());
        b.tvTotalHargaRekap.setText(FormatterUtils.formatRupiah(s.getTotalHarga()));

        if (s.getTotalHarga() < 0) {
            b.tvNamaRekap.setTextColor(warnaDeposit);
            b.tvTotalHargaRekap.setTextColor(warnaDeposit);
            b.ivStatusRekap.setImageResource(R.drawable.ic_deposit);
            b.ivStatusRekap.setColorFilter(warnaDeposit);
            b.btnLunasi.setVisibility(android.view.View.GONE);
        } else {
            b.tvNamaRekap.setTextColor(warnaHutang);
            b.tvTotalHargaRekap.setTextColor(warnaHutang);
            b.ivStatusRekap.setImageResource(R.drawable.ic_hutang);
            b.ivStatusRekap.setColorFilter(warnaHutang);
            b.btnLunasi.setVisibility(android.view.View.VISIBLE);
            b.btnLunasi.setOnClickListener(v -> listener.onLunasi(s));
        }
        b.containerProduk.removeAllViews();

        List<Transaksi> listTampil = new ArrayList<>();
        Map<String, Transaksi> produkGabungan = new LinkedHashMap<>();

        for (Transaksi t : s.getListTransaksi()) {
            if (t.getNamaProduk().equalsIgnoreCase("Hutang Tambahan") || t.getTotalHarga() < 0) {
                listTampil.add(copyTransaksi(t));
            } else {
                if (produkGabungan.containsKey(t.getNamaProduk())) {
                    Transaksi lama = produkGabungan.get(t.getNamaProduk());
                    lama.setJumlah(lama.getJumlah() + t.getJumlah());
                    lama.setTotalHarga(lama.getTotalHarga() + t.getTotalHarga());
                } else {
                    produkGabungan.put(t.getNamaProduk(), copyTransaksi(t));
                }
            }
        }

        listTampil.addAll(produkGabungan.values());

        for (Transaksi t : listTampil) {
            ItemProdukRekapBinding bp =
                    ItemProdukRekapBinding.inflate(
                            LayoutInflater.from(b.getRoot().getContext()),
                            b.containerProduk,
                            false);

            bp.tvNamaProduk.setText(t.getNamaProduk());
            long hargaSatuan =
                    (t.getJumlah() != 0) ? Math.abs(t.getTotalHarga() / t.getJumlah()) : 0;

            bp.tvDetailQtyHarga.setText(
                    String.format(
                            Locale.getDefault(),
                            "%d x %s",
                            t.getJumlah(),
                            FormatterUtils.formatRupiah(hargaSatuan)));

            bp.tvSubtotalItem.setText(FormatterUtils.formatRupiah(t.getTotalHarga()));
            b.containerProduk.addView(bp.getRoot());
        }
        b.btnItemPdf.setOnClickListener(
                v -> {
                    if (listener != null) listener.onPdfClick(s);
                });

        b.btnItemShare.setOnClickListener(
                v -> {
                    if (listener != null) listener.onShareClick(s);
                });

        b.btnStruk.setOnClickListener(
                v -> {
                    if (listener != null) listener.onStrukClick(s);
                });

        holder.itemView.setOnLongClickListener(
                v -> {
                    if (longClickListener != null) {
                        int currentPos = holder.getBindingAdapterPosition();

                        if (currentPos != RecyclerView.NO_POSITION) {
                            longClickListener.onLongClick(list.get(currentPos));
                        }
                    }
                    return true;
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

    private void updateTanggalMenarik(TextView tv, String tanggalTransaksi) {
        try {
            SimpleDateFormat sdfInput =
                    new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Date dateTgl = sdfInput.parse(tanggalTransaksi);
            if (dateTgl == null) {
                tv.setText("Diupdate: " + tanggalTransaksi);
                return;
            }

            Calendar calTx = Calendar.getInstance();
            calTx.setTime(dateTgl);
            Calendar calNow = Calendar.getInstance();

            String prefix = "Diupdate: ";
            String hasilTgl;

            if (calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
                    && calTx.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)) {

                String jam = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateTgl);
                hasilTgl = "Hari ini, " + jam;

                SpannableString ss = new SpannableString(prefix + hasilTgl);
                ss.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                        prefix.length(),
                        prefix.length() + 8,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(ss);

            } else {
                if (calTx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)) {
                    hasilTgl =
                            new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                    .format(dateTgl);
                } else {
                    hasilTgl =
                            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(dateTgl);
                }
                tv.setText(prefix + hasilTgl);
            }
        } catch (Exception e) {
            tv.setText("Diupdate: " + tanggalTransaksi);
        }
    }

    private static class SelisihDiffCallback
            extends androidx.recyclerview.widget.DiffUtil.Callback {
        private final List<Selisih> oldList;
        private final List<Selisih> newList;

        public SelisihDiffCallback(List<Selisih> oldList, List<Selisih> newList) {
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
            return oldList.get(oldPos)
                    .getNamaKonsumen()
                    .equals(newList.get(newPos).getNamaKonsumen());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getTotalHarga() == newList.get(newPos).getTotalHarga();
        }
    }
}
