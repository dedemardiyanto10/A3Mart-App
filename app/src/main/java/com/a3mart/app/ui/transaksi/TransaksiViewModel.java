package com.a3mart.app.ui.transaksi;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.a3mart.app.ui.produk.Produk;
import com.a3mart.app.utils.A3MartBackupModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransaksiViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Transaksi>> transaksiList =
            new MutableLiveData<>(new ArrayList<>());
    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    private static final String ALGORITHM = "AES";
    private static final String KEY = "A3Mart_Secure_16";
    private static final String PREF_AUTO_BACKUP = "auto_backup_enabled";

    private static final String PREF_TOTAL_HUTANG_FIX = "total_hutang_fix";
    private static final String PREF_TOTAL_LUNAS_FIX = "total_lunas_fix";
    private static final String PREF_TOTAL_DEPOSIT_FIX = "total_deposit_fix";

    private final MutableLiveData<Long> totalHutang = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalLunas = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> totalDeposit = new MutableLiveData<>(0L);

    public LiveData<Long> getTotalHutang() {
        return totalHutang;
    }

    public LiveData<Long> getTotalLunas() {
        return totalLunas;
    }

    public LiveData<Long> getTotalDeposit() {
        return totalDeposit;
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
        if (currentList == null) return;

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
            hitungUlangSemua();
            saveData(currentList);
        }
    }

    public void hapusTransaksi(Transaksi t) {
        List<Transaksi> current = transaksiList.getValue();
        if (current != null) {
            current.removeIf(item -> item.getId().equals(t.getId()));
            transaksiList.setValue(current);
            hitungUlangSemua();
            saveData(current);
        }
    }

    public void hapusSemuaTransaksi() {
        List<Transaksi> currentList = transaksiList.getValue();
        if (currentList != null) {
            currentList.clear();
            transaksiList.setValue(currentList);
            saveData(currentList);

            sharedPreferences
                    .edit()
                    .putLong(PREF_TOTAL_HUTANG_FIX, 0)
                    .putLong(PREF_TOTAL_LUNAS_FIX, 0)
                    .putLong(PREF_TOTAL_DEPOSIT_FIX, 0)
                    .apply();

            totalHutang.setValue(0L);
            totalLunas.setValue(0L);
            totalDeposit.setValue(0L);
        }
    }

    public void hapusTransaksiTerfilter(String tipe) {
        List<Transaksi> current = transaksiList.getValue();
        if (current == null) return;

        List<Transaksi> newList = new ArrayList<>();
        for (Transaksi t : current) {
            boolean hapus = false;
            long nominal = t.getTotalHarga();
            String status = t.getStatus();

            if (tipe.equalsIgnoreCase("Lunas")) {
                if ((status.contains("Lunas")) && nominal >= 0) hapus = true;
            } else if (tipe.equalsIgnoreCase("Hutang")) {
                if (status.equalsIgnoreCase("Hutang")) hapus = true;
            } else if (tipe.equalsIgnoreCase("Deposit")) {
                if (nominal < 0) hapus = true;
            } else if (tipe.equalsIgnoreCase("SEMUA")) {
                hapus = true;
            }

            if (!hapus) newList.add(t);
        }

        transaksiList.setValue(newList);
        saveData(newList);
    }

    public void tambahTransaksi(String namaP, String namaK, int qty, long total, String status) {
        List<Transaksi> current = transaksiList.getValue();
        if (current == null) current = new ArrayList<>();

        String id = String.valueOf(System.currentTimeMillis());
        String tgl =
                new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date());

        current.add(0, new Transaksi(id, namaP, namaK, qty, total, tgl, status));
        transaksiList.setValue(current);
        hitungUlangSemua();
        saveData(current);
    }

    private void prosesPelunasanOtomatis(List<Transaksi> list, String namaKonsumen) {
        String target = namaKonsumen.trim().toLowerCase();
        long saldoBersih = 0;

        for (Transaksi t : list) {
            if (t.getNamaKonsumen().trim().toLowerCase().equals(target)) {
                if (t.getStatus().equalsIgnoreCase("Hutang") || t.getTotalHarga() < 0) {
                    saldoBersih += t.getTotalHarga();
                }
            }
        }

        if (saldoBersih <= 0) {
            for (Transaksi t : list) {
                if (t.getNamaKonsumen().trim().toLowerCase().equals(target)
                        && t.getStatus().equalsIgnoreCase("Hutang")) {

                    t.setStatus("Lunas_Hutang");
                }
            }
        }
    }

    private void updateRekapPermanen(String key, long nominal, MutableLiveData<Long> liveData) {
        long currentVal = sharedPreferences.getLong(key, 0);
        long newVal = currentVal + nominal;
        sharedPreferences.edit().putLong(key, newVal).apply();
        liveData.postValue(newVal);
    }

    public void lunasiSemuaHutang(String namaKonsumen) {
        List<Transaksi> current = transaksiList.getValue();
        if (current == null || current.isEmpty()) return;

        String target = namaKonsumen.trim().toLowerCase();
        long saldoAktif = 0;

        for (Transaksi t : current) {
            if (t.getNamaKonsumen().trim().toLowerCase().equals(target)) {
                if (!t.getStatus().equalsIgnoreCase("Lunas")) {
                    saldoAktif += t.getTotalHarga();
                }
            }
        }

        if (saldoAktif > 0) {
            String id = String.valueOf(System.currentTimeMillis());
            String tgl =
                    new java.text.SimpleDateFormat(
                                    "dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            .format(new java.util.Date());

            Transaksi pelunas =
                    new Transaksi(
                            id,
                            "Pelunasan Saldo",
                            namaKonsumen,
                            0,
                            -saldoAktif,
                            tgl,
                            "Lunas_Hutang");

            List<Transaksi> newList = new ArrayList<>(current);
            newList.add(0, pelunas);

            transaksiList.setValue(newList);
            hitungUlangSemua();
            saveData(newList);
        }
    }

    private void saveData(List<Transaksi> listTransaksi) {
        String jsonT = gson.toJson(listTransaksi);
        sharedPreferences.edit().putString("list_transaksi", jsonT).apply();

        String jsonP = sharedPreferences.getString("list_produk", null);

        List<Produk> listProduk;
        if (jsonP != null) {
            Type typeP = new TypeToken<ArrayList<Produk>>() {}.getType();
            listProduk = gson.fromJson(jsonP, typeP);
        } else {
            listProduk = new ArrayList<>();
        }

        SharedPreferences settingsPref =
                getApplication().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        if (settingsPref.getBoolean("auto_backup_enabled", false)) {
            autoBackupSilent(listTransaksi, listProduk);
        }
    }

    public void importData(String jsonTerdekripsi) {
        try {
            A3MartBackupModel dataPaket = gson.fromJson(jsonTerdekripsi, A3MartBackupModel.class);

            if (dataPaket != null) {
                if (dataPaket.products != null) {
                    String jsonP = gson.toJson(dataPaket.products);
                    sharedPreferences.edit().putString("list_produk", jsonP).commit();
                }

                if (dataPaket.transactions != null && !dataPaket.transactions.isEmpty()) {
                    List<Transaksi> listBaru = dataPaket.transactions;

                    String jsonT = gson.toJson(listBaru);
                    sharedPreferences.edit().putString("list_transaksi", jsonT).commit();

                    transaksiList.postValue(new ArrayList<>(listBaru));

                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(
                                    () -> {
                                        hitungUlangSemua();
                                    });

                    android.util.Log.d("A3Mart", "Transaksi masuk: " + listBaru.size());
                }
            }
        } catch (Exception e) {
            android.util.Log.e("A3Mart", "Gagal import: " + e.getMessage());
        }
    }

    public void importData(List<Transaksi> newList) {
        if (newList != null) {
            saveData(newList);
            transaksiList.postValue(new ArrayList<>(newList));

            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(
                            () -> {
                                hitungUlangSemua();
                            });
        }
    }

    public void hitungUlangSemua() {
        List<Transaksi> list = transaksiList.getValue();
        if (list == null || list.isEmpty()) {
            resetSemuaRekap();
            return;
        }

        long totalPendapatanLunasMurni = 0;
        HashMap<String, Long> poolDeposit = new HashMap<>();
        for (Transaksi t : list) {
            String nama = t.getNamaKonsumen().trim().toLowerCase();
            long nominal = t.getTotalHarga();
            String status = t.getStatus();

            if (status.equalsIgnoreCase("Lunas") && nominal > 0) {
                totalPendapatanLunasMurni += nominal;
            } else if (nominal < 0) {
                long sisaDep = poolDeposit.getOrDefault(nama, 0L);
                poolDeposit.put(nama, sisaDep + Math.abs(nominal));
                t.setStatus("Lunas_Hutang");
            }
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            Transaksi t = list.get(i);
            String nama = t.getNamaKonsumen().trim().toLowerCase();
            long nominal = t.getTotalHarga();

            if (!t.getStatus().equalsIgnoreCase("Lunas") && nominal > 0) {
                long sisaUangPelihat = poolDeposit.getOrDefault(nama, 0L);

                if (sisaUangPelihat >= nominal) {
                    t.setStatus("Lunas_Hutang");
                    poolDeposit.put(nama, sisaUangPelihat - nominal);
                } else if (sisaUangPelihat > 0) {
                    t.setStatus("Hutang");
                    poolDeposit.put(nama, 0L);
                } else {
                    t.setStatus("Hutang");
                }
            }
        }

        long finalHutang = 0;
        long finalDeposit = 0;
        long totalLunasAkhir = totalPendapatanLunasMurni;

        for (Long sisa : poolDeposit.values()) {
            finalDeposit += sisa;
        }

        for (Transaksi t : list) {
            if (t.getStatus().equalsIgnoreCase("Hutang")) {
                finalHutang += t.getTotalHarga();
            } else if (t.getStatus().equalsIgnoreCase("Lunas_Hutang") && t.getTotalHarga() > 0) {
                totalLunasAkhir += t.getTotalHarga();
            }
        }

        totalLunas.setValue(totalLunasAkhir);
        totalHutang.setValue(finalHutang);
        totalDeposit.setValue(finalDeposit);

        sharedPreferences
                .edit()
                .putLong(PREF_TOTAL_HUTANG_FIX, finalHutang)
                .putLong(PREF_TOTAL_LUNAS_FIX, totalLunasAkhir)
                .putLong(PREF_TOTAL_DEPOSIT_FIX, finalDeposit)
                .apply();

        transaksiList.setValue(new ArrayList<>(list));
    }

    public String[] getDaftarKonsumenTetap() {
        return new String[] {
            "Umum", "Singgih", "Hasan", "Krisna", "Khamer", "Ipan", "Murat", "Amar", "Dede"
        };
    }

    public void prosesBarcodeKeTransaksi(Produk produk) {
        tambahTransaksi(produk.getNama(), "Umum", 1, (long) produk.getHarga(), "Lunas");
    }

    private void autoBackupSilent(List<Transaksi> dataList, List<Produk> produkList) {
        if (dataList == null || dataList.isEmpty()) return;

        new Thread(
                        () -> {
                            try {

                                String hariIni =
                                        new java.text.SimpleDateFormat(
                                                        "EEEE", new java.util.Locale("id", "ID"))
                                                .format(new java.util.Date());

                                A3MartBackupModel paketLengkap =
                                        new A3MartBackupModel(dataList, produkList);
                                String jsonString = gson.toJson(paketLengkap);

                                javax.crypto.spec.SecretKeySpec secretKey =
                                        new javax.crypto.spec.SecretKeySpec(
                                                KEY.getBytes(), ALGORITHM);
                                javax.crypto.Cipher cipher =
                                        javax.crypto.Cipher.getInstance(ALGORITHM);
                                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);

                                byte[] encryptedBytes = cipher.doFinal(jsonString.getBytes());
                                String encryptedJson =
                                        android.util.Base64.encodeToString(
                                                encryptedBytes, android.util.Base64.DEFAULT);

                                File folder =
                                        new File(
                                                android.os.Environment
                                                        .getExternalStoragePublicDirectory(
                                                                android.os.Environment
                                                                        .DIRECTORY_DOCUMENTS),
                                                "A3Mart/Backup");

                                if (!folder.exists() && !folder.mkdirs()) return;

                                File file = new File(folder, "A3Mart_AB_" + hariIni + ".bak");

                                try (java.io.FileOutputStream fos =
                                        new java.io.FileOutputStream(file)) {
                                    fos.write(encryptedJson.getBytes());
                                }

                                android.util.Log.d(
                                        "A3Mart_Backup", "Auto Backup Berhasil: " + file.getName());
                            } catch (Exception e) {
                                android.util.Log.e(
                                        "A3Mart_Backup", "Gagal auto backup: " + e.getMessage());
                            }
                        })
                .start();
    }

    public void resetSemuaRekap() {
        sharedPreferences
                .edit()
                .putLong(PREF_TOTAL_HUTANG_FIX, 0)
                .putLong(PREF_TOTAL_LUNAS_FIX, 0)
                .putLong(PREF_TOTAL_DEPOSIT_FIX, 0)
                .apply();

        totalHutang.postValue(0L);
        totalLunas.postValue(0L);
        totalDeposit.postValue(0L);

        hapusSemuaTransaksi();
    }

    private void loadData() {
        String json = sharedPreferences.getString("list_transaksi", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Transaksi>>() {}.getType();
            List<Transaksi> list = gson.fromJson(json, type);
            transaksiList.setValue(list);
        }

        totalHutang.setValue(sharedPreferences.getLong(PREF_TOTAL_HUTANG_FIX, 0));
        totalLunas.setValue(sharedPreferences.getLong(PREF_TOTAL_LUNAS_FIX, 0));
        totalDeposit.setValue(sharedPreferences.getLong(PREF_TOTAL_DEPOSIT_FIX, 0));
    }
}
