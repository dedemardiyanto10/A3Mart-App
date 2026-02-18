package com.a3mart.app.utils;

import com.a3mart.app.ui.produk.Produk;
import com.a3mart.app.ui.transaksi.Transaksi;
import java.util.List;

public class A3MartBackupModel {
    public List<Transaksi> transactions;
    public List<Produk> products;

    public A3MartBackupModel(List<Transaksi> transactions, List<Produk> products) {
        this.transactions = transactions;
        this.products = products;
    }
}
