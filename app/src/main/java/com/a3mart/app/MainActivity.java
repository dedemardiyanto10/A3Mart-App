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
import com.a3mart.app.utils.DialogUtils;
import com.a3mart.app.utils.FormatterUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProdukViewModel produkViewModel;
    private TransaksiViewModel transaksiViewModel;
    private Thread downloadThread;

    private final String UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/dedemardiyanto10/A3Mart-App/main/update.json";

    private final androidx.activity.result.ActivityResultLauncher<String[]>
            requestPermissionsLauncher =
                    registerForActivityResult(
                            new androidx.activity.result.contract.ActivityResultContracts
                                    .RequestMultiplePermissions(),
                            result -> {
                                Boolean camera =
                                        result.getOrDefault(
                                                android.Manifest.permission.CAMERA, false);
                                if (camera) {
                                    smartToast("Izin Kamera Aktif");
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

        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        if (pref.getBoolean("keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        checkSecurity();
        checkAllPermissions();
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

    private void mintaIzinBorongan() {
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions =
                    new String[] {
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    };
        } else {
            permissions =
                    new String[] {
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    };
        }

        requestPermissionsLauncher.launch(permissions);
    }

    private void checkAllPermissions() {
        boolean butuhKamera =
                androidx.core.content.ContextCompat.checkSelfPermission(
                                this, android.Manifest.permission.CAMERA)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED;

        boolean butuhNotif = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            butuhNotif =
                    androidx.core.content.ContextCompat.checkSelfPermission(
                                    this, android.Manifest.permission.POST_NOTIFICATIONS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED;
        }

        if (butuhKamera || butuhNotif) {
            androidx.appcompat.app.AlertDialog dialog =
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setIcon(R.drawable.ic_notifications)
                            .setTitle("Izin Akses Aplikasi")
                            .setMessage(
                                    "A3 Mart memerlukan izin Kamera (Scan Barcode) dan Notifikasi (Suara Pembayaran).")
                            .setCancelable(false)
                            .setPositiveButton(
                                    "Izinkan Semua",
                                    (d, w) -> {
                                        mintaIzinBorongan();
                                    })
                            .setNegativeButton("Nanti Saja", null)
                            .create();

            DialogUtils.terapkanEfekMewah(dialog);

            dialog.show();
        }
    }

    private void updateHeaderInstan(int position) {
        if (getSupportActionBar() == null) return;

        switch (position) {
            case 0:
                getSupportActionBar().setTitle("Transaksi");
                List<Transaksi> listT = transaksiViewModel.getTransaksiList().getValue();
                if (listT != null) {
                    long total = 0;
                    for (Transaksi t : listT) total += t.getTotalHarga();
                    getSupportActionBar()
                            .setSubtitle(
                                    "Total: "
                                            + listT.size()
                                            + " | "
                                            + FormatterUtils.formatRupiah(total));
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
                                    daftarPeminjam.size()
                                            + " Orang | "
                                            + FormatterUtils.formatRupiah(totalHutang));
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

    private void checkUpdate() {
        new Thread(
                        () -> {
                            try {
                                java.net.URL url = new java.net.URL(UPDATE_JSON_URL);
                                java.net.HttpURLConnection conn =
                                        (java.net.HttpURLConnection) url.openConnection();
                                java.io.InputStream is = conn.getInputStream();
                                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                                String result = s.hasNext() ? s.next() : "";

                                org.json.JSONObject json = new org.json.JSONObject(result);
                                int latestVersionCode = json.getInt("versionCode");
                                String latestVersionName = json.optString("versionName", "Terbaru");
                                String releaseDate = json.optString("updateDate", "");
                                String downloadUrl = json.getString("downloadUrl");
                                String changelog = json.getString("changelog");

                                String fileSize = json.optString("fileSize", "--- MB");
                                String releaseType =
                                        json.optString("releaseType", "Official Release");
                                boolean isForceUpdate = json.optBoolean("isForceUpdate", false);

                                android.content.pm.PackageInfo pInfo =
                                        getPackageManager().getPackageInfo(getPackageName(), 0);
                                long currentVersionCode =
                                        androidx.core.content.pm.PackageInfoCompat
                                                .getLongVersionCode(pInfo);

                                if (latestVersionCode > currentVersionCode) {
                                    runOnUiThread(
                                            () ->
                                                    showUpdateDialog(
                                                            downloadUrl,
                                                            changelog,
                                                            latestVersionName,
                                                            releaseDate,
                                                            fileSize,
                                                            releaseType,
                                                            isForceUpdate));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    public void showUpdateDialog(
            String url,
            String notes,
            String versionName,
            String date,
            String size,
            String type,
            boolean isForce) {

        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_update, null);
        TextView tvChangelog = dialogView.findViewById(R.id.tv_changelog);
        TextView tvVersion = dialogView.findViewById(R.id.tv_version_name);

        tvChangelog.setText(notes);
        String fullInfo =
                "Versi " + versionName + " | " + size + " (" + type + ")\nDirilis: " + date;
        tvVersion.setText(fullInfo);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setView(dialogView)
                        .setCancelable(!isForce)
                        .setPositiveButton(
                                "Update Sekarang", (d, w) -> downloadAndInstall(url, versionName));

        if (!isForce) {
            builder.setNegativeButton("Nanti", null);
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void downloadAndInstall(String apkUrl, String verName) {
        com.a3mart.app.databinding.LayoutDownloadProgressBinding db =
                com.a3mart.app.databinding.LayoutDownloadProgressBinding.inflate(
                        getLayoutInflater());

        androidx.appcompat.app.AlertDialog progressDialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setView(db.getRoot())
                        .setCancelable(false)
                        .create();

        DialogUtils.terapkanEfekMewah(progressDialog);

        db.tvVersionInfo.setText("Update ke Versi: " + verName);
        db.tvStatus.setText("Menghubungkan...");
        db.tvSizeProgress.setText("0 MB / 0 MB");

        progressDialog.show();

        db.btnCancel.setOnClickListener(
                v -> {
                    if (downloadThread != null) downloadThread.interrupt();
                    progressDialog.dismiss();
                    smartToast("Unduhan dibatalkan");
                });

        downloadThread =
                new Thread(
                        () -> {
                            try {
                                java.net.URL url = new java.net.URL(apkUrl);
                                java.net.HttpURLConnection conn =
                                        (java.net.HttpURLConnection) url.openConnection();
                                conn.setInstanceFollowRedirects(true);

                                int status = conn.getResponseCode();
                                if (status == 301 || status == 302 || status == 303) {
                                    url = new java.net.URL(conn.getHeaderField("Location"));
                                    conn = (java.net.HttpURLConnection) url.openConnection();
                                }
                                conn.connect();

                                int fileLength = conn.getContentLength();
                                double totalMb = (double) fileLength / (1024 * 1024);
                                String totalMbStr =
                                        String.format(
                                                java.util.Locale.getDefault(), "%.2f MB", totalMb);

                                java.io.File apkFile =
                                        new java.io.File(getExternalCacheDir(), "update.apk");
                                if (apkFile.exists()) apkFile.delete();

                                try (java.io.InputStream input =
                                                new java.io.BufferedInputStream(
                                                        conn.getInputStream());
                                        java.io.OutputStream output =
                                                new java.io.FileOutputStream(apkFile)) {

                                    byte[] data = new byte[8192];
                                    long total = 0;
                                    int count;

                                    runOnUiThread(() -> db.tvStatus.setText("Mengunduh data..."));

                                    while ((count = input.read(data)) != -1) {
                                        if (Thread.currentThread().isInterrupted()) return;
                                        total += count;

                                        double currentMb = (double) total / (1024 * 1024);
                                        String currentMbStr =
                                                String.format(
                                                        java.util.Locale.getDefault(),
                                                        "%.2f MB",
                                                        currentMb);

                                        if (fileLength > 0) {
                                            int progress = (int) (total * 100 / fileLength);
                                            runOnUiThread(
                                                    () -> {
                                                        db.progressHorizontal.setProgress(progress);
                                                        db.tvSizeProgress.setText(
                                                                currentMbStr + " / " + totalMbStr);
                                                    });
                                        }
                                        output.write(data, 0, count);
                                    }
                                    output.flush();
                                }

                                runOnUiThread(
                                        () -> {
                                            progressDialog.dismiss();
                                            if (apkFile.exists() && apkFile.length() > 100000) {
                                                installApk(apkFile);
                                            } else {
                                                smartToast("File rusak atau tidak lengkap.");
                                            }
                                        });

                            } catch (Exception e) {
                                runOnUiThread(
                                        () -> {
                                            progressDialog.dismiss();
                                            smartToast("Gagal: " + e.getMessage());
                                        });
                            }
                        });
        downloadThread.start();
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

        try {
            startActivity(intent);
        } catch (Exception e) {
            smartToast("Gagal membuka installer: " + e.getMessage());
        }
    }

    private void smartToast(String pesan) {
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isToastEnabled = pref.getBoolean("show_toast", true);

        if (isToastEnabled) {
            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show();
        }
    }
}
