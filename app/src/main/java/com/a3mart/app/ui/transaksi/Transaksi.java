package com.a3mart.app.ui.transaksi;

public class Transaksi {
    private String id;
    private String namaProduk;
    private String namaKonsumen;
    private int jumlah;
    private long totalHarga;
    private String tanggal;
    private String status;

    public Transaksi(
            String id,
            String namaProduk,
            String namaKonsumen,
            int jumlah,
            long totalHarga,
            String tanggal,
            String status) {
        this.id = id;
        this.namaProduk = namaProduk;
        this.namaKonsumen = namaKonsumen;
        this.jumlah = jumlah;
        this.totalHarga = totalHarga;
        this.tanggal = tanggal;
        this.status = status;
    }

    public String getId() {
        return this.id;
    }

    public String getNamaProduk() {
        return namaProduk;
    }

    public String getNamaKonsumen() {
        return namaKonsumen;
    }

    public int getJumlah() {
        return jumlah;
    }

    public long getTotalHarga() {
        return totalHarga;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getStatus() {
        return status;
    }

    public void setNamaProduk(String namaProduk) {
        this.namaProduk = namaProduk;
    }

    public void setNamaKonsumen(String namaKonsumen) {
        this.namaKonsumen = namaKonsumen;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = jumlah;
    }

    public void setTotalHarga(long totalHarga) {
        this.totalHarga = totalHarga;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
