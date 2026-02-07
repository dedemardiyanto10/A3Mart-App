package com.a3mart.app.ui.transaksi;

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

public class TransaksiViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Transaksi>> transaksiList =
            new MutableLiveData<>(new ArrayList<>());
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    private final MutableLiveData<Long> totalHutang = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalLunas = new MutableLiveData<>(0L);

    public LiveData<Long> getTotalHutang() {
        return totalHutang;
    }

    public LiveData<Long> getTotalLunas() {
        return totalLunas;
    }

    public TransaksiViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("A3Mart_Prefs", Context.MODE_PRIVATE);
        loadData();
    }

    public LiveData<List<Transaksi>> getTransaksiList() {
        return transaksiList;
    }

    public void updateTransaksi(
            Transaksi lama, String namaP, String namaK, int qty, long total, String status) {
        List<Transaksi> currentList = transaksiList.getValue();
        if (currentList != null) {
            int index = -1;
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(lama.getId())) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                Transaksi updated =
                        new Transaksi(
                                lama.getId(), namaP, namaK, qty, total, lama.getTanggal(), status);

                currentList.set(index, updated);
                transaksiList.setValue(currentList);
                saveData(currentList);
            }
        }
    }

    public void hapusTransaksi(Transaksi t) {
        List<Transaksi> current = transaksiList.getValue();
        if (current != null) {
            current.removeIf(item -> item.getId().equals(t.getId()));
            transaksiList.setValue(current);
            saveData(current);
        }
    }

    public void hapusSemuaTransaksi() {
        List<Transaksi> currentList = transaksiList.getValue();
        if (currentList != null) {
            currentList.clear();
            transaksiList.setValue(currentList);
            saveData(currentList);
        }
    }

    public void hapusTransaksiTerfilter(String tipe) {
        List<Transaksi> currentList = transaksiList.getValue();
        if (currentList == null) return;

        List<Transaksi> newList = new ArrayList<>();

        for (Transaksi t : currentList) {
            boolean harusHapus = false;
            long nominal = t.getTotalHarga();
            String status = t.getStatus();

            if (tipe.equalsIgnoreCase("SEMUA")) {
                harusHapus = true;
            } else if (tipe.equalsIgnoreCase("Lunas")) {
                if ((status.equalsIgnoreCase("Lunas") || status.equalsIgnoreCase("Lunas_Hutang"))
                        && nominal >= 0) {
                    harusHapus = true;
                }
            } else if (tipe.equalsIgnoreCase("Hutang")) {
                if (status.equalsIgnoreCase("Hutang") && nominal > 0) {
                    harusHapus = true;
                }
            } else if (tipe.equalsIgnoreCase("Deposit")) {
                if (nominal < 0) {
                    harusHapus = true;
                }
            }

            if (!harusHapus) {
                newList.add(t);
            }
        }

        transaksiList.setValue(newList);
        saveData(newList);
    }

    public void tambahTransaksi(String namaP, String namaK, int qty, long total, String status) {
        List<Transaksi> oldList = transaksiList.getValue();
        List<Transaksi> newList = new ArrayList<>();
        if (oldList != null) newList.addAll(oldList);

        String id = String.valueOf(System.currentTimeMillis());
        String tgl =
                new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date());
        newList.add(0, new Transaksi(id, namaP, namaK, qty, total, tgl, status));

        String cariNama = namaK.trim().toLowerCase();

        long saldoTotal = 0;
        for (Transaksi t : newList) {
            if (t.getNamaKonsumen().trim().toLowerCase().equals(cariNama)) {
                saldoTotal += t.getTotalHarga();
            }
        }

        if (saldoTotal <= 0) {
            for (int i = 0; i < newList.size(); i++) {
                Transaksi t = newList.get(i);
                if (t.getNamaKonsumen().trim().toLowerCase().equals(cariNama)
                        && t.getStatus().equalsIgnoreCase("Hutang")) {

                    newList.set(
                            i,
                            new Transaksi(
                                    t.getId(),
                                    t.getNamaProduk(),
                                    t.getNamaKonsumen(),
                                    t.getJumlah(),
                                    t.getTotalHarga(),
                                    t.getTanggal(),
                                    "Lunas_Hutang"));
                }
            }
        }

        transaksiList.setValue(newList);
        saveData(newList);
    }

    public void lunasiSemuaHutang(String namaKonsumen) {
        List<Transaksi> currentList = transaksiList.getValue();
        if (currentList == null) return;

        List<Transaksi> newList = new ArrayList<>();
        String target = namaKonsumen.trim().toLowerCase();
        boolean adaPerubahan = false;

        for (Transaksi t : currentList) {
            if (t.getNamaKonsumen().trim().toLowerCase().equals(target)
                    && t.getStatus().equalsIgnoreCase("Hutang")) {

                newList.add(
                        new Transaksi(
                                t.getId(),
                                t.getNamaProduk(),
                                t.getNamaKonsumen(),
                                t.getJumlah(),
                                t.getTotalHarga(),
                                t.getTanggal(),
                                "Lunas_Hutang"));
                adaPerubahan = true;
            } else {
                newList.add(t);
            }
        }

        if (adaPerubahan) {
            transaksiList.setValue(newList);
            saveData(newList);
        }
    }

    private void saveData(List<Transaksi> list) {
        String json = gson.toJson(list);
        sharedPreferences.edit().putString("list_transaksi", json).apply();
    }

    public void importData(List<Transaksi> newList) {
        if (newList != null) {
            saveData(newList);
            hitungUlangNominal(newList);
            transaksiList.setValue(new ArrayList<>());
            transaksiList.postValue(newList);
        }
    }

    private void hitungUlangNominal(List<Transaksi> list) {
        long h = 0;
        long l = 0;
        if (list != null) {
            for (Transaksi t : list) {
                if (t.getStatus().equalsIgnoreCase("Hutang")) {
                    h += t.getTotalHarga();
                } else {
                    l += t.getTotalHarga();
                }
            }
        }
        totalHutang.postValue(h);
        totalLunas.postValue(l);
    }

    public String[] getDaftarKonsumenTetap() {
        return new String[] {
            "Umum", "Singgih", "Hasan", "Krisna", "Khamer", "Ipan", "Murat", "Amar", "Dede"
        };
    }

    private void loadData() {
        String json = sharedPreferences.getString("list_transaksi", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Transaksi>>() {}.getType();
            List<Transaksi> list = gson.fromJson(json, type);
            transaksiList.setValue(list);
        }
    }
}
