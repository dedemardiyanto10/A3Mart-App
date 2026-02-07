package com.a3mart.app.ui.produk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProdukViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Produk>> mProdukList = new MutableLiveData<>();
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();
    private final String KEY_PRODUK = "list_produk";

    public ProdukViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("A3Mart_Prefs", Context.MODE_PRIVATE);
        loadData();
    }

    public LiveData<List<Produk>> getProdukList() {
        return mProdukList;
    }

    public void tambahProduk(String nama, int harga, int stok) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        currentList.add(new Produk(nama, harga, stok));
        mProdukList.setValue(currentList);
        saveData(currentList);
    }

    public void updateProduk(int position, String nama, int harga, int stok) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList != null) {
            currentList.set(position, new Produk(nama, harga, stok));
            mProdukList.setValue(currentList);
            saveData(currentList);
        }
    }

    public void hapusProduk(int position) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList != null) {
            currentList.remove(position);
            mProdukList.setValue(currentList);
            saveData(currentList);
        }
    }

    public void kurangiStok(String namaProduk, int jumlahDibel) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList != null) {
            for (Produk p : currentList) {
                if (p.getNama().equals(namaProduk)) {
                    p.setStok(p.getStok() - jumlahDibel);
                    break;
                }
            }
            mProdukList.setValue(currentList);
            saveData(currentList);
        }
    }

    private void saveData(List<Produk> list) {
        String json = gson.toJson(list);
        sharedPreferences.edit().putString(KEY_PRODUK, json).apply();
    }

    private void loadData() {
        String json = sharedPreferences.getString(KEY_PRODUK, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Produk>>() {}.getType();
            List<Produk> list = gson.fromJson(json, type);
            mProdukList.setValue(list);
        } else {
            List<Produk> dummy = new ArrayList<>();
            dummy.add(new Produk("Camel", 25000, 10));
            dummy.add(new Produk("Evo", 27000, 10));
            dummy.add(new Produk("Dji Sam Soe Refill", 25000, 10));
            dummy.add(new Produk("Ziga", 19000, 10));
            dummy.add(new Produk("Surya 16", 38000, 10));
            mProdukList.setValue(dummy);
            saveData(dummy);
        }
    }
}
