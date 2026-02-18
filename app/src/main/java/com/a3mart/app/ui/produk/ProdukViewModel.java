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

    private final SharedPreferences.OnSharedPreferenceChangeListener mListener;

    public ProdukViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("A3Mart_Prefs", Context.MODE_PRIVATE);

        mListener =
                (prefs, key) -> {
                    if (KEY_PRODUK.equals(key)) {
                        loadData();
                    }
                };

        sharedPreferences.registerOnSharedPreferenceChangeListener(mListener);

        loadData();
    }

    public LiveData<List<Produk>> getProdukList() {
        return mProdukList;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
    }

    public void tambahProduk(String barcode, String nama, int harga, int stok) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList == null) currentList = new ArrayList<>();

        if (barcode != null && !barcode.isEmpty()) {
            for (Produk p : currentList) {
                if (barcode.equals(p.getBarcode())) {
                    return;
                }
            }
        }

        currentList.add(new Produk(barcode, nama, harga, stok));
        mProdukList.setValue(currentList);
        saveData(currentList);
    }

    public void updateProduk(int position, String barcode, String nama, int harga, int stok) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            currentList.set(position, new Produk(barcode, nama, harga, stok));
            mProdukList.setValue(currentList);
            saveData(currentList);
        }
    }

    public void hapusProduk(int position) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList != null && position >= 0 && position < currentList.size()) {
            currentList.remove(position);
            mProdukList.setValue(currentList);
            saveData(currentList);
        }
    }

    public void updateStok(String namaProduk, int nilai, boolean isAbsolute) {
        List<Produk> currentList = mProdukList.getValue();
        if (currentList == null) return;

        List<Produk> newList = new ArrayList<>();
        for (Produk p : currentList) {
            Produk newP = new Produk(p.getBarcode(), p.getNama(), p.getHarga(), p.getStok());
            newP.setId(p.getId());

            if (newP.getNama().equals(namaProduk)) {
                if (isAbsolute) {
                    newP.setStok(nilai);
                } else {
                    newP.setStok(newP.getStok() + nilai);
                }
            }
            newList.add(newP);
        }

        mProdukList.setValue(newList);
        saveData(newList);
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
            dummy.add(new Produk("", "Camel", 25000, 10));
            dummy.add(new Produk("", "Evo", 27000, 10));
            dummy.add(new Produk("", "Dji Sam Soe Refill", 25000, 10));
            dummy.add(new Produk("", "Ziga", 19000, 10));
            dummy.add(new Produk("", "Surya 16", 38000, 10));
            mProdukList.setValue(dummy);
            saveData(dummy);
        }
    }
}
