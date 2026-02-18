package com.a3mart.app.ui.produk;

import java.util.UUID;

public class Produk {
    private String id;
    private String barcode;
    private String nama;
    private int harga;
    private int stok;

    public Produk(String id, String barcode, String nama, int harga, int stok) {
        this.id = id;
        this.barcode = barcode == null ? "" : barcode;
        this.nama = nama;
        this.harga = harga;
        this.stok = stok;
    }

    public Produk(String barcode, String nama, int harga, int stok) {
        this(UUID.randomUUID().toString(), barcode, nama, harga, stok);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public int getHarga() {
        return harga;
    }

    public void setHarga(int harga) {
        this.harga = harga;
    }

    public int getStok() {
        return stok;
    }

    public void setStok(int stok) {
        this.stok = stok;
    }
}
