package com.a3mart.app.ui.transaksi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3mart.app.BuildConfig;
import com.a3mart.app.R;
import com.a3mart.app.ScannerActivity;
import com.a3mart.app.databinding.DialogSettingsBinding;
import com.a3mart.app.databinding.DialogTambahTransaksiBinding;
import com.a3mart.app.databinding.FragmentTransaksiBinding;
import com.a3mart.app.databinding.LayoutDialogHapusBinding;
import com.a3mart.app.databinding.LayoutDialogInfoBinding;
import com.a3mart.app.ui.produk.Produk;
import com.a3mart.app.ui.produk.ProdukViewModel;
import com.a3mart.app.utils.DialogUtils;
import com.a3mart.app.utils.FormatterUtils;
import com.a3mart.app.utils.NotificationHelper;
import com.a3mart.app.utils.SwipeActionHelper;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransaksiFragment extends Fragment {
    private FragmentTransaksiBinding binding;
    private TransaksiViewModel viewModel;
    private TransaksiAdapter adapter;
    private ProdukViewModel produkViewModel;
    private int jumlahTemp = 1;
    private int lastListSize = 0;
    private int currentCheckedId = R.id.chip_all;
    private boolean isFabOpen = false;
    private boolean isUpdateAvailable = false;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private ActivityResultLauncher<Intent> logoPickerLauncher;
    private DialogSettingsBinding currentSettingsBinding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransaksiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scannerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == android.app.Activity.RESULT_OK
                                    && result.getData() != null) {
                                String code = result.getData().getStringExtra("result");
                                if (code != null) onBarcodeScanned(code);
                            }
                        });

        logoPickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == android.app.Activity.RESULT_OK
                                    && result.getData() != null) {
                                android.net.Uri selectedImage = result.getData().getData();
                                if (selectedImage != null) {
                                    requireContext()
                                            .getContentResolver()
                                            .takePersistableUriPermission(
                                                    selectedImage,
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    SharedPreferences pref =
                                            requireActivity()
                                                    .getSharedPreferences(
                                                            "Settings", Context.MODE_PRIVATE);
                                    pref.edit()
                                            .putString("logo_path", selectedImage.toString())
                                            .apply();

                                    String savedLogo = pref.getString("logo_path", null);
                                    if (savedLogo != null) {
                                        com.bumptech.glide.Glide.with(requireContext())
                                                .load(android.net.Uri.parse(savedLogo))
                                                .centerCrop()
                                                .placeholder(R.drawable.logo)
                                                .error(R.drawable.logo)
                                                .into(currentSettingsBinding.ivPreviewLogo);
                                    }

                                    smartToast("Logo diperbarui!");
                                }
                            }
                        });

        produkViewModel = new ViewModelProvider(requireActivity()).get(ProdukViewModel.class);
        viewModel = new ViewModelProvider(requireActivity()).get(TransaksiViewModel.class);
        binding.rvTransaksi.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TransaksiAdapter(new ArrayList<>());
        binding.rvTransaksi.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback simpleCallback =
                new SwipeActionHelper() {
                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();

                        Transaksi item = adapter.getTransaksiAt(position);

                        if (direction == ItemTouchHelper.LEFT) {
                            tampilkanDialogHapus(item, position);
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            if (item.getStatus().equalsIgnoreCase("Hutang")) {
                                eksekusiLunasSwipe(item, position);
                            } else {
                                adapter.notifyItemChanged(position);
                                smartToast("Sudah lunas, Cok!");
                            }
                        }
                    }
                };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.rvTransaksi);

        setupToolbarMenu();

        adapter.setOnItemLongClickListener((t, position) -> showEditTransaksiSheet(t, position));

        viewModel
                .getTransaksiList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list != null) {
                                applyFilter(list);
                                adapter.notifyDataSetChanged();

                                if (list.size() > lastListSize) {
                                    binding.rvTransaksi.post(
                                            () -> {
                                                if (list.size() > 0)
                                                    binding.rvTransaksi.smoothScrollToPosition(0);
                                            });
                                }
                                lastListSize = list.size();
                            }
                        });

        viewModel
                .getTotalLunas()
                .observe(
                        getViewLifecycleOwner(),
                        total -> {
                            binding.tvTotalLunas.setText(FormatterUtils.formatRupiah(total));
                            updateChart();
                        });

        viewModel
                .getTotalHutang()
                .observe(
                        getViewLifecycleOwner(),
                        total -> {
                            binding.tvTotalHutang.setText(FormatterUtils.formatRupiah(total));
                            updateChart();
                        });

        viewModel
                .getTotalDeposit()
                .observe(
                        getViewLifecycleOwner(),
                        total -> {
                            binding.tvTotalDeposit.setText(FormatterUtils.formatRupiah(total));
                            updateChart();
                        });

        binding.chipGroupFilter.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {
                    if (!checkedIds.isEmpty()) {
                        currentCheckedId = checkedIds.get(0);
                    } else {
                        currentCheckedId = R.id.chip_all;
                    }

                    List<Transaksi> currentList = viewModel.getTransaksiList().getValue();
                    if (currentList != null) {
                        applyFilter(currentList);
                    }
                });

        binding.cardSummary.setOnClickListener(
                v -> {
                    boolean isVisible = binding.barChart.getVisibility() == View.VISIBLE;

                    android.transition.TransitionSet set =
                            new android.transition.TransitionSet()
                                    .addTransition(new android.transition.ChangeBounds())
                                    .addTransition(new android.transition.Fade())
                                    .setDuration(350)
                                    .setInterpolator(
                                            new android.view.animation.DecelerateInterpolator());

                    android.transition.TransitionManager.beginDelayedTransition(
                            (ViewGroup) binding.getRoot(), set);

                    if (isVisible) {
                        binding.barChart.setVisibility(View.GONE);
                    } else {
                        binding.barChart.setVisibility(View.VISIBLE);
                        binding.barChart.animateY(
                                1000, com.github.mikephil.charting.animation.Easing.EaseOutQuart);
                    }
                });

        setupSpeedDial();

        binding.rvTransaksi.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (isFabOpen) return;

                        if (dy > 0 && binding.fabMainTransaksi.isShown()) {
                            binding.fabMainTransaksi.hide();
                        } else if (dy < 0 && !binding.fabMainTransaksi.isShown()) {
                            binding.fabMainTransaksi.show();
                        }
                    }

                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        if (newState == RecyclerView.SCROLL_STATE_DRAGGING && isFabOpen) {
                            closeFabMenu();
                        }

                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!recyclerView.canScrollVertically(-1)
                                    || !recyclerView.canScrollVertically(1)) {
                                binding.fabMainTransaksi.show();
                            }
                        }
                    }
                });
    }

    private void eksekusiLunasSwipe(Transaksi transaksi, int position) {
        SharedPreferences pref =
                requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Konfirmasi Pelunasan")
                        .setIcon(R.drawable.ic_check)
                        .setMessage(
                                "Yakin ingin menandai transaksi "
                                        + transaksi.getNamaKonsumen()
                                        + " sebesar "
                                        + FormatterUtils.formatRupiah(transaksi.getTotalHarga())
                                        + " sebagai LUNAS?")
                        .setCancelable(false)
                        .setNegativeButton(
                                "Batal",
                                (d, which) -> {
                                    adapter.notifyItemChanged(position);
                                })
                        .setPositiveButton(
                                "Ya, Lunas",
                                (d, which) -> {
                                    viewModel.updateTransaksi(
                                            transaksi,
                                            transaksi.getNamaProduk(),
                                            transaksi.getNamaKonsumen(),
                                            transaksi.getJumlah(),
                                            transaksi.getTotalHarga(),
                                            "Lunas");

                                    if (pref.getBoolean("notif_lunas", true)) {
                                        NotificationHelper.kirimNotifikasiLunas(
                                                requireContext(),
                                                transaksi.getNamaKonsumen(),
                                                transaksi.getTotalHarga());
                                    }
                                    smartToast("Transaksi Lunas");
                                });

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void tampilkanDialogHapus(Transaksi transaksi, int position) {

        ProdukViewModel pVM = new ViewModelProvider(requireActivity()).get(ProdukViewModel.class);

        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hapus Transaksi")
                        .setIcon(R.drawable.ic_delete)
                        .setMessage(
                                "Yakin ingin menghapus transaksi "
                                        + transaksi.getNamaProduk()
                                        + " oleh "
                                        + transaksi.getNamaKonsumen()
                                        + "?")
                        .setCancelable(false)
                        .setNegativeButton(
                                "Batal",
                                (d, which) -> {
                                    adapter.notifyItemChanged(position);
                                })
                        .setPositiveButton(
                                "Hapus",
                                (d, which) -> {
                                    pVM.updateStok(
                                            transaksi.getNamaProduk(),
                                            transaksi.getJumlah(),
                                            false);

                                    viewModel.hapusTransaksi(transaksi);

                                    smartToast("Transaksi berhasil dihapus & stok dikembalikan!");
                                })
                        .create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void onBarcodeScanned(String barcode) {
        List<Produk> masterProduk = produkViewModel.getProdukList().getValue();
        Produk ditemukan = null;

        if (masterProduk != null) {
            for (Produk p : masterProduk) {
                if (barcode.equals(p.getBarcode())) {
                    ditemukan = p;
                    break;
                }
            }
        }

        if (ditemukan != null) {
            showTambahTransaksiSheet(ditemukan);
        } else {
            smartToast("Barcode tidak dikenal!");
        }
    }

    private void setupSpeedDial() {
        binding.fabMainTransaksi.setOnClickListener(
                v -> {
                    if (!isFabOpen) showFabMenu();
                    else closeFabMenu();
                });

        binding.fabTambahManual.setOnClickListener(
                v -> {
                    closeFabMenu();
                    showTambahTransaksiSheet(null);
                });

        binding.fabScanBarcode.setOnClickListener(
                v -> {
                    closeFabMenu();

                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    requireContext(), android.Manifest.permission.CAMERA)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {

                        scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class));
                    } else {
                        smartToast("Izin kamera belum diaktifkan. Silakan cek pengaturan HP.");
                    }
                });
    }

    private void showFabMenu() {
        isFabOpen = true;

        binding.fabTambahManual.show();
        binding.fabScanBarcode.show();
        binding.fabMainTransaksi.animate().rotation(45f).setDuration(200).start();
    }

    private void closeFabMenu() {
        isFabOpen = false;

        binding.fabTambahManual.hide();
        binding.fabScanBarcode.hide();

        binding.fabMainTransaksi.show();
        binding.fabMainTransaksi.animate().rotation(0f).setDuration(200).start();
    }

    private void updateChart() {
        if (binding.barChart == null) return;

        long lunas =
                viewModel.getTotalLunas().getValue() != null
                        ? viewModel.getTotalLunas().getValue()
                        : 0L;
        long hutangAktif =
                viewModel.getTotalHutang().getValue() != null
                        ? viewModel.getTotalHutang().getValue()
                        : 0L;

        long depositAktif =
                viewModel.getTotalDeposit().getValue() != null
                        ? Math.abs(viewModel.getTotalDeposit().getValue())
                        : 0L;

        int colorOnSurface =
                com.google.android.material.color.MaterialColors.getColor(
                        binding.barChart, com.google.android.material.R.attr.colorOnSurface);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) lunas));
        entries.add(new BarEntry(1f, (float) hutangAktif));
        entries.add(new BarEntry(2f, (float) depositAktif));

        BarDataSet dataSet = new BarDataSet(entries, "Ringkasan Kas");

        dataSet.setColors(
                new int[] {
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#F44336"),
                    Color.parseColor("#FF9800")
                });

        dataSet.setValueTextColor(colorOnSurface);
        dataSet.setValueTextSize(11f);

        dataSet.setValueFormatter(
                new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return FormatterUtils.formatRupiah((long) value);
                    }
                });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        com.github.mikephil.charting.components.XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(colorOnSurface);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(
                new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                        new String[] {"Lunas", "Hutang", "Deposit Aktif"}));

        binding.barChart.getAxisLeft().setTextColor(colorOnSurface);
        binding.barChart.getAxisLeft().setDrawGridLines(true);
        binding.barChart.getAxisRight().setEnabled(false);

        binding.barChart.setData(barData);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.setFitBars(true);
        binding.barChart.animateY(800);
        binding.barChart.invalidate();
    }

    private void applyFilter(List<Transaksi> fullList) {
        List<Transaksi> filtered = new ArrayList<>();
        long totalFilter = 0;
        String statusFilter;
        if (currentCheckedId == R.id.chip_deposit) {
            statusFilter = "Deposit";
        } else if (currentCheckedId == R.id.chip_lunas) {
            statusFilter = "Lunas";
        } else if (currentCheckedId == R.id.chip_hutang) {
            statusFilter = "Hutang";
        } else {
            statusFilter = "Semua";
        }

        for (Transaksi t : fullList) {
            boolean isDeposit = t.getTotalHarga() < 0;

            if (currentCheckedId == R.id.chip_deposit) {
                if (isDeposit) {
                    filtered.add(t);
                    totalFilter += t.getTotalHarga();
                }
            } else if (currentCheckedId == R.id.chip_lunas) {
                boolean isTerbayar =
                        t.getStatus().equalsIgnoreCase("Lunas")
                                || t.getStatus().equalsIgnoreCase("Lunas_Hutang");
                if (isTerbayar && t.getTotalHarga() >= 0) {
                    filtered.add(t);
                    totalFilter += t.getTotalHarga();
                }
            } else if (currentCheckedId == R.id.chip_hutang) {
                if (t.getStatus().equalsIgnoreCase("Hutang") && t.getTotalHarga() > 0) {
                    filtered.add(t);
                    totalFilter += t.getTotalHarga();
                }
            } else {
                filtered.add(t);
                totalFilter += t.getTotalHarga();
            }
        }

        if (filtered.isEmpty()) {
            binding.rvTransaksi.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);

            TextView tvEmptyMsg = binding.layoutEmpty.findViewById(R.id.tv_empty_msg);
            if (tvEmptyMsg != null) {
                tvEmptyMsg.setText("Tidak ada transaksi " + statusFilter);
            }
        } else {
            binding.rvTransaksi.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }

        adapter.updateData(filtered);
    }

    private void setupToolbarMenu() {
        androidx.core.view.MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(
                new androidx.core.view.MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.transaksi_menu, menu);

                        MenuItem searchItem = menu.findItem(R.id.action_search);
                        if (searchItem != null) {
                            androidx.appcompat.widget.SearchView searchView =
                                    (androidx.appcompat.widget.SearchView)
                                            searchItem.getActionView();

                            searchView.setQueryHint("Cari nama konsumen...");

                            searchView.setOnQueryTextListener(
                                    new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                                        @Override
                                        public boolean onQueryTextSubmit(String query) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onQueryTextChange(String newText) {
                                            if (adapter != null) {
                                                adapter.filter(newText);
                                            }
                                            return true;
                                        }
                                    });

                            searchItem.setOnActionExpandListener(
                                    new MenuItem.OnActionExpandListener() {
                                        @Override
                                        public boolean onMenuItemActionExpand(MenuItem item) {
                                            return true;
                                        }

                                        @Override
                                        public boolean onMenuItemActionCollapse(MenuItem item) {
                                            if (viewModel.getTransaksiList().getValue() != null) {
                                                applyFilter(
                                                        viewModel.getTransaksiList().getValue());
                                            }
                                            return true;
                                        }
                                    });
                        }
                    }

                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (id == R.id.action_delete_all) {
                            showDialogPilihanHapus();
                            return true;
                        } else if (id == R.id.action_settings) {
                            showSettingsDialog();
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(),
                androidx.lifecycle.Lifecycle.State.RESUMED);
    }

    private void showDialogPilihanHapus() {

        LayoutDialogHapusBinding dBinding = LayoutDialogHapusBinding.inflate(getLayoutInflater());

        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setView(dBinding.getRoot())
                        .setNegativeButton("Batal", null)
                        .create();

        DialogUtils.terapkanEfekMewah(dialog);

        dBinding.btnHapusSemua.setOnClickListener(
                v -> {
                    tampilkanKonfirmasiHapus(
                            "SEMUA", "Semua riwayat transaksi akan dihapus permanen.");
                    dialog.dismiss();
                });

        dBinding.btnHapusLunas.setOnClickListener(
                v -> {
                    tampilkanKonfirmasiHapus("Lunas", "Hanya data lunas yang akan dibersihkan.");
                    dialog.dismiss();
                });

        dBinding.btnHapusHutang.setOnClickListener(
                v -> {
                    tampilkanKonfirmasiHapus("Hutang", "Hanya data hutang yang akan dibersihkan.");
                    dialog.dismiss();
                });

        dBinding.btnHapusDeposit.setOnClickListener(
                v -> {
                    tampilkanKonfirmasiHapus(
                            "Deposit", "Hanya data deposit yang akan dibersihkan.");
                    dialog.dismiss();
                });

        dialog.show();
    }

    private void tampilkanKonfirmasiHapus(String tipe, String pesan) {
        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setIcon(R.drawable.ic_delete)
                        .setTitle("Yakin Hapus?")
                        .setMessage(pesan)
                        .setPositiveButton(
                                "Hapus",
                                (d, w) -> {
                                    if (tipe.equals("SEMUA")) {
                                        viewModel.hapusSemuaTransaksi();
                                    } else {
                                        viewModel.hapusTransaksiTerfilter(tipe);
                                    }
                                    smartToast("Berhasil dihapus!");
                                })
                        .setNegativeButton("Batal", null)
                        .create();

        DialogUtils.terapkanEfekMewah(dialog);

        dialog.show();
    }

    private void showEditTransaksiSheet(Transaksi transaksi, int position) {
        DialogTambahTransaksiBinding sBinding =
                DialogTambahTransaksiBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        ProdukViewModel pVM = new ViewModelProvider(requireActivity()).get(ProdukViewModel.class);

        sBinding.tvTitleSheet.setText("Edit Transaksi");
        sBinding.btnSimpanTransaksi.setText("Update");
        sBinding.btnHapusTransaksi.setVisibility(View.VISIBLE);

        jumlahTemp = transaksi.getJumlah();
        int jumlahLama = transaksi.getJumlah();
        String produkLama = transaksi.getNamaProduk();
        String namaKonsumenLama = transaksi.getNamaKonsumen();

        sBinding.tvJumlahBeli.setText(String.valueOf(jumlahTemp));
        boolean statusTerbayar =
                transaksi.getStatus().equalsIgnoreCase("Lunas")
                        || transaksi.getStatus().equalsIgnoreCase("Lunas_Hutang");
        sBinding.cbStatusBayar.setChecked(statusTerbayar);

        List<String> listNama = new ArrayList<>();
        String[] konsumenTetap = viewModel.getDaftarKonsumenTetap();
        listNama.addAll(java.util.Arrays.asList(konsumenTetap));

        List<Transaksi> allTransaksi = viewModel.getTransaksiList().getValue();
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

        ArrayAdapter<String> kAdapter =
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, listNama);
        sBinding.spinnerKonsumen.setAdapter(kAdapter);

        int posK = -1;
        for (int i = 0; i < listNama.size(); i++) {
            if (listNama.get(i).equals(namaKonsumenLama)) {
                posK = i;
                break;
            }
        }

        if (namaKonsumenLama.toLowerCase().startsWith("umum")) {
            for (int i = 0; i < listNama.size(); i++) {
                if (listNama.get(i).equalsIgnoreCase("Umum")) {
                    sBinding.spinnerKonsumen.setSelection(i);
                    sBinding.tilNamaManual.setVisibility(View.VISIBLE);
                    sBinding.etNamaManual.setText(namaKonsumenLama);
                    break;
                }
            }
        } else if (posK != -1) {
            sBinding.spinnerKonsumen.setSelection(posK);
            sBinding.tilNamaManual.setVisibility(View.GONE);
        } else {
            sBinding.spinnerKonsumen.setSelection(0);
        }

        sBinding.spinnerKonsumen.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selected = listNama.get(position);
                        if (selected.equalsIgnoreCase("Umum")) {
                            sBinding.tilNamaManual.setVisibility(View.VISIBLE);
                        } else {
                            sBinding.tilNamaManual.setVisibility(View.GONE);
                            sBinding.etNamaManual.setText("");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        pVM.getProdukList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list != null) {
                                ProdukSpinnerAdapter pAdapter =
                                        new ProdukSpinnerAdapter(requireContext(), list);
                                sBinding.spinnerProduk.setAdapter(pAdapter);
                                for (int i = 0; i < list.size(); i++) {
                                    if (list.get(i).getNama().equals(produkLama)) {
                                        sBinding.spinnerProduk.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        });

        sBinding.btnSimpanTransaksi.setOnClickListener(
                v -> {
                    Produk p = (Produk) sBinding.spinnerProduk.getSelectedItem();
                    if (p == null) return;

                    int stokDiGudang = p.getStok();
                    int jumlahBaru = jumlahTemp;

                    int sisaStokFinal;
                    if (p.getNama().equals(produkLama)) {
                        sisaStokFinal = (stokDiGudang + jumlahLama) - jumlahBaru;
                    } else {
                        sisaStokFinal = stokDiGudang - jumlahBaru;
                    }

                    String selected = sBinding.spinnerKonsumen.getSelectedItem().toString();
                    String kFinal =
                            selected.equalsIgnoreCase("Umum")
                                    ? sBinding.etNamaManual.getText().toString().trim()
                                    : selected;

                    if (kFinal.isEmpty()) {
                        sBinding.tilNamaManual.setError("Isi nama!");
                        return;
                    }

                    int kapasitasMaksimal =
                            p.getNama().equals(produkLama)
                                    ? (stokDiGudang + jumlahLama)
                                    : stokDiGudang;

                    if (jumlahBaru <= kapasitasMaksimal) {
                        SharedPreferences pref =
                                requireActivity()
                                        .getSharedPreferences(
                                                "Settings", android.content.Context.MODE_PRIVATE);

                        String statusBaru = sBinding.cbStatusBayar.isChecked() ? "Lunas" : "Hutang";
                        long totalBaru = (long) (p.getHarga() * jumlahBaru);

                        pVM.updateStok(produkLama, jumlahLama, false);
                        pVM.updateStok(p.getNama(), -jumlahBaru, false);

                        if (pref.getBoolean("low_stock_alert", true) && sisaStokFinal <= 2) {
                            String pesan =
                                    (sisaStokFinal <= 0)
                                            ? "Stok " + p.getNama() + " sudah HABIS!"
                                            : "Stok "
                                                    + p.getNama()
                                                    + " sisa "
                                                    + sisaStokFinal
                                                    + ". Segera restok!";
                            NotificationHelper.kirimNotifikasiStokTipis(
                                    requireContext(), p.getNama(), pesan);
                        }

                        viewModel.updateTransaksi(
                                transaksi, p.getNama(), kFinal, jumlahBaru, totalBaru, statusBaru);

                        if (statusBaru.equalsIgnoreCase("Lunas")
                                && pref.getBoolean("notif_lunas", true)) {
                            NotificationHelper.kirimNotifikasiLunas(
                                    requireContext(), kFinal, totalBaru);
                        }
                        dialog.dismiss();
                        smartToast("Berhasil diperbarui!");
                    } else {
                        smartToast("Stok tidak mencukupi!");
                    }
                });

        sBinding.btnTambah.setOnClickListener(
                v -> {
                    Produk p = (Produk) sBinding.spinnerProduk.getSelectedItem();
                    if (p != null) {
                        int stokMaksimal =
                                p.getStok() + (p.getNama().equals(produkLama) ? jumlahLama : 0);
                        if (jumlahTemp < stokMaksimal) {
                            jumlahTemp++;
                            sBinding.tvJumlahBeli.setText(String.valueOf(jumlahTemp));
                        } else {
                            smartToast("Stok tidak cukup!");
                        }
                    }
                });

        sBinding.btnKurang.setOnClickListener(
                v -> {
                    if (jumlahTemp > 1) {
                        jumlahTemp--;
                        sBinding.tvJumlahBeli.setText(String.valueOf(jumlahTemp));
                    }
                });

        sBinding.btnHapusTransaksi.setOnClickListener(
                v -> {
                    dialog.dismiss();
                    tampilkanDialogHapus(transaksi, position);
                });

        dialog.show();
    }

    private void showTambahTransaksiSheet(@Nullable Produk scanResult) {
        DialogTambahTransaksiBinding sBinding =
                DialogTambahTransaksiBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        jumlahTemp = 1;
        sBinding.tvJumlahBeli.setText("1");

        List<String> listNama = new ArrayList<>();
        listNama.addAll(java.util.Arrays.asList(viewModel.getDaftarKonsumenTetap()));

        List<Transaksi> allTransaksi = viewModel.getTransaksiList().getValue();
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

        ArrayAdapter<String> kAdapter =
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, listNama);
        sBinding.spinnerKonsumen.setAdapter(kAdapter);

        sBinding.spinnerKonsumen.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selected = listNama.get(position);
                        if (selected.equalsIgnoreCase("Umum")) {
                            sBinding.tilNamaManual.setVisibility(View.VISIBLE);
                        } else {
                            sBinding.tilNamaManual.setVisibility(View.GONE);
                            sBinding.etNamaManual.setText("");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

        produkViewModel
                .getProdukList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list != null) {
                                ProdukSpinnerAdapter pAdapter =
                                        new ProdukSpinnerAdapter(requireContext(), list);
                                sBinding.spinnerProduk.setAdapter(pAdapter);

                                if (scanResult != null) {
                                    for (int i = 0; i < list.size(); i++) {
                                        if (list.get(i).getNama().equals(scanResult.getNama())) {
                                            sBinding.spinnerProduk.setSelection(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        });

        sBinding.btnSimpanTransaksi.setOnClickListener(
                v -> {
                    Produk p = (Produk) sBinding.spinnerProduk.getSelectedItem();
                    if (p == null) return;

                    int stokAwal = p.getStok();
                    int jumlahBeli = jumlahTemp;
                    int sisaSetelahTransaksi = stokAwal - jumlahBeli;

                    String selected = sBinding.spinnerKonsumen.getSelectedItem().toString();
                    String kFinal =
                            selected.equalsIgnoreCase("Umum")
                                    ? sBinding.etNamaManual.getText().toString().trim()
                                    : selected;

                    if (kFinal.isEmpty()) {
                        sBinding.tilNamaManual.setError("Isi nama pembeli!");
                        return;
                    }

                    if (stokAwal >= jumlahBeli) {
                        String status = sBinding.cbStatusBayar.isChecked() ? "Lunas" : "Hutang";
                        long totalHarga = p.getHarga() * jumlahBeli;

                        viewModel.tambahTransaksi(
                                p.getNama(), kFinal, jumlahBeli, totalHarga, status);
                        produkViewModel.updateStok(p.getNama(), -jumlahBeli, false);

                        SharedPreferences pref =
                                requireActivity()
                                        .getSharedPreferences("Settings", Context.MODE_PRIVATE);

                        if (pref.getBoolean("low_stock_alert", true)) {
                            if (sisaSetelahTransaksi <= 0) {
                                NotificationHelper.kirimNotifikasiStokTipis(
                                        requireContext(),
                                        p.getNama(),
                                        "Stok " + p.getNama() + " sudah HABIS!");
                            } else if (sisaSetelahTransaksi <= 2) {
                                NotificationHelper.kirimNotifikasiStokTipis(
                                        requireContext(),
                                        p.getNama(),
                                        "Stok "
                                                + p.getNama()
                                                + " sisa "
                                                + sisaSetelahTransaksi
                                                + ". Segera restok!");
                            }
                        }

                        if (status.equalsIgnoreCase("Lunas")
                                && pref.getBoolean("notif_lunas", true)) {
                            NotificationHelper.kirimNotifikasiLunas(
                                    requireContext(), kFinal, totalHarga);
                        }

                        dialog.dismiss();
                        smartToast("Transaksi Berhasil!");
                    } else {
                        smartToast("Stok tidak mencukupi!");
                    }
                });

        sBinding.btnTambah.setOnClickListener(
                v -> {
                    Produk p = (Produk) sBinding.spinnerProduk.getSelectedItem();
                    if (p != null && jumlahTemp < p.getStok()) {
                        jumlahTemp++;
                        sBinding.tvJumlahBeli.setText(String.valueOf(jumlahTemp));
                    }
                });
        sBinding.btnKurang.setOnClickListener(
                v -> {
                    if (jumlahTemp > 1) {
                        jumlahTemp--;
                        sBinding.tvJumlahBeli.setText(String.valueOf(jumlahTemp));
                    }
                });

        dialog.show();
    }

    public class ProdukSpinnerAdapter extends ArrayAdapter<Produk> {
        public ProdukSpinnerAdapter(android.content.Context context, List<Produk> list) {
            super(context, android.R.layout.simple_spinner_dropdown_item, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            Produk p = getItem(position);
            if (p != null) {
                view.setText(p.getNama());
                view.setPadding(20, 20, 20, 20);
                view.setTextColor(
                        p.getStok() <= 0
                                ? android.graphics.Color.parseColor("#F44336")
                                : android.graphics.Color.parseColor("#4CAF50"));
            }
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            Produk p = getItem(position);
            if (p != null) {
                String label =
                        p.getNama()
                                + (p.getStok() <= 0 ? " (Habis)" : " (Stok: " + p.getStok() + ")");
                view.setText(label);
                view.setPadding(20, 20, 20, 20);
                if (p.getStok() <= 0) {
                    view.setTextColor(android.graphics.Color.parseColor("#F44336"));
                } else {
                    view.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                }
            }
            return view;
        }
    }

    private void showSettingsDialog() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        currentSettingsBinding = DialogSettingsBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(currentSettingsBinding.getRoot());

        SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);

        currentSettingsBinding.btnToggleProfil.setOnClickListener(
                v -> {
                    boolean isVisible =
                            currentSettingsBinding.layoutProfilContent.getVisibility()
                                    == View.VISIBLE;

                    android.transition.TransitionManager.beginDelayedTransition(
                            (ViewGroup) currentSettingsBinding.getRoot(),
                            new android.transition.AutoTransition());

                    if (isVisible) {
                        currentSettingsBinding.layoutProfilContent.setVisibility(View.GONE);
                        currentSettingsBinding.ivArrowProfil.animate().rotation(0).start();
                    } else {
                        currentSettingsBinding.layoutProfilContent.setVisibility(View.VISIBLE);
                        currentSettingsBinding.ivArrowProfil.animate().rotation(180).start();
                    }
                });

        String savedLogo = pref.getString("logo_path", null);
        if (savedLogo != null) {
            com.bumptech.glide.Glide.with(requireContext())
                    .load(android.net.Uri.parse(savedLogo))
                    .centerCrop()
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(currentSettingsBinding.ivPreviewLogo);
        }

        currentSettingsBinding.etNamaToko.setText(pref.getString("nama_toko", "A3 Mart"));
        currentSettingsBinding.etAlamatToko.setText(
                pref.getString("alamat_toko", "Bekasi, Indonesia"));
        currentSettingsBinding.etTeleponToko.setText(
                pref.getString("telepon_toko", "+6282333058981"));
                
        currentSettingsBinding.btnPilihLogo.setOnClickListener(
                v -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    logoPickerLauncher.launch(intent);
                });

        currentSettingsBinding.tvAppVersion.setText("Versi " + BuildConfig.VERSION_NAME);

        int savedMode =
                pref.getInt(
                        "theme_mode",
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (savedMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            currentSettingsBinding.rbMalam.setChecked(true);
        else if (savedMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            currentSettingsBinding.rbSiang.setChecked(true);
        else currentSettingsBinding.rbSistem.setChecked(true);

        currentSettingsBinding.rgTheme.setOnCheckedChangeListener(
                (group, checkedId) -> {
                    int mode;
                    if (checkedId == R.id.rb_malam)
                        mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
                    else if (checkedId == R.id.rb_siang)
                        mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
                    else mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

                    pref.edit().putInt("theme_mode", mode).apply();
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
                    bottomSheetDialog.dismiss();
                });

        currentSettingsBinding.switchOntime.setChecked(pref.getBoolean("keep_screen_on", false));
        currentSettingsBinding.switchOntime.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("keep_screen_on", isChecked).apply();

                    if (isChecked) {
                        requireActivity()
                                .getWindow()
                                .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        requireActivity()
                                .getWindow()
                                .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });

        currentSettingsBinding.switchTampilkanStruk.setChecked(
                pref.getBoolean("show_struk_btn", false));
        currentSettingsBinding.switchTampilkanStruk.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("show_struk_btn", isChecked).apply();
                    Bundle result = new Bundle();
                    result.putBoolean("status_struk", isChecked);
                    getParentFragmentManager().setFragmentResult("request_key_setting", result);

                    if (adapter != null) adapter.notifyDataSetChanged();
                });

        currentSettingsBinding.switchLowStokAlert.setChecked(
                pref.getBoolean("low_stock_alert", true));
        currentSettingsBinding.switchLowStokAlert.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("low_stock_alert", isChecked).apply();
                });

        currentSettingsBinding.switchFingerprint.setChecked(pref.getBoolean("use_finger", false));
        currentSettingsBinding.switchFingerprint.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("use_finger", isChecked).apply();
                    smartToast(isChecked ? "Keamanan Aktif" : "Keamanan Nonaktif");
                });

        currentSettingsBinding.switchAutoBackup.setChecked(
                pref.getBoolean("auto_backup_enabled", false));

        currentSettingsBinding.switchAutoBackup.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("auto_backup_enabled", isChecked).apply();

                    if (isChecked) {
                        smartToast("Auto Backup Aktif (Folder Documents/A3Mart)");
                        List<Transaksi> current = viewModel.getTransaksiList().getValue();
                        if (current != null) viewModel.importData(current);
                    } else {
                        smartToast("Auto Backup Nonaktif");
                    }
                });

        currentSettingsBinding.switchLunas.setChecked(pref.getBoolean("notif_lunas", true));
        currentSettingsBinding.switchLunas.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("notif_lunas", isChecked).apply();
                });

        currentSettingsBinding.switchToast.setChecked(pref.getBoolean("show_toast", true));
        currentSettingsBinding.switchToast.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("show_toast", isChecked).apply();
                });

        currentSettingsBinding.btnInfoApp.setOnClickListener(
                v -> {
                    try {
                        Context context = v.getContext();
                        LayoutDialogInfoBinding infoBinding =
                                LayoutDialogInfoBinding.inflate(LayoutInflater.from(context));

                        android.content.pm.PackageInfo pInfo =
                                context.getPackageManager()
                                        .getPackageInfo(context.getPackageName(), 0);

                        String githubUsername = "dedemardiyanto10";
                        String profileUrl = "https://github.com/" + githubUsername + ".png";

                        com.bumptech.glide.Glide.with(context)
                                .load(profileUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_github)
                                .error(R.drawable.ic_github)
                                .into(infoBinding.imgDevProfile);

                        infoBinding.tvBuildTime.setText("Build Time: " + BuildConfig.BUILD_TIME);

                        if (BuildConfig.DEBUG) {
                            infoBinding.tvBuildType.setText("DEBUG");
                            infoBinding.tvBuildType.setTextColor(android.graphics.Color.RED);
                        } else {
                            infoBinding.tvBuildType.setText("RELEASE");
                        }

                        infoBinding.infoVersion.setText("Versi: " + BuildConfig.VERSION_NAME);
                        infoBinding.infoPackage.setText(context.getPackageName());

                        infoBinding.infoDevice.setText(
                                android.os.Build.MODEL
                                        + " | "
                                        + "Android "
                                        + android.os.Build.VERSION.RELEASE);
                        infoBinding.infoApi.setText(
                                "API Level: " + android.os.Build.VERSION.SDK_INT);
                        String repoText = "GitHub Repository (" + BuildConfig.GIT_HASH + ")";
                        infoBinding.tvRepoHash.setText(repoText);

                        infoBinding.infoGithub.setText("github.com/dedemardiyanto10");

                        infoBinding.infoRepoContainer.setOnClickListener(
                                vRepo -> {
                                    String url = "https://github.com/dedemardiyanto10/A3Mart-App";
                                    context.startActivity(
                                            new Intent(
                                                    Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(url)));
                                });

                        infoBinding.infoGithubContainer.setOnClickListener(
                                vGit -> {
                                    String url = "https://github.com/dedemardiyanto10";
                                    context.startActivity(
                                            new Intent(
                                                    Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(url)));
                                });

                        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                                new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                                context)
                                        .setView(infoBinding.getRoot())
                                        .setPositiveButton("Tutup", null);

                        androidx.appcompat.app.AlertDialog dialog = builder.create();

                        DialogUtils.terapkanEfekMewah(dialog);

                        dialog.show();

                    } catch (Exception e) {
                        smartToast("Gagal memuat info aplikasi");
                    }
                });

        currentSettingsBinding.btnResetApp.setOnClickListener(
                v -> {
                    androidx.appcompat.app.AlertDialog dialog =
                            new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                            requireContext())
                                    .setTitle("Reset Aplikasi?")
                                    .setIcon(R.drawable.ic_warning)
                                    .setMessage(
                                            "Semua data transaksi, produk, dan pengaturan akan dihapus permanen.")
                                    .setPositiveButton(
                                            "LANJUTKAN",
                                            (d, w) -> {
                                                verifyBiometricBeforeReset();
                                            })
                                    .setNegativeButton("Batal", null)
                                    .create();

                    DialogUtils.terapkanEfekMewah(dialog);

                    dialog.show();
                });

        bottomSheetDialog.setOnDismissListener(
                dialog -> {
                    String nama = currentSettingsBinding.etNamaToko.getText().toString();
                    String alamat = currentSettingsBinding.etAlamatToko.getText().toString();
                    String telepon = currentSettingsBinding.etTeleponToko.getText().toString();
                    pref.edit()
                            .putString("nama_toko", nama)
                            .putString("alamat_toko", alamat)
                            .putString("telepon_toko", telepon)
                            .apply();

                    currentSettingsBinding = null;
                });

        bottomSheetDialog.show();
    }

    private void smartToast(String pesan) {
        if (getContext() == null) return;
        SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        boolean isToastEnabled = pref.getBoolean("show_toast", true);

        if (isToastEnabled) {
            android.widget.Toast.makeText(getContext(), pesan, Toast.LENGTH_SHORT).show();
        }
    }

    private void simpanPilihanTema(int mode) {
        android.content.SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        pref.edit().putInt("theme_mode", mode).apply();
    }

    private int muatPilihanTema() {
        android.content.SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);
        return pref.getInt(
                "theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void resetAplikasiTotal() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                android.app.ActivityManager am =
                        (android.app.ActivityManager)
                                requireContext()
                                        .getSystemService(android.content.Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.clearApplicationUserData();
                }
            } else {
                Runtime.getRuntime().exec("pm clear " + requireContext().getPackageName());
            }
        } catch (Exception e) {
            smartToast("Gagal reset otomatis: " + e.getMessage());
        }
    }

    private void verifyBiometricBeforeReset() {

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo =
                new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Otorisasi Super Admin")
                        .setSubtitle("Verifikasi diperlukan untuk menghapus seluruh data aplikasi")
                        .setAllowedAuthenticators(
                                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        | androidx.biometric.BiometricManager.Authenticators
                                                .DEVICE_CREDENTIAL)
                        .build();

        androidx.biometric.BiometricPrompt biometricPrompt =
                new androidx.biometric.BiometricPrompt(
                        this,
                        androidx.core.content.ContextCompat.getMainExecutor(requireContext()),
                        new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(
                                    @NonNull
                                            androidx.biometric.BiometricPrompt.AuthenticationResult
                                                    result) {
                                super.onAuthenticationSucceeded(result);
                                executeResetTotal();
                            }

                            @Override
                            public void onAuthenticationError(
                                    int errorCode, @NonNull CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                                android.util.Log.e("BIOMETRIC_ERROR", errString.toString());
                            }
                        });

        biometricPrompt.authenticate(promptInfo);
    }

    private void executeResetTotal() {
        try {
            Context context = requireContext().getApplicationContext();

            smartToast("Data berhasil dihapus. Silakan buka kembali aplikasi.");

            android.app.ActivityManager am =
                    (android.app.ActivityManager)
                            context.getSystemService(android.content.Context.ACTIVITY_SERVICE);

            if (am != null) {
                am.clearApplicationUserData();
            } else {
                System.exit(0);
            }

        } catch (Exception e) {
            android.util.Log.e("RESET_APP", "Gagal reset: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.getTransaksiList().getValue() != null) {
            applyFilter(viewModel.getTransaksiList().getValue());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
