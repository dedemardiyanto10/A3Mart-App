package com.a3mart.app.ui.selisih;

import com.a3mart.app.ui.transaksi.Transaksi;
import java.util.List;

public class Selisih {
    private String namaKonsumen;
    private List<Transaksi> listTransaksi;
    private int totalQty;
    private int totalHarga;

    public Selisih(
            String namaKonsumen, List<Transaksi> listTransaksi, int totalQty, int totalHarga) {
        this.namaKonsumen = namaKonsumen;
        this.listTransaksi = listTransaksi;
        this.totalQty = totalQty;
        this.totalHarga = totalHarga;
    }

    public String getNamaKonsumen() {
        return namaKonsumen;
    }

    public List<Transaksi> getListTransaksi() {
        return listTransaksi;
    }

    public int getTotalQty() {
        return totalQty;
    }

    public int getTotalHarga() {
        return totalHarga;
    }
}
