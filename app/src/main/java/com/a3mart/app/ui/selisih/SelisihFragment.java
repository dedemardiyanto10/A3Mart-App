package com.a3mart.app.ui.selisih;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3mart.app.R;
import com.a3mart.app.databinding.DialogTambahDepositBinding;
import com.a3mart.app.databinding.FragmentSelisihBinding;
import com.a3mart.app.ui.transaksi.Transaksi;
import com.a3mart.app.ui.transaksi.TransaksiViewModel;
import com.a3mart.app.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SelisihFragment extends Fragment implements SelisihAdapter.OnRekapActionListener {
    private FragmentSelisihBinding binding;
    private SelisihAdapter adapter;
    private SelisihViewModel selisihViewModel;
    private TransaksiViewModel transaksiViewModel;
    private static final String DATABASE_NAME = "a3mart_database";

    private final androidx.activity.result.ActivityResultLauncher<Intent> restoreLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts
                            .StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK
                                && result.getData() != null) {
                            handleRestoreUri(result.getData().getData());
                        }
                    });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSelisihBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        androidx.core.view.MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(
                new androidx.core.view.MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.selisih_menu, menu);
                    }

                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (id == R.id.action_backup) {
                            showBackupDialog();
                            return true;
                        } else if (id == R.id.action_restore) {
                            showRestoreDialog();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(),
                androidx.lifecycle.Lifecycle.State.RESUMED);

        adapter = new SelisihAdapter(new ArrayList<>(), this);
        binding.rvRekapHutang.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRekapHutang.setAdapter(adapter);

        selisihViewModel = new ViewModelProvider(this).get(SelisihViewModel.class);
        transaksiViewModel = new ViewModelProvider(requireActivity()).get(TransaksiViewModel.class);

        transaksiViewModel
                .getTransaksiList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            prosesDanTampilkan(list);
                        });

        binding.rvRekapHutang.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (dy > 0 && binding.fabAddDeposit.isShown()) {
                            binding.fabAddDeposit.hide();
                        } else if (dy < 0 && !binding.fabAddDeposit.isShown()) {
                            binding.fabAddDeposit.show();
                        }
                    }

                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!recyclerView.canScrollVertically(-1)
                                    || !recyclerView.canScrollVertically(1)) {
                                binding.fabAddDeposit.show();
                            }
                        }
                    }
                });

        binding.fabAddDeposit.setOnClickListener(v -> showBottomSheetDeposit());
    }

    private void prosesDanTampilkan(List<Transaksi> list) {
        if (list != null) {
            List<Selisih> rekapList = selisihViewModel.prosesRekapHutang(list);
            adapter.updateData(rekapList);

            if (rekapList.isEmpty()) {
                binding.layoutEmptyRekap.setVisibility(View.VISIBLE);
                binding.rvRekapHutang.setVisibility(View.GONE);
                binding.tvEmptyRekapMsg.setText("Semua Hutang Lunas!");
            } else {
                binding.layoutEmptyRekap.setVisibility(View.GONE);
                binding.rvRekapHutang.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onLunasi(Selisih selisih) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.logo)
                .setTitle("Konfirmasi Pelunasan")
                .setMessage(
                        "Semua transaksi hutang atas nama "
                                + selisih.getNamaKonsumen()
                                + " akan ditandai sebagai LUNAS. Lanjutkan?")
                .setPositiveButton(
                        "Ya, Lunasi",
                        (dialog, which) -> {
                            transaksiViewModel.lunasiSemuaHutang(selisih.getNamaKonsumen());

                            android.content.SharedPreferences pref =
                                    requireActivity()
                                            .getSharedPreferences(
                                                    "Settings",
                                                    android.content.Context.MODE_PRIVATE);
                            if (pref.getBoolean("notif_lunas", true)) {
                                NotificationHelper.kirimNotifikasiLunas(
                                        requireContext(),
                                        selisih.getNamaKonsumen(),
                                        selisih.getTotalHarga());
                            }

                            smartToast(
                                    "Status hutang "
                                            + selisih.getNamaKonsumen()
                                            + " berhasil diubah ke Lunas");
                        })
                .setNegativeButton(
                        "Batal",
                        (dialog, which) -> {
                            dialog.dismiss();
                        })
                .show();
    }

    @Override
    public void onPdfClick(Selisih selisih) {
        buatDanSimpanPdf(selisih, false);
    }

    @Override
    public void onShareClick(Selisih selisih) {
        buatDanSimpanPdf(selisih, true);
    }

    @Override
    public void onSimpanBayarSebagian(Selisih selisih, long nominalBayar) {
        if (nominalBayar >= selisih.getTotalHarga()) {
            transaksiViewModel.lunasiSemuaHutang(selisih.getNamaKonsumen());

            if (nominalBayar > selisih.getTotalHarga()) {
                long deposit = nominalBayar - selisih.getTotalHarga();
                transaksiViewModel.tambahTransaksi(
                        "Deposit", selisih.getNamaKonsumen(), 1, -deposit, "Hutang");
            }
            smartToast("Pembayaran melebihi hutang, status jadi Lunas");
        } else {
            transaksiViewModel.tambahTransaksi(
                    "Bayar Cicilan", selisih.getNamaKonsumen(), 1, -nominalBayar, "Hutang");
            smartToast("Cicilan Rp" + nominalBayar + " dicatat");
        }
    }

    private void buatDanSimpanPdf(Selisih selisih, boolean isShare) {

        androidx.appcompat.app.AlertDialog progressDialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setView(R.layout.layout_progress)
                        .setCancelable(false)
                        .create();

        progressDialog.show();

        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        () -> {
                            android.graphics.pdf.PdfDocument document =
                                    new android.graphics.pdf.PdfDocument();
                            try {
                                android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                                        new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                        595, 842, 1)
                                                .create();
                                android.graphics.pdf.PdfDocument.Page page =
                                        document.startPage(pageInfo);

                                android.graphics.Canvas canvas = page.getCanvas();
                                android.graphics.Paint paint = new android.graphics.Paint();
                                android.graphics.Paint linePaint = new android.graphics.Paint();

                                android.graphics.Bitmap bitmap =
                                        android.graphics.BitmapFactory.decodeResource(
                                                getResources(), R.drawable.logo);
                                android.graphics.Bitmap scaledLogo =
                                        android.graphics.Bitmap.createScaledBitmap(
                                                bitmap, 60, 60, false);
                                canvas.drawBitmap(scaledLogo, 50, 50, paint);

                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setFakeBoldText(true);
                                paint.setTextSize(22f);
                                canvas.drawText("A3 MART", 125, 75, paint);

                                paint.setTextSize(11f);
                                paint.setFakeBoldText(false);
                                paint.setColor(android.graphics.Color.GRAY);
                                canvas.drawText(
                                        "Laporan Saldo & Rincian Transaksi", 125, 95, paint);

                                linePaint.setColor(android.graphics.Color.parseColor("#1976D2"));
                                linePaint.setStrokeWidth(3f);
                                canvas.drawLine(50, 125, 545, 125, linePaint);

                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setTextSize(12f);
                                paint.setFakeBoldText(true);
                                canvas.drawText("PENERIMA:", 50, 150, paint);
                                paint.setFakeBoldText(false);
                                canvas.drawText(
                                        selisih.getNamaKonsumen().toUpperCase(), 50, 170, paint);

                                paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                                String tgl =
                                        new java.text.SimpleDateFormat(
                                                        "dd/MM/yyyy", java.util.Locale.getDefault())
                                                .format(new java.util.Date());
                                canvas.drawText("Tanggal: " + tgl, 545, 150, paint);
                                paint.setTextAlign(android.graphics.Paint.Align.LEFT);

                                int yTable = 210;
                                paint.setColor(android.graphics.Color.parseColor("#EEEEEE"));
                                canvas.drawRect(50, yTable - 20, 545, yTable + 10, paint);
                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setFakeBoldText(true);
                                canvas.drawText("Produk", 60, yTable, paint);
                                canvas.drawText("Qty", 350, yTable, paint);
                                canvas.drawText("Subtotal", 460, yTable, paint);

                                yTable += 35;
                                paint.setFakeBoldText(false);
                                java.text.NumberFormat nf =
                                        java.text.NumberFormat.getInstance(
                                                new java.util.Locale("in", "ID"));
                                for (Transaksi t : selisih.getListTransaksi()) {
                                    if (yTable > 750) break;
                                    canvas.drawText(t.getNamaProduk(), 60, yTable, paint);
                                    canvas.drawText(
                                            String.valueOf(t.getJumlah()), 350, yTable, paint);
                                    canvas.drawText(
                                            "Rp " + nf.format(t.getTotalHarga()),
                                            460,
                                            yTable,
                                            paint);

                                    linePaint.setStrokeWidth(0.5f);
                                    linePaint.setColor(android.graphics.Color.LTGRAY);
                                    canvas.drawLine(50, yTable + 10, 545, yTable + 10, linePaint);
                                    yTable += 30;
                                }

                                yTable += 20;
                                paint.setFakeBoldText(true);
                                paint.setTextSize(14f);
                                long total = selisih.getTotalHarga();
                                String label = (total < 0) ? "SISA DEPOSIT" : "TOTAL HUTANG";
                                canvas.drawText(label, 300, yTable, paint);
                                paint.setColor(
                                        total < 0
                                                ? android.graphics.Color.parseColor("#2E7D32")
                                                : android.graphics.Color.RED);
                                canvas.drawText(
                                        "Rp " + nf.format(Math.abs(total)), 460, yTable, paint);

                                paint.setColor(android.graphics.Color.GRAY);
                                paint.setTextSize(10f);
                                paint.setFakeBoldText(false);
                                canvas.drawText(
                                        "*Dokumen ini adalah bukti sah transaksi di A3 Mart.",
                                        50,
                                        800,
                                        paint);

                                document.finishPage(page);

                                java.io.File folder =
                                        new java.io.File(
                                                android.os.Environment
                                                        .getExternalStoragePublicDirectory(
                                                                android.os.Environment
                                                                        .DIRECTORY_DOCUMENTS),
                                                "A3Mart");
                                if (!folder.exists()) folder.mkdirs();

                                String fileName =
                                        "Invoice_"
                                                + selisih.getNamaKonsumen().replace(" ", "_")
                                                + ".pdf";
                                java.io.File file = new java.io.File(folder, fileName);

                                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                                document.writeTo(fos);
                                document.close();
                                fos.close();

                                progressDialog.dismiss();

                                if (isShare) {
                                    shareViaWhatsApp(file, selisih);
                                } else {
                                    smartToast("PDF Berhasil Disimpan!");
                                    new com.google.android.material.dialog
                                                    .MaterialAlertDialogBuilder(requireContext())
                                            .setIcon(R.drawable.ic_pdf)
                                            .setTitle("Berhasil Disimpan")
                                            .setMessage(
                                                    "File: "
                                                            + file.getName()
                                                            + "\nLokasi: Documents/A3Mart")
                                            .setPositiveButton(
                                                    "Lihat File",
                                                    (d, w) -> {
                                                        bukaFolderPenyimpanan();
                                                    })
                                            .setNegativeButton("Tutup", null)
                                            .show();
                                }

                            } catch (java.io.IOException e) {
                                document.close();
                                progressDialog.dismiss();
                                smartToast("Gagal simpan file: " + e.getMessage());
                            } catch (Exception e) {
                                progressDialog.dismiss();
                                smartToast("Error: " + e.getMessage());
                            }
                        },
                        1000);
    }

    private void bukaFolderPenyimpanan() {
        android.net.Uri uri =
                android.net.Uri.parse(
                        android.os.Environment.getExternalStoragePublicDirectory(
                                        android.os.Environment.DIRECTORY_DOCUMENTS)
                                + "/A3Mart/");

        android.content.Intent intent =
                new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(uri, "*/*");
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);

        try {
            startActivity(android.content.Intent.createChooser(intent, "Buka Folder A3 Mart"));
        } catch (android.content.ActivityNotFoundException e) {
            smartToast("File Manager tidak ditemukan.");
        }
    }

    private void shareViaWhatsApp(java.io.File file, Selisih selisih) {
        android.net.Uri uri =
                androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file);

        android.content.Intent intent =
                new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);

        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(android.content.Intent.createChooser(intent, "Kirim Rekap PDF"));
        } catch (Exception e) {
            smartToast("Gagal membagikan file!");
        }
    }

    private void backupDatabase() {
        try {
            List<Transaksi> dataList = transaksiViewModel.getTransaksiList().getValue();

            if (dataList == null || dataList.isEmpty()) {
                smartToast("Gagal: Tidak ada data untuk dibackup!");
                return;
            }

            com.google.gson.Gson gson = new com.google.gson.Gson();
            String jsonString = gson.toJson(dataList);

            File backupDir =
                    new File(
                            android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOCUMENTS),
                            "A3Mart/Backup");

            if (!backupDir.exists()) backupDir.mkdirs();

            String timeStamp =
                    new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                            .format(new java.util.Date());
            File file = new File(backupDir, "A3Mart_Backup_" + timeStamp + ".json");

            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(jsonString);
            writer.close();

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Backup Berhasil")
                    .setMessage("File disimpan sebagai:\n" + file.getName())
                    .setPositiveButton("Oke", null)
                    .show();

        } catch (Exception e) {
            smartToast("Gagal Backup: " + e.getMessage());
        }
    }

    private void showBackupDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_backup)
                .setTitle("Backup Data")
                .setMessage(
                        "Data transaksi akan disimpan ke folder:\n\nDocuments/A3Mart/Backup\n\nFile ini bisa digunakan untuk memulihkan data jika aplikasi terhapus. Lanjutkan?")
                .setPositiveButton(
                        "Backup Sekarang",
                        (d, w) -> {
                            backupDatabase();
                        })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void handleRestoreUri(android.net.Uri uri) {
        try {
            java.io.InputStream inputStream =
                    requireContext().getContentResolver().openInputStream(uri);
            java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Transaksi>>() {}.getType();
            List<Transaksi> dataBaru = gson.fromJson(content, type);

            if (dataBaru != null) {
                transaksiViewModel.importData(dataBaru);
                smartToast("Restore berhasil!");
            }
        } catch (Exception e) {
            smartToast("Gagal membaca file backup!");
        }
    }

    private void showRestoreDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_restore)
                .setTitle("Restore Data")
                .setMessage("Pilih file backup .json untuk mengembalikan data.")
                .setPositiveButton(
                        "Pilih File",
                        (d, w) -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/json");
                            restoreLauncher.launch(intent);
                        })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showBottomSheetDeposit() {
        DialogTambahDepositBinding sBinding =
                DialogTambahDepositBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        final long[] currentHutang = {0};

        List<String> listNama = new ArrayList<>();
        String[] konsumenTetap = transaksiViewModel.getDaftarKonsumenTetap();
        listNama.addAll(java.util.Arrays.asList(konsumenTetap));

        List<Transaksi> allTransaksi = transaksiViewModel.getTransaksiList().getValue();
        if (allTransaksi != null) {
            java.util.Set<String> setNamaUnik = new java.util.TreeSet<>();
            for (Transaksi t : allTransaksi) {
                String n = t.getNamaKonsumen();
                if (n != null && !listNama.contains(n)) {
                    setNamaUnik.add(n);
                }
            }
            listNama.addAll(setNamaUnik);
        }

        ArrayAdapter<String> adapterK =
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, listNama);
        sBinding.spinnerKonsumen.setAdapter(adapterK);

        Runnable updateSimulasiSaldo =
                () -> {
                    String inputStr = sBinding.etNominal.getText().toString().trim();
                    long nominalInput = inputStr.isEmpty() ? 0 : Long.parseLong(inputStr);

                    long sisa = currentHutang[0] - nominalInput;

                    java.text.NumberFormat nf =
                            java.text.NumberFormat.getInstance(new java.util.Locale("in", "ID"));

                    if (sisa > 0) {
                        sBinding.tvSisaAtauNabung.setText("Sisa Hutang: Rp " + nf.format(sisa));
                        sBinding.tvSisaAtauNabung.setTextColor(
                                android.graphics.Color.parseColor("#F44336"));
                    } else if (sisa < 0) {
                        sBinding.tvSisaAtauNabung.setText(
                                "Deposit Aktif: Rp " + nf.format(Math.abs(sisa)));
                        sBinding.tvSisaAtauNabung.setTextColor(
                                android.graphics.Color.parseColor("#FF9800"));
                    } else {
                        sBinding.tvSisaAtauNabung.setText("Lunas!");
                        sBinding.tvSisaAtauNabung.setTextColor(
                                android.graphics.Color.parseColor("#4CAF50"));
                    }
                };

        sBinding.spinnerKonsumen.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selected = listNama.get(position);

                        boolean ditemukan = false;
                        for (Selisih s : adapter.getDataList()) {
                            if (s.getNamaKonsumen().equalsIgnoreCase(selected)) {
                                currentHutang[0] = s.getTotalHarga();
                                ditemukan = true;
                                break;
                            }
                        }

                        if (selected.equalsIgnoreCase("Umum")) {
                            sBinding.tilNamaManual.setVisibility(View.VISIBLE);
                            sBinding.layoutInfoSaldo.setVisibility(View.GONE);
                            sBinding.etNamaManual.setText("Umum ");
                            sBinding.etNamaManual.setSelection(
                                    sBinding.etNamaManual.getText().length());
                            currentHutang[0] = 0;
                        } else {
                            sBinding.tilNamaManual.setVisibility(View.GONE);
                            sBinding.etNamaManual.setText("");

                            if (ditemukan) {
                                sBinding.layoutInfoSaldo.setVisibility(View.VISIBLE);
                                updateSimulasiSaldo.run();
                            } else {
                                sBinding.layoutInfoSaldo.setVisibility(View.GONE);
                                currentHutang[0] = 0;
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        sBinding.etNominal.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateSimulasiSaldo.run();
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });

        sBinding.btnSimpan.setOnClickListener(
                v -> {
                    String selected = sBinding.spinnerKonsumen.getSelectedItem().toString();
                    String namaFinal =
                            selected.equalsIgnoreCase("Umum")
                                    ? sBinding.etNamaManual.getText().toString().trim()
                                    : selected;

                    String nominalStr = sBinding.etNominal.getText().toString().trim();

                    if (namaFinal.isEmpty()) {
                        sBinding.tilNamaManual.setError("Nama tidak boleh kosong!");
                        return;
                    }

                    if (!nominalStr.isEmpty()) {
                        long nominal = Long.parseLong(nominalStr);

                        String keterangan = "Deposit";
                        if (currentHutang[0] > 0) {
                            keterangan = (nominal >= currentHutang[0]) ? "Pelunasan" : "Cicilan";
                        }

                        transaksiViewModel.tambahTransaksi(
                                keterangan, namaFinal, 1, -nominal, "Lunas");

                        NotificationHelper.kirimNotifikasiLunas(
                                requireContext(), namaFinal, nominal);

                        smartToast(keterangan + " " + namaFinal + " berhasil!");
                        dialog.dismiss();
                    } else {
                        sBinding.etNominal.setError("Isi nominal!");
                    }
                });

        dialog.show();
    }

    private void smartToast(String pesan) {
        if (getContext() == null) return;
        android.content.SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        boolean isToastEnabled = pref.getBoolean("show_toast", true);

        if (isToastEnabled) {
            android.widget.Toast.makeText(getContext(), pesan, android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
