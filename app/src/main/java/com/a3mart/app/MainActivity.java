package com.a3mart.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.a3mart.app.databinding.ActivityMainBinding;
import com.a3mart.app.ui.produk.Produk;
import com.a3mart.app.ui.produk.ProdukViewModel;
import com.a3mart.app.ui.transaksi.Transaksi;
import com.a3mart.app.ui.transaksi.TransaksiViewModel;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProdukViewModel produkViewModel;
    private TransaksiViewModel transaksiViewModel;
    
    private final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/USER_KAMU/REPO_KAMU/main/update.json";

    private final androidx.activity.result.ActivityResultLauncher<String>
            requestNotificationPermissionLauncher =
                    registerForActivityResult(
                            new androidx.activity.result.contract.ActivityResultContracts
                                    .RequestPermission(),
                            isGranted -> {
                                if (isGranted) {
                                    Toast.makeText(this, "Notifikasi aktif!", Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    Toast.makeText(
                                                    this,
                                                    "Izin ditolak, suara 'Kaching' tidak akan bunyi.",
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            });

    @Override
    protected void attachBaseContext(android.content.Context newBase) {

        android.content.SharedPreferences pref =
                newBase.getSharedPreferences("Settings", MODE_PRIVATE);
        int mode =
                pref.getInt(
                        "theme_mode",
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);

        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        if (pref.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        checkSecurity();
        checkNotificationPermission();
        setSupportActionBar(binding.toolbar);
        
        checkUpdate();

        produkViewModel = new ViewModelProvider(this).get(ProdukViewModel.class);
        transaksiViewModel = new ViewModelProvider(this).get(TransaksiViewModel.class);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        transaksiViewModel
                .getTransaksiList()
                .observe(
                        this,
                        list -> {
                            int currentPos = binding.viewPager.getCurrentItem();
                            if (currentPos == 0 || currentPos == 2) {
                                updateHeaderInstan(currentPos);
                            }
                        });

        produkViewModel
                .getProdukList()
                .observe(
                        this,
                        list -> {
                            int currentPos = binding.viewPager.getCurrentItem();
                            if (currentPos == 1) {
                                updateHeaderInstan(currentPos);
                            }
                        });

        binding.viewPager.setOffscreenPageLimit(2);

        updateHeaderInstan(0);

        binding.viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);

                        int menuId =
                                (position == 0)
                                        ? R.id.navigation_transaksi
                                        : (position == 1)
                                                ? R.id.navigation_produk
                                                : R.id.navigation_selisih;
                        binding.navView.getMenu().findItem(menuId).setChecked(true);

                        updateHeaderInstan(position);
                    }
                });

        binding.navView.setOnItemSelectedListener(
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.navigation_transaksi) binding.viewPager.setCurrentItem(0, true);
                    else if (id == R.id.navigation_produk)
                        binding.viewPager.setCurrentItem(1, true);
                    else if (id == R.id.navigation_selisih)
                        binding.viewPager.setCurrentItem(2, true);
                    return true;
                });
    }

    private void updateHeaderInstan(int position) {
        if (getSupportActionBar() == null) return;

        java.text.DecimalFormatSymbols symbols =
                new java.text.DecimalFormatSymbols(new Locale("in", "ID"));
        symbols.setCurrencySymbol("Rp");
        java.text.DecimalFormat df = new java.text.DecimalFormat("Rp#,##0.00;-Rp#,##0.00", symbols);

        switch (position) {
            case 0:
                getSupportActionBar().setTitle("Transaksi");
                List<Transaksi> listT = transaksiViewModel.getTransaksiList().getValue();
                if (listT != null) {
                    long total = 0;
                    for (Transaksi t : listT) total += t.getTotalHarga();
                    getSupportActionBar()
                            .setSubtitle("Total: " + listT.size() + " | " + df.format(total));
                } else {
                    getSupportActionBar().setSubtitle("Belum ada data");
                }
                break;

            case 1:
                getSupportActionBar().setTitle("Daftar Produk");
                List<Produk> listP = produkViewModel.getProdukList().getValue();
                if (listP != null) {
                    int stok = 0;
                    for (Produk p : listP) stok += p.getStok();
                    getSupportActionBar().setSubtitle("Total Stok: " + stok);
                } else {
                    getSupportActionBar().setSubtitle("Stok Kosong");
                }
                break;

            case 2:
                getSupportActionBar().setTitle("Rekap Hutang");
                List<Transaksi> listH = transaksiViewModel.getTransaksiList().getValue();
                if (listH != null) {
                    Set<String> daftarPeminjam = new HashSet<>();
                    long totalHutang = 0;
                    for (Transaksi t : listH) {
                        if (t.getStatus().equalsIgnoreCase("Hutang")) {
                            daftarPeminjam.add(t.getNamaKonsumen());
                            totalHutang += t.getTotalHarga();
                        }
                    }
                    getSupportActionBar()
                            .setSubtitle(
                                    daftarPeminjam.size() + " Orang | " + df.format(totalHutang));
                } else {
                    getSupportActionBar().setSubtitle("Tidak ada hutang");
                }
                break;
        }
    }

    private void checkSecurity() {
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean useFinger = pref.getBoolean("use_finger", false);

        if (useFinger) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(
                            () -> {
                                androidx.biometric.BiometricPrompt.PromptInfo promptInfo =
                                        new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                                .setTitle("A3 Mart Security")
                                                .setSubtitle("Gunakan sidik jari atau PIN HP")
                                                .setAllowedAuthenticators(
                                                        androidx.biometric.BiometricManager
                                                                        .Authenticators
                                                                        .BIOMETRIC_STRONG
                                                                | androidx.biometric
                                                                        .BiometricManager
                                                                        .Authenticators
                                                                        .DEVICE_CREDENTIAL)
                                                .build();

                                androidx.biometric.BiometricPrompt biometricPrompt =
                                        new androidx.biometric.BiometricPrompt(
                                                this,
                                                androidx.core.content.ContextCompat.getMainExecutor(
                                                        this),
                                                new androidx.biometric.BiometricPrompt
                                                        .AuthenticationCallback() {
                                                    @Override
                                                    public void onAuthenticationSucceeded(
                                                            @NonNull
                                                                    androidx.biometric
                                                                                    .BiometricPrompt
                                                                                    .AuthenticationResult
                                                                            result) {
                                                        super.onAuthenticationSucceeded(result);
                                                    }

                                                    @Override
                                                    public void onAuthenticationError(
                                                            int errorCode,
                                                            @NonNull CharSequence errString) {
                                                        super.onAuthenticationError(
                                                                errorCode, errString);
                                                        finish();
                                                    }
                                                });

                                biometricPrompt.authenticate(promptInfo);
                            },
                            300);
        }
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this, android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_notifications)
                    .setTitle("Aktifkan Notifikasi Kasir")
                    .setMessage(
                            "A3 Mart akan membunyikan suara 'Kaching!' tiap ada pembayaran lunas. Izinkan notifikasi di langkah selanjutnya ya!")
                    .setCancelable(false)
                    .setPositiveButton(
                            "Siap!",
                            (d, w) -> {
                                requestNotificationPermissionLauncher.launch(
                                        android.Manifest.permission.POST_NOTIFICATIONS);
                            })
                    .setNegativeButton("Nanti Saja", null)
                    .show();
        }
    }
    
    
   private void checkUpdate() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(UPDATE_JSON_URL);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                
                org.json.JSONObject json = new org.json.JSONObject(result);
                int latestVersion = json.getInt("versionCode");
                String downloadUrl = json.getString("downloadUrl");
                String changelog = json.getString("changelog");

                // Bandingkan dengan versi aplikasi sekarang (BuildConfig.VERSION_CODE)
                try {
    int currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
    if (latestVersion > currentVersion) {
        runOnUiThread(() -> showUpdateDialog(downloadUrl, changelog));
    }
} catch (Exception e) {
    e.printStackTrace();
}

            } catch (Exception e) {
                e.printStackTrace(); // Abaikan jika gagal koneksi (offline)
            }
        }).start();
    }

    private void showUpdateDialog(String url, String notes) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Update Baru Tersedia!")
                .setIcon(R.drawable.ic_backup) // Pastikan icon ini ada
                .setMessage("Apa yang baru di versi ini:\n" + notes)
                .setCancelable(false)
                .setPositiveButton("Update Sekarang", (d, w) -> downloadAndInstall(url))
                .setNegativeButton("Nanti", null)
                .show();
    }

    private void downloadAndInstall(String apkUrl) {
        // 1. Siapkan Dialog
        View dialogView = getLayoutInflater().inflate(R.layout.layout_download_progress, null);
        com.google.android.material.progressindicator.LinearProgressIndicator progressBar =
                dialogView.findViewById(R.id.progress_horizontal);
        TextView tvPercentage = dialogView.findViewById(R.id.tv_percentage);

        androidx.appcompat.app.AlertDialog progressDialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Memperbarui A3 Mart")
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

        progressDialog.show();

        // 2. Proses Download di Background
        new Thread(
                        () -> {
                            try {
                                java.net.URL url = new java.net.URL(apkUrl);
                                java.net.HttpURLConnection connection =
                                        (java.net.HttpURLConnection) url.openConnection();
                                connection.connect();

                                int fileLength = connection.getContentLength();
                                java.io.File apkFile =
                                        new java.io.File(getExternalFilesDir(null), "update.apk");

                                java.io.InputStream input =
                                        new java.io.BufferedInputStream(url.openStream());
                                java.io.OutputStream output = new java.io.FileOutputStream(apkFile);

                                byte[] data = new byte[4096];
                                long total = 0;
                                int count;
                                while ((count = input.read(data)) != -1) {
                                    total += count;
                                    if (fileLength > 0) {
                                        int progress = (int) (total * 100 / fileLength);
                                        runOnUiThread(
                                                () -> {
                                                    progressBar.setProgress(progress);
                                                    tvPercentage.setText(progress + "%");
                                                });
                                    }
                                    output.write(data, 0, count);
                                }

                                output.flush();
                                output.close();
                                input.close();

                                runOnUiThread(
                                        () -> {
                                            progressDialog.dismiss();
                                            installApk(apkFile);
                                        });

                            } catch (Exception e) {
                                runOnUiThread(
                                        () -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(
                                                            this,
                                                            "Gagal download update: "
                                                                    + e.getMessage(),
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        });
                            }
                        })
                .start();
    }

    private void installApk(java.io.File file) {
        android.net.Uri apkUri =
                androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".fileprovider", file);

        android.content.Intent intent =
                new android.content.Intent(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        // Untuk Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivity(
                        new android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                android.net.Uri.parse("package:" + getPackageName())));
                return;
            }
        }

        startActivity(intent);
    }
}
