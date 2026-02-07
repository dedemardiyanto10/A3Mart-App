package com.a3mart.app.ui.transaksi;

import android.view.*;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.databinding.ItemTransaksiBinding;
import com.a3mart.app.R;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransaksiAdapter extends RecyclerView.Adapter<TransaksiAdapter.ViewHolder> {
    private List<Transaksi> list;
    private OnItemLongClickListener listener;
    private final Set<String> animatedIds = new HashSet<>();

    public interface OnItemLongClickListener {
        void onItemLongClick(Transaksi t, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.listener = listener;
    }

    public TransaksiAdapter(List<Transaksi> list) {
        this.list = list;
    }

    public void updateData(List<Transaksi> newList) {
        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new TransaksiDiffCallback(this.list, newList));
        this.list.clear();
        this.list.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                ItemTransaksiBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaksi t = list.get(position);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("in", "ID"));
        symbols.setCurrencySymbol("Rp");
        DecimalFormat df = new DecimalFormat("Rp#,##0.00;-Rp#,##0.00", symbols);

        String hexColor;
        if (t.getTotalHarga() < 0) {
            hexColor = "#FF9800";
        } else if (t.getStatus().equalsIgnoreCase("Lunas")
                || t.getStatus().equalsIgnoreCase("Lunas_Hutang")) {
            hexColor = "#4CAF50";
        } else if (t.getStatus().equalsIgnoreCase("Hutang")) {
            hexColor = "#F44336";
        } else {
            hexColor = "#BDBDBD";
        }

       // holder.binding.cardTransaksi.setStrokeColor(android.graphics.Color.parseColor(hexColor));
       // holder.binding.cardTransaksi.setStrokeWidth(2);

        holder.binding.ivStatusTransaksi.animate().cancel();

        if (!animatedIds.contains(t.getId())) {
            holder.binding.ivStatusTransaksi.setScaleX(2.5f);
            holder.binding.ivStatusTransaksi.setScaleY(2.5f);
            holder.binding
                    .ivStatusTransaksi
                    .animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            animatedIds.add(t.getId());
        } else {
            holder.binding.ivStatusTransaksi.setScaleX(1f);
            holder.binding.ivStatusTransaksi.setScaleY(1f);
        }

        holder.binding.tvNamaTransaksi.setText(t.getNamaProduk());
        holder.binding.tvQtyTransaksi.setText(t.getJumlah() + " pcs");
        holder.binding.tvTotalTransaksi.setText(df.format(t.getTotalHarga()));
        holder.binding.tvTanggalTransaksi.setText(t.getTanggal());
        holder.binding.tvKonsumenTransaksi.setText(t.getNamaKonsumen());

        if (t.getStatus() != null) {
            if (t.getTotalHarga() < 0) {
                int warnaKuning = android.graphics.Color.parseColor("#FF9800");
                int warnaHijau = android.graphics.Color.parseColor("#4CAF50");

                holder.binding.ivStatusTransaksi.setImageResource(R.drawable.ic_deposit);
                holder.binding.ivStatusTransaksi.setColorFilter(warnaKuning);

                holder.binding.tvKonsumenTransaksi.setTextColor(warnaKuning);
                holder.binding.tvTotalTransaksi.setTextColor(warnaKuning);

            } else {
                boolean isLunas =
                        t.getStatus().equalsIgnoreCase("Lunas")
                                || t.getStatus().equalsIgnoreCase("Lunas_Hutang");

                holder.binding.ivStatusTransaksi.setImageResource(
                        isLunas ? R.drawable.ic_lunas : R.drawable.ic_hutang);

                int warnaStatus =
                        isLunas
                                ? android.graphics.Color.parseColor("#4CAF50")
                                : android.graphics.Color.parseColor("#F44336");

                holder.binding.tvKonsumenTransaksi.setTextColor(warnaStatus);
                holder.binding.tvTotalTransaksi.setTextColor(warnaStatus);

                if (isLunas) {
                    holder.binding.ivStatusTransaksi.setColorFilter(warnaStatus);
                } else {
                    holder.binding.ivStatusTransaksi.setColorFilter(warnaStatus);
                }
            }
        }
        holder.itemView.setOnLongClickListener(
                v -> {
                    if (listener != null) listener.onItemLongClick(t, position);
                    return true;
                });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemTransaksiBinding binding;

        public ViewHolder(ItemTransaksiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class TransaksiDiffCallback extends DiffUtil.Callback {
        private final List<Transaksi> oldList, newList;

        public TransaksiDiffCallback(List<Transaksi> oldList, List<Transaksi> newList) {
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
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }
}
