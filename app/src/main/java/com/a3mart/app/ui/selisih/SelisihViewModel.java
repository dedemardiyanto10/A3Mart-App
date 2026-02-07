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

    public LiveData<List<Selisih>> getRekapHutang(LiveData<List<Transaksi>> source) {
        return Transformations.map(source, this::prosesRekapHutang);
    }

    public List<Selisih> prosesRekapHutang(List<Transaksi> allTransaksi) {
        if (allTransaksi == null) return new ArrayList<>();

        Map<String, RekapData> groupMap = new HashMap<>();

        for (Transaksi t : allTransaksi) {
            String nama = t.getNamaKonsumen();
            if (!groupMap.containsKey(nama)) {
                groupMap.put(nama, new RekapData());
            }

            RekapData d = groupMap.get(nama);
            String status = t.getStatus();
            long nominal = t.getTotalHarga();

            if (status.equalsIgnoreCase("Hutang")
                    || status.equalsIgnoreCase("Lunas_Hutang")
                    || nominal < 0) {
                d.totalQty += t.getJumlah();
                d.totalHarga += nominal;
            }
            d.listTransaksiAsli.add(t);
        }

        List<Selisih> hasil = new ArrayList<>();
        for (Map.Entry<String, RekapData> entry : groupMap.entrySet()) {
            RekapData d = entry.getValue();
            boolean masihPunyaHutangBelumLunas = false;
            for (Transaksi t : d.listTransaksiAsli) {
                if (t.getStatus().equalsIgnoreCase("Hutang")) {
                    masihPunyaHutangBelumLunas = true;
                    break;
                }
            }

            if (d.totalHarga < 0 || (masihPunyaHutangBelumLunas && d.totalHarga > 0)) {
                hasil.add(
                        new Selisih(entry.getKey(), d.listTransaksiAsli, d.totalQty, d.totalHarga));
            }
        }
        return hasil;
    }

    private static class RekapData {
        int totalQty = 0;
        int totalHarga = 0;

        List<Transaksi> listTransaksiAsli = new ArrayList<>();

        void tambah(Transaksi t) {
            totalQty += t.getJumlah();
            totalHarga += t.getTotalHarga();
            listTransaksiAsli.add(t);
        }
    }
}
