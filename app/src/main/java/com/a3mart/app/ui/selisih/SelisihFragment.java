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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3mart.app.R;
import com.a3mart.app.databinding.DialogTambahDepositBinding;
import com.a3mart.app.databinding.DialogTambahHutangBinding;
import com.a3mart.app.databinding.FragmentSelisihBinding;
import com.a3mart.app.ui.transaksi.Transaksi;
import com.a3mart.app.ui.transaksi.TransaksiViewModel;
import com.a3mart.app.utils.DialogUtils;
import com.a3mart.app.utils.FormatterUtils;
import com.a3mart.app.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelisihFragment extends Fragment implements SelisihAdapter.OnRekapActionListener {
    private FragmentSelisihBinding binding;
    private SelisihAdapter adapter;
    private SelisihViewModel selisihViewModel;
    private TransaksiViewModel transaksiViewModel;
    private static final String DATABASE_NAME = "a3mart_database";

    private static final String ALGORITHM = "AES";
    private static final String KEY = "A3Mart_Secure_16";

    private javax.crypto.spec.SecretKeySpec generateKey() {
        byte[] keyBytes = KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new javax.crypto.spec.SecretKeySpec(keyBytes, ALGORITHM);
    }

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

        binding.chipGroupSelisih.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (checkedIds.isEmpty()) return;

                    int id = checkedIds.get(0);

                    List<Transaksi> listTransaksi =
                            transaksiViewModel.getTransaksiList().getValue();
                    if (listTransaksi == null) return;

                    List<Selisih> dataAsli = selisihViewModel.prosesRekapHutang(listTransaksi);

                    List<Selisih> filtered = new ArrayList<>();

                    if (id == binding.chipAll.getId()) {
                        filtered.addAll(dataAsli);
                    } else if (id == binding.chipDeposit.getId()) {
                        for (Selisih s : dataAsli) {
                            if (s.getTotalHarga() < 0) filtered.add(s);
                        }
                    } else if (id == binding.chipHutang.getId()) {
                        for (Selisih s : dataAsli) {
                            if (s.getTotalHarga() > 0) filtered.add(s);
                        }
                    }

                    adapter.updateData(filtered);
                });

        adapter.setOnLongClickListener(
                selisih -> {
                    bukaBottomSheetTambahHutang(selisih.getNamaKonsumen());
                });

        getParentFragmentManager()
                .setFragmentResultListener(
                        "request_key_setting",
                        getViewLifecycleOwner(),
                        (requestKey, bundle) -> {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        });
    }

    private void bukaBottomSheetTambahHutang(String nama) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        DialogTambahHutangBinding dialogBinding =
                DialogTambahHutangBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(dialogBinding.getRoot());

        dialogBinding.tvHeader.setText("Tambah Hutang " + nama);

        dialogBinding.btnSimpan.setOnClickListener(
                v -> {
                    String nominalStr = dialogBinding.etHargaHutang.getText().toString();
                    if (!nominalStr.isEmpty()) {
                        long nominal = Long.parseLong(nominalStr);

                        transaksiViewModel.tambahTransaksi(
                                "Hutang Tambahan", nama, 1, nominal, "Hutang");

                        bottomSheetDialog.dismiss();
                        smartToast("Hutang " + nama + " dicatat");
                    } else {
                        dialogBinding.etHargaHutang.setError("Masukkan nominal!");
                    }
                });

        bottomSheetDialog.show();
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setIcon(R.drawable.logo)
                        .setTitle("Konfirmasi Pelunasan")
                        .setMessage(
                                "Semua transaksi hutang atas nama "
                                        + selisih.getNamaKonsumen()
                                        + " akan ditandai sebagai LUNAS. Lanjutkan?")
                        .setPositiveButton(
                                "Ya, Lunasi",
                                (dialogInterface, which) -> {
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
                                "Batal", (dialogInterface, which) -> dialogInterface.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    @Override
    public void onPdfClick(Selisih selisih) {
        buatDanSimpanPdf(selisih, false);
    }

    @Override
    public void onShareClick(Selisih selisih) {
        shareTagihanViaTeks(selisih);
    }

    @Override
    public void onStrukClick(Selisih selisih) {
        buatStrukThermal(selisih);
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

    private void buatStrukThermal(Selisih selisih) {
        android.print.PrintManager printManager =
                (android.print.PrintManager)
                        requireContext().getSystemService(android.content.Context.PRINT_SERVICE);

        printManager.print(
                "Struk_A3Mart_" + selisih.getNamaKonsumen(),
                new android.print.PrintDocumentAdapter() {
                    @Override
                    public void onLayout(
                            android.print.PrintAttributes oldAttributes,
                            android.print.PrintAttributes newAttributes,
                            android.os.CancellationSignal cancellationSignal,
                            LayoutResultCallback callback,
                            Bundle extras) {
                        callback.onLayoutFinished(
                                new android.print.PrintDocumentInfo.Builder("struk.pdf")
                                        .setContentType(
                                                android.print.PrintDocumentInfo
                                                        .CONTENT_TYPE_DOCUMENT)
                                        .build(),
                                true);
                    }

                    @Override
                    public void onWrite(
                            android.print.PageRange[] pages,
                            android.os.ParcelFileDescriptor destination,
                            android.os.CancellationSignal cancellationSignal,
                            WriteResultCallback callback) {
                        android.graphics.pdf.PdfDocument document =
                                new android.graphics.pdf.PdfDocument();

                        android.content.SharedPreferences pref =
                                requireContext()
                                        .getSharedPreferences(
                                                "Settings", android.content.Context.MODE_PRIVATE);
                        String namaToko = pref.getString("nama_toko", "A3 Mart");
                        String alamatToko = pref.getString("alamat_toko", "Alamat belum diatur");
                        String telpToko = pref.getString("telepon_toko", "No.Telp belum diatur");

                        int lebarCanvas = 500;
                        int tinggiCanvas = 850 + (selisih.getListTransaksi().size() * 100);

                        android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                                new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                lebarCanvas, tinggiCanvas, 1)
                                        .create();
                        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
                        android.graphics.Canvas canvas = page.getCanvas();

                        android.graphics.Paint paint = new android.graphics.Paint();
                        paint.setAntiAlias(true);

                        int marginKiri = 40;
                        int posisiTeksX = 145;

                        try {
                            String logoPath = pref.getString("logo_path", null);
                            android.graphics.Bitmap b;
                            if (logoPath != null) {
                                b =
                                        android.graphics.BitmapFactory.decodeStream(
                                                requireContext()
                                                        .getContentResolver()
                                                        .openInputStream(
                                                                android.net.Uri.parse(logoPath)));
                            } else {
                                b =
                                        android.graphics.BitmapFactory.decodeResource(
                                                getResources(), R.drawable.logo);
                            }
                            if (b != null) {
                                int size = 85;
                                android.graphics.Bitmap scaled =
                                        android.graphics.Bitmap.createScaledBitmap(
                                                b, size, size, true);
                                canvas.drawBitmap(scaled, marginKiri, 40, paint);
                            }
                        } catch (Exception e) {
                        }

                        paint.setTextAlign(android.graphics.Paint.Align.LEFT);

                        paint.setTypeface(
                                android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
                                        android.graphics.Typeface.BOLD));
                        paint.setTextSize(26f);
                        canvas.drawText(namaToko.toUpperCase(), posisiTeksX, 70, paint);

                        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
                        paint.setTextSize(14f);
                        paint.setFakeBoldText(false);
                        paint.setColor(android.graphics.Color.DKGRAY);
                        canvas.drawText(alamatToko, posisiTeksX, 95, paint);
                        canvas.drawText("Telp: " + telpToko, posisiTeksX, 115, paint);

                        paint.setColor(android.graphics.Color.parseColor("#1976D2"));
                        paint.setStrokeWidth(3f);
                        canvas.drawLine(marginKiri, 145, lebarCanvas - marginKiri, 145, paint);

                        paint.setColor(android.graphics.Color.BLACK);
                        paint.setTextSize(16f);
                        canvas.drawText(
                                "PELANGGAN : " + selisih.getNamaKonsumen().toUpperCase(),
                                marginKiri,
                                185,
                                paint);
                        canvas.drawText(
                                "TANGGAL   : "
                                        + new java.text.SimpleDateFormat("dd/MM/yy HH:mm")
                                                .format(new java.util.Date()),
                                marginKiri,
                                210,
                                paint);
                        canvas.drawText(
                                "------------------------------------------",
                                marginKiri,
                                240,
                                paint);

                        int y = 280;
                        int marginKanan = lebarCanvas - 40;

                        for (com.a3mart.app.ui.transaksi.Transaksi t : selisih.getListTransaksi()) {
                            paint.setFakeBoldText(true);
                            canvas.drawText(t.getNamaProduk(), marginKiri, y, paint);

                            y += 30;
                            paint.setFakeBoldText(false);
                            canvas.drawText(t.getJumlah() + " x ", marginKiri, y, paint);

                            paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                            canvas.drawText(
                                    FormatterUtils.formatRupiah(t.getTotalHarga()),
                                    marginKanan,
                                    y,
                                    paint);

                            paint.setTextAlign(android.graphics.Paint.Align.LEFT);
                            y += 55;
                        }

                        y += 20;
                        canvas.drawText(
                                "------------------------------------------", marginKiri, y, paint);
                        y += 45;
                        paint.setFakeBoldText(true);
                        paint.setTextSize(20f);
                        long total = selisih.getTotalHarga();
                        canvas.drawText(
                                total > 0 ? "TOTAL HUTANG" : "SISA DEPOSIT", marginKiri, y, paint);

                        paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                        canvas.drawText(
                                FormatterUtils.formatRupiah(Math.abs(total)),
                                marginKanan,
                                y,
                                paint);

                        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                        paint.setFakeBoldText(false);
                        paint.setTextSize(16f);
                        y += 90;
                        canvas.drawText("TERIMA KASIH", lebarCanvas / 2, y, paint);
                        canvas.drawText("STRUK SAH A3 MART", lebarCanvas / 2, y + 25, paint);

                        document.finishPage(page);
                        try {
                            document.writeTo(
                                    new java.io.FileOutputStream(destination.getFileDescriptor()));
                        } catch (java.io.IOException e) {
                            callback.onWriteFailed(e.getMessage());
                        } finally {
                            document.close();
                        }
                        callback.onWriteFinished(
                                new android.print.PageRange[] {android.print.PageRange.ALL_PAGES});
                    }
                },
                null);
    }

    private void buatDanSimpanPdf(Selisih selisih, boolean isShare) {
        androidx.appcompat.app.AlertDialog progressDialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setView(R.layout.layout_progress)
                        .setCancelable(false)
                        .create();

        DialogUtils.terapkanEfekMewah(progressDialog);
        progressDialog.show();

        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(
                        () -> {
                            android.graphics.pdf.PdfDocument document =
                                    new android.graphics.pdf.PdfDocument();
                            try {
                                android.content.SharedPreferences pref =
                                        requireContext()
                                                .getSharedPreferences(
                                                        "Settings",
                                                        android.content.Context.MODE_PRIVATE);
                                String namaTokoProfil = pref.getString("nama_toko", "A3 Mart");
                                String alamatTokoProfil =
                                        pref.getString("alamat_toko", "Alamat belum diatur");
                                String telpTokoProfil =
                                        pref.getString("telepon_toko", "No.Telp belum diatur");
                                String logoPath = pref.getString("logo_path", null);

                                android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                                        new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                                        595, 842, 1)
                                                .create();
                                android.graphics.pdf.PdfDocument.Page page =
                                        document.startPage(pageInfo);

                                android.graphics.Canvas canvas = page.getCanvas();
                                android.graphics.Paint paint = new android.graphics.Paint();
                                android.graphics.Paint linePaint = new android.graphics.Paint();
                                paint.setAntiAlias(true);

                                try {
                                    android.graphics.Bitmap bitmap;
                                    if (logoPath != null && !logoPath.isEmpty()) {
                                        bitmap =
                                                android.graphics.BitmapFactory.decodeStream(
                                                        requireContext()
                                                                .getContentResolver()
                                                                .openInputStream(
                                                                        android.net.Uri.parse(
                                                                                logoPath)));
                                    } else {
                                        bitmap =
                                                android.graphics.BitmapFactory.decodeResource(
                                                        getResources(), R.drawable.logo);
                                    }
                                    if (bitmap != null) {
                                        float targetSize = 60f;
                                        android.graphics.RectF rect =
                                                new android.graphics.RectF(
                                                        50, 50, 50 + targetSize, 50 + targetSize);
                                        canvas.drawBitmap(bitmap, null, rect, paint);
                                    }
                                } catch (Exception e) {
                                }

                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setFakeBoldText(true);
                                paint.setTextSize(22f);
                                canvas.drawText(namaTokoProfil.toUpperCase(), 125, 75, paint);

                                paint.setTextSize(10f);
                                paint.setFakeBoldText(false);
                                paint.setColor(android.graphics.Color.DKGRAY);
                                canvas.drawText(alamatTokoProfil, 125, 92, paint);
                                canvas.drawText("Telp: " + telpTokoProfil, 125, 107, paint);

                                linePaint.setColor(android.graphics.Color.parseColor("#1976D2"));
                                linePaint.setStrokeWidth(3f);
                                canvas.drawLine(50, 125, 545, 125, linePaint);

                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setTextSize(12f);
                                paint.setFakeBoldText(true);
                                canvas.drawText("PENERIMA:", 50, 155, paint);
                                paint.setFakeBoldText(false);
                                canvas.drawText(
                                        selisih.getNamaKonsumen().toUpperCase(), 50, 175, paint);

                                paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                                String tgl =
                                        new java.text.SimpleDateFormat(
                                                        "dd/MM/yyyy", java.util.Locale.getDefault())
                                                .format(new java.util.Date());
                                canvas.drawText("Tanggal: " + tgl, 545, 155, paint);
                                paint.setTextAlign(android.graphics.Paint.Align.LEFT);

                                int yTable = 215;
                                paint.setColor(android.graphics.Color.parseColor("#EEEEEE"));
                                canvas.drawRect(50, yTable - 20, 545, yTable + 10, paint);
                                paint.setColor(android.graphics.Color.BLACK);
                                paint.setFakeBoldText(true);
                                canvas.drawText("Produk", 60, yTable, paint);
                                canvas.drawText("Qty", 350, yTable, paint);
                                canvas.drawText("Subtotal", 460, yTable, paint);

                                yTable += 35;
                                paint.setFakeBoldText(false);
                                for (com.a3mart.app.ui.transaksi.Transaksi t :
                                        selisih.getListTransaksi()) {
                                    canvas.drawText(t.getNamaProduk(), 60, yTable, paint);
                                    canvas.drawText(
                                            String.valueOf(t.getJumlah()), 350, yTable, paint);
                                    canvas.drawText(
                                            FormatterUtils.formatRupiah(t.getTotalHarga()),
                                            460,
                                            yTable,
                                            paint);
                                    yTable += 30;
                                }

                                yTable += 20;
                                paint.setFakeBoldText(true);
                                paint.setTextSize(14f);
                                long total = selisih.getTotalHarga();
                                canvas.drawText(
                                        total < 0 ? "SISA DEPOSIT" : "TOTAL HUTANG",
                                        300,
                                        yTable,
                                        paint);
                                paint.setColor(
                                        total < 0
                                                ? android.graphics.Color.parseColor("#2E7D32")
                                                : android.graphics.Color.RED);
                                canvas.drawText(
                                        FormatterUtils.formatRupiah(Math.abs(total)),
                                        460,
                                        yTable,
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

                                androidx.appcompat.app.AlertDialog successDialog =
                                        new com.google.android.material.dialog
                                                        .MaterialAlertDialogBuilder(
                                                        requireContext())
                                                .setIcon(R.drawable.ic_pdf)
                                                .setTitle("Invoice Berhasil Disimpan")
                                                .setMessage(
                                                        "File: "
                                                                + file.getName()
                                                                + "\nLokasi: Documents/A3Mart")
                                                .setPositiveButton(
                                                        "Lihat File",
                                                        (d, w) -> bukaFolderPenyimpanan())
                                                .setNegativeButton("Tutup", null)
                                                .create();

                                DialogUtils.terapkanEfekMewah(successDialog);
                                successDialog.show();

                            } catch (Exception e) {
                                if (document != null) document.close();
                                progressDialog.dismiss();
                                smartToast("Error: " + e.getMessage());
                            }
                        },
                        1000);
    }

    private void shareTagihanViaTeks(Selisih selisih) {
        android.content.SharedPreferences pref =
                requireContext()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        String namaToko = pref.getString("nama_toko", "A3 MART");

        StringBuilder sb = new StringBuilder();
        sb.append("      *").append(namaToko.toUpperCase()).append("*\n");
        sb.append("------------------------------------------\n");
        sb.append("*RINCIAN TAGIHAN*\n\n");
        sb.append("Kepada: *").append(selisih.getNamaKonsumen().toUpperCase()).append("*\n");
        sb.append("Tanggal: ")
                .append(
                        new java.text.SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                .format(new java.util.Date()))
                .append("\n\n");

        sb.append("*Daftar Transaksi:*\n");

        for (com.a3mart.app.ui.transaksi.Transaksi t : selisih.getListTransaksi()) {
            sb.append("- ")
                    .append(t.getNamaProduk())
                    .append(" (")
                    .append(t.getJumlah())
                    .append("x) ")
                    .append("= Rp ")
                    .append(FormatterUtils.formatRupiah(t.getTotalHarga()))
                    .append("\n");
        }

        long total = selisih.getTotalHarga();
        sb.append("\n------------------------------------------\n");
        if (total > 0) {
            sb.append("*TOTAL HUTANG: Rp ").append(FormatterUtils.formatRupiah(total)).append("*");
        } else {
            sb.append("*SISA DEPOSIT: Rp ")
                    .append(FormatterUtils.formatRupiah(Math.abs(total)))
                    .append("*");
        }
        sb.append("\n------------------------------------------\n");
        sb.append("_Mohon segera melakukan pelunasan._\n");
        sb.append("_Terima kasih telah berbelanja!_");

        android.content.Intent intent =
                new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());

        try {
            startActivity(android.content.Intent.createChooser(intent, "Kirim Tagihan via..."));
        } catch (Exception e) {
            smartToast("Gagal membagikan tagihan!");
        }
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
                smartToast("Tidak ada data transaksi untuk di-backup");
                return;
            }

            android.content.SharedPreferences sp =
                    requireActivity()
                            .getSharedPreferences(
                                    "A3Mart_Prefs", android.content.Context.MODE_PRIVATE);
            String jsonP = sp.getString("list_produk", null);

            List<com.a3mart.app.ui.produk.Produk> listP = new ArrayList<>();
            if (jsonP != null) {
                java.lang.reflect.Type typeP =
                        new com.google.gson.reflect.TypeToken<
                                ArrayList<com.a3mart.app.ui.produk.Produk>>() {}.getType();
                listP = new com.google.gson.Gson().fromJson(jsonP, typeP);
            }

            com.a3mart.app.utils.A3MartBackupModel paketLengkap =
                    new com.a3mart.app.utils.A3MartBackupModel(dataList, listP);

            String jsonString = new com.google.gson.Gson().toJson(paketLengkap);

            String encryptedJson = encrypt(jsonString);

            String hariIni =
                    new java.text.SimpleDateFormat("EEEE", new java.util.Locale("id", "ID"))
                            .format(new java.util.Date());

            File backupDir =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOCUMENTS),
                            "A3Mart/Backup");
            if (!backupDir.exists()) backupDir.mkdirs();

            File file = new File(backupDir, "A3Mart_MB_" + hariIni + ".bak");

            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write(encryptedJson);
            }

            smartToast("Backup Manual (" + hariIni + ") Berhasil!");

        } catch (Exception e) {
            smartToast("Gagal Backup: " + e.getMessage());
            android.util.Log.e("BACKUP_MANUAL", "Error: ", e);
        }
    }

    private void showBackupDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
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
                        .setNegativeButton("Batal", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void showRestoreDialog() {
        File backupDir =
                new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOCUMENTS),
                        "A3Mart/Backup");

        if (!backupDir.exists() || backupDir.listFiles() == null) {
            smartToast("Folder backup belum dibuat.");
            return;
        }

        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".bak"));
        if (files == null || files.length == 0) {
            smartToast("Tidak ditemukan file backup.");
            return;
        }

        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        View dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_restore_list, null);
        androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rvBackupList);

        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setView(dialogView)
                        .setPositiveButton(
                                "Cari File Lain",
                                (d, w) -> {
                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    intent.setType("*/*");
                                    restoreLauncher.launch(intent);
                                })
                        .setNegativeButton("Batal", null)
                        .create();

        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        rv.setAdapter(
                new androidx.recyclerview.widget.RecyclerView.Adapter<BackupViewHolder>() {
                    @NonNull
                    @Override
                    public BackupViewHolder onCreateViewHolder(
                            @NonNull ViewGroup parent, int viewType) {
                        View v =
                                LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.item_file_backup, parent, false);
                        return new BackupViewHolder(v);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull BackupViewHolder holder, int position) {
                        File file = files[position];
                        holder.tvName.setText(file.getName());

                        java.text.SimpleDateFormat sdfDate =
                                new java.text.SimpleDateFormat(
                                        "dd MMM yyyy", java.util.Locale.getDefault());
                        holder.tvInfo.setText(
                                sdfDate.format(new java.util.Date(file.lastModified())));

                        java.text.SimpleDateFormat sdfTime =
                                new java.text.SimpleDateFormat(
                                        "HH:mm", java.util.Locale.getDefault());
                        holder.tvTime.setText(
                                sdfTime.format(new java.util.Date(file.lastModified())));

                        holder.card.setOnClickListener(
                                v -> {
                                    dialog.dismiss();
                                    confirmRestoreExecution(file);
                                });
                    }

                    @Override
                    public int getItemCount() {
                        return files.length;
                    }
                });

        DialogUtils.terapkanEfekMewah(dialog);
        dialog.show();
    }

    static class BackupViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        android.widget.TextView tvName, tvInfo, tvTime;
        com.google.android.material.card.MaterialCardView card;

        BackupViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.cardItem);
            tvName = v.findViewById(R.id.tvFileName);
            tvInfo = v.findViewById(R.id.tvFileInfo);
            tvTime = v.findViewById(R.id.tvTimeInfo);
        }
    }

    private void confirmRestoreExecution(File file) {
        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Peringatan!")
                        .setIcon(R.drawable.ic_warning)
                        .setMessage(
                                "Anda akan memulihkan data dari: "
                                        + file.getName()
                                        + "\n\nData transaksi saat ini akan hilang. Lanjutkan?")
                        .setPositiveButton(
                                "Ya, Pulihkan",
                                (d, w) -> {
                                    prosesFileKeViewModel(file);
                                })
                        .setNegativeButton("Batal", null)
                        .create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void eksekusiRestoreData(String encryptedContent) {
        try {
            String decryptedJson = decrypt(encryptedContent.trim());

            if (decryptedJson != null && !decryptedJson.isEmpty()) {
                transaksiViewModel.importData(decryptedJson);

                smartToast("Restore Berhasil! Data transaksi & stok dipulihkan.");
            } else {
                smartToast("File kosong atau tidak valid.");
            }
        } catch (Exception e) {
            smartToast("Gagal: Dekripsi gagal atau format salah!");
            android.util.Log.e("RESTORE_ERROR", "Error: ", e);
        }
    }

    private void prosesFileKeViewModel(File file) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            java.util.Scanner s = new java.util.Scanner(fis).useDelimiter("\\A");
            String content = s.hasNext() ? s.next() : "";
            s.close();

            eksekusiRestoreData(content);
        } catch (Exception e) {
            smartToast("Gagal membaca file backup.");
        }
    }

    private void handleRestoreUri(android.net.Uri uri) {
        try {
            java.io.InputStream inputStream =
                    requireContext().getContentResolver().openInputStream(uri);
            java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            eksekusiRestoreData(content);
        } catch (Exception e) {
            smartToast("Gagal membaca file: " + e.getMessage());
        }
    }

    private String encrypt(String data) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, generateKey());
        byte[] encryptedBytes =
                cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.DEFAULT);
    }

    private String decrypt(String encryptedData) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, generateKey());

        byte[] decodedBytes =
                android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP);

        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
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
                            sBinding.etNamaManual.setText("");
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
