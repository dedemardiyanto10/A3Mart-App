package com.a3mart.app.ui.selisih;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import com.a3mart.app.ui.transaksi.Transaksi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelisihViewModel extends ViewModel {

    public List<Selisih> prosesRekapHutang(List<Transaksi> allTransaksi) {
        if (allTransaksi == null) return new ArrayList<>();

        Map<String, RekapData> groupMap = new HashMap<>();

        for (Transaksi t : allTransaksi) {
            String nama = t.getNamaKonsumen().trim();
            groupMap.computeIfAbsent(nama, k -> new RekapData()).tambah(t);
        }

        List<Selisih> hasil = new ArrayList<>();
        for (Map.Entry<String, RekapData> entry : groupMap.entrySet()) {
            RekapData d = entry.getValue();

            if (d.totalHarga != 0) {
                hasil.add(
                        new Selisih(entry.getKey(), d.listTransaksiAsli, d.totalQty, d.totalHarga));
            }
        }
        return hasil;
    }

    private static class RekapData {
        int totalQty = 0;
        long totalHarga = 0;
        List<Transaksi> listTransaksiAsli = new ArrayList<>();

        void tambah(Transaksi t) {
            long nominal = t.getTotalHarga();
            String status = t.getStatus();

            if (status.equalsIgnoreCase("Lunas") && nominal > 0) {
                return;
            }

            totalHarga += nominal;

            if (status.equalsIgnoreCase("Hutang") && nominal > 0) {
                totalQty += t.getJumlah();
            }

            listTransaksiAsli.add(t);
        }
    }
}
