package com.a3mart.app.ui.transaksi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3mart.app.R;
import com.a3mart.app.databinding.DialogSettingsBinding;
import com.a3mart.app.databinding.DialogTambahTransaksiBinding;
import com.a3mart.app.databinding.FragmentTransaksiBinding;
import com.a3mart.app.ui.produk.Produk;
import com.a3mart.app.ui.produk.ProdukViewModel;
import com.a3mart.app.utils.NotificationHelper;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TransaksiFragment extends Fragment {
    private FragmentTransaksiBinding binding;
    private TransaksiViewModel viewModel;
    private TransaksiAdapter adapter;
    private int jumlahTemp = 1;
    private int lastListSize = 0;
    private int currentCheckedId = R.id.chip_all;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransaksiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(TransaksiViewModel.class);
        binding.rvTransaksi.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TransaksiAdapter(new ArrayList<>());
        binding.rvTransaksi.setAdapter(adapter);

        binding.rvTransaksi.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (dy > 0 && binding.fabAddTransaksi.isShown()) {
                            binding.fabAddTransaksi.hide();
                        } else if (dy < 0 && !binding.fabAddTransaksi.isShown()) {
                            binding.fabAddTransaksi.show();
                        }
                    }

                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!recyclerView.canScrollVertically(-1)
                                    || !recyclerView.canScrollVertically(1)) {
                                binding.fabAddTransaksi.show();
                            }
                        }
                    }
                });

        setupToolbarMenu();

        adapter.setOnItemLongClickListener((t, position) -> showEditTransaksiSheet(t));

        viewModel
                .getTransaksiList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list != null) {
                                hitungRingkasan(list);
                                applyFilter(list);

                                if (list.size() > lastListSize) {
                                    binding.rvTransaksi.post(
                                            () -> {
                                                if (list.size() > 0) {
                                                    binding.rvTransaksi.smoothScrollToPosition(0);
                                                }
                                            });
                                }

                                lastListSize = list.size();
                            }
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

        binding.fabAddTransaksi.setOnClickListener(
                v -> {
                    showTambahTransaksiSheet();
                });

        binding.cardSummary.setOnClickListener(
                v -> {
                    boolean isVisible = binding.barChart.getVisibility() == View.VISIBLE;

                    android.transition.TransitionSet set =
                            new android.transition.TransitionSet()
                                    .addTransition(new android.transition.ChangeBounds())
                                    .addTransition(new android.transition.Fade())
                                    .setDuration(400)
                                    .setInterpolator(
                                            new android.view.animation.DecelerateInterpolator());

                    android.transition.TransitionManager.beginDelayedTransition(
                            binding.cardSummary, set);

                    if (isVisible) {
                        binding.barChart.setVisibility(View.GONE);
                    } else {
                        binding.barChart.setVisibility(View.VISIBLE);
                        binding.barChart.animateY(
                                1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuart);
                    }
                });
    }

    private void hitungRingkasan(List<Transaksi> list) {
        if (list == null) return;

        long totalHutangAktif = 0;
        long totalDepositAktif = 0;
        long totalLunasSah = 0;

        HashMap<String, Long> mapHutangMurni = new HashMap<>();
        HashMap<String, Long> mapSaldoPotongDeposit = new HashMap<>();

        for (Transaksi t : list) {
            long nominal = t.getTotalHarga();
            String status = t.getStatus();
            String nama = t.getNamaKonsumen().trim().toLowerCase();

            if ((status.equalsIgnoreCase("Lunas") || status.equalsIgnoreCase("Lunas_Hutang"))
                    && nominal > 0) {
                totalLunasSah += nominal;
            }

            if (status.equalsIgnoreCase("Hutang")) {
                mapHutangMurni.put(nama, mapHutangMurni.getOrDefault(nama, 0L) + nominal);
            }

            if (status.equalsIgnoreCase("Hutang")
                    || status.equalsIgnoreCase("Lunas_Hutang")
                    || nominal < 0) {
                mapSaldoPotongDeposit.put(
                        nama, mapSaldoPotongDeposit.getOrDefault(nama, 0L) + nominal);
            }
        }

        for (long saldo : mapHutangMurni.values()) {
            if (saldo > 0) totalHutangAktif += saldo;
        }

        for (long saldo : mapSaldoPotongDeposit.values()) {
            if (saldo < 0) totalDepositAktif += saldo;
        }

        java.text.DecimalFormat df =
                new java.text.DecimalFormat(
                        "Rp#,##0.00;-Rp#,##0.00",
                        new java.text.DecimalFormatSymbols(new Locale("in", "ID")));

        binding.tvTotalLunas.setText(df.format(totalLunasSah));
        binding.tvTotalHutang.setText(df.format(totalHutangAktif));
        binding.tvTotalDeposit.setText(df.format(totalDepositAktif));

        int colorOnSurface =
                com.google.android.material.color.MaterialColors.getColor(
                        binding.barChart, com.google.android.material.R.attr.colorOnSurface);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) totalLunasSah));
        entries.add(new BarEntry(1f, (float) totalHutangAktif));
        entries.add(new BarEntry(2f, (float) Math.abs(totalDepositAktif)));

        BarDataSet dataSet = new BarDataSet(entries, "Ringkasan Kas");

        dataSet.setColors(
                new int[] {
                    Color.parseColor("#4CAF50"),
                    Color.parseColor("#F44336"),
                    Color.parseColor("#FF9800")
                });
        dataSet.setValueTextColor(colorOnSurface);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        binding.barChart.setData(barData);

        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setDrawBarShadow(false);

        com.github.mikephil.charting.components.XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(colorOnSurface);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(
                new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(
                        new String[] {"Lunas", "Hutang", "Deposit"}));

        binding.barChart.getAxisLeft().setTextColor(colorOnSurface);
        binding.barChart.getAxisLeft().setDrawGridLines(false);
        binding.barChart.getAxisRight().setEnabled(false);

        binding.barChart.animateY(1000);
        binding.barChart.invalidate();
    }

    private void applyFilter(List<Transaksi> fullList) {
        List<Transaksi> filtered = new ArrayList<>();
        long totalFilter = 0;

        // 1. Tentukan statusFilter di awal berdasarkan Chip yang aktif
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
        updateToolbarSubtitle(filtered.size(), totalFilter, statusFilter);
    }

    private void setupToolbarMenu() {
        androidx.core.view.MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(
                new androidx.core.view.MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.transaksi_menu, menu);
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
        String[] opsi = {
            "Hapus Semua", "Hapus Lunas Saja", "Hapus Hutang Saja", "Hapus Deposit Saja"
        };

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle("Pilih Mode Hapus")
                .setItems(
                        opsi,
                        (dialog, which) -> {
                            String tipe;
                            if (which == 0) tipe = "SEMUA";
                            else if (which == 1) tipe = "Lunas";
                            else if (which == 2) tipe = "Hutang";
                            else tipe = "Deposit";

                            tampilkanKonfirmasiHapus(
                                    tipe, "Data " + tipe + " akan dihapus permanen.");
                        })
                .show();
    }

    private void tampilkanKonfirmasiHapus(String tipe, String pesan) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle("Yakin Hapus?")
                .setMessage(pesan)
                .setPositiveButton(
                        "Hapus",
                        (d, w) -> {
                            if (tipe.equals("SEMUA")) viewModel.hapusSemuaTransaksi();
                            else viewModel.hapusTransaksiTerfilter(tipe);
                            smartToast("Berhasil dihapus!");
                        })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateToolbarSubtitle(int jumlah, long nominal, String tipe) {
        if (getActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar ab =
                    ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (ab != null) {
                java.text.DecimalFormatSymbols symbols =
                        new java.text.DecimalFormatSymbols(new Locale("in", "ID"));
                symbols.setCurrencySymbol("Rp");
                java.text.DecimalFormat df =
                        new java.text.DecimalFormat("Rp#,##0.00;-Rp#,##0.00", symbols);
                String rincian = tipe + ": " + jumlah + " | " + df.format(nominal);
                ab.setSubtitle(rincian);
            }
        }
    }

    private void showEditTransaksiSheet(Transaksi transaksi) {
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

                        pVM.kurangiStok(produkLama, -jumlahLama);
                        pVM.kurangiStok(p.getNama(), jumlahBaru);

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
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                    requireContext())
                            .setIcon(R.drawable.logo)
                            .setTitle("Hapus Transaksi")
                            .setMessage("Hapus transaksi ini dan kembalikan stok?")
                            .setPositiveButton(
                                    "Hapus",
                                    (d, w) -> {
                                        pVM.kurangiStok(
                                                transaksi.getNamaProduk(), -transaksi.getJumlah());
                                        viewModel.hapusTransaksi(transaksi);
                                        dialog.dismiss();
                                    })
                            .setNegativeButton("Batal", null)
                            .show();
                });

        dialog.show();
    }

    private void showTambahTransaksiSheet() {
        DialogTambahTransaksiBinding sBinding =
                DialogTambahTransaksiBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        ProdukViewModel pVM = new ViewModelProvider(requireActivity()).get(ProdukViewModel.class);

        jumlahTemp = 1;
        sBinding.tvJumlahBeli.setText("1");

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

        sBinding.spinnerKonsumen.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        String selected = listNama.get(position);

                        if (selected.equalsIgnoreCase("Umum")) {
                            sBinding.tilNamaManual.setVisibility(View.VISIBLE);

                            if (sBinding.etNamaManual.getText().toString().isEmpty()) {
                                sBinding.etNamaManual.setText("Umum ");
                                sBinding.etNamaManual.setSelection(
                                        sBinding.etNamaManual.getText().length());
                            }
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
                                sBinding.spinnerProduk.setAdapter(
                                        new ProdukSpinnerAdapter(requireContext(), list));
                            }
                        });

        sBinding.btnSimpanTransaksi.setOnClickListener(
                v -> {
                    Produk p = (Produk) sBinding.spinnerProduk.getSelectedItem();

                    if (p == null) return;

                    int stokAwal = p.getStok();
                    int jumlahBeli = jumlahTemp;
                    int sisaStokFinal = stokAwal - jumlahBeli;

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

                        SharedPreferences pref =
                                requireActivity()
                                        .getSharedPreferences(
                                                "Settings", android.content.Context.MODE_PRIVATE);

                        if (status.equalsIgnoreCase("Lunas")
                                && pref.getBoolean("notif_lunas", true)) {
                            NotificationHelper.kirimNotifikasiLunas(
                                    requireContext(), kFinal, totalHarga);
                        }

                        pVM.kurangiStok(p.getNama(), jumlahBeli);

                        if (pref.getBoolean("low_stock_alert", true) && sisaStokFinal <= 2) {
                            String pesan;
                            if (sisaStokFinal <= 0) {
                                pesan = "Stok " + p.getNama() + " sudah HABIS!";
                            } else {
                                pesan =
                                        "Stok "
                                                + p.getNama()
                                                + " sisa "
                                                + sisaStokFinal
                                                + ". Segera restok!";
                            }
                            NotificationHelper.kirimNotifikasiStokTipis(
                                    requireContext(), p.getNama(), pesan);
                        }

                        dialog.dismiss();
                        smartToast("Transaksi berhasil!");
                    } else {
                        smartToast("Stok tidak mencukupi! (Sisa: " + stokAwal + ")");
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
                                + (p.getStok() <= 0
                                        ? " (Habis/Kosong)"
                                        : " (Stok: " + p.getStok() + ")");
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
        DialogSettingsBinding dialogBinding = DialogSettingsBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(dialogBinding.getRoot());

        SharedPreferences pref =
                requireActivity()
                        .getSharedPreferences("Settings", android.content.Context.MODE_PRIVATE);

        int savedMode =
                pref.getInt(
                        "theme_mode",
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (savedMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            dialogBinding.rbMalam.setChecked(true);
        else if (savedMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            dialogBinding.rbSiang.setChecked(true);
        else dialogBinding.rbSistem.setChecked(true);

        dialogBinding.rgTheme.setOnCheckedChangeListener(
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

        dialogBinding.switchOntime.setChecked(pref.getBoolean("keep_screen_on", false));
        dialogBinding.switchOntime.setOnCheckedChangeListener(
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

        dialogBinding.switchLowStokAlert.setChecked(pref.getBoolean("low_stock_alert", true));
        dialogBinding.switchLowStokAlert.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("low_stock_alert", isChecked).apply();
                });

        dialogBinding.switchFingerprint.setChecked(pref.getBoolean("use_finger", false));
        dialogBinding.switchFingerprint.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("use_finger", isChecked).apply();
                    smartToast(isChecked ? "Keamanan Aktif" : "Keamanan Nonaktif");
                });

        dialogBinding.switchLunas.setChecked(pref.getBoolean("notif_lunas", true));
        dialogBinding.switchLunas.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("notif_lunas", isChecked).apply();
                });

        dialogBinding.switchToast.setChecked(pref.getBoolean("show_toast", true));
        dialogBinding.switchToast.setOnCheckedChangeListener(
                (v, isChecked) -> {
                    pref.edit().putBoolean("show_toast", isChecked).apply();
                });

        dialogBinding.btnInfoApp.setOnClickListener(
                v -> {
                    try {

                        android.content.pm.PackageInfo pInfo =
                                requireActivity()
                                        .getPackageManager()
                                        .getPackageInfo(requireActivity().getPackageName(), 0);

                        String version = pInfo.versionName;
                        long buildVersion = android.os.Build.VERSION.SDK_INT;
                        String device = android.os.Build.MODEL;

                        StringBuilder info = new StringBuilder();
                        info.append("**A3Mart - Kasir Pintar**\n");
                        info.append("Sistem manajemen toko simpel & cepat.\n\n");
                        info.append("━━━━━━━━━━━━━━━\n");
                        info.append("**Informasi Aplikasi**\n");
                        info.append("• Versi: ").append(version).append("\n");

                        boolean isDebug =
                                ((requireActivity().getApplicationInfo().flags
                                                & android.content.pm.ApplicationInfo
                                                        .FLAG_DEBUGGABLE)
                                        != 0);
                        String buildType = isDebug ? "DEBUG" : "RELEASE";

                        info.append("• Build: ").append(buildType).append("\n");

                        info.append("• Package: ")
                                .append(requireActivity().getPackageName())
                                .append("\n\n");

                        info.append("**Informasi Sistem**\n");
                        info.append("• Perangkat: ").append(device).append("\n");
                        info.append("• Android API: ").append(buildVersion).append("\n\n");

                        info.append("**Pengembang**\n");
                        info.append("A3Mart Team © 2026\n");
                        info.append("━━━━━━━━━━━━━━━");

                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                        requireContext())
                                .setIcon(R.drawable.logo)
                                .setTitle("Tentang Aplikasi")
                                .setMessage(info.toString())
                                .setPositiveButton("Tutup", null)
                                .setNeutralButton(
                                        "Salin Info",
                                        (d, w) -> {
                                            android.content.ClipboardManager clipboard =
                                                    (android.content.ClipboardManager)
                                                            requireContext()
                                                                    .getSystemService(
                                                                            android.content.Context
                                                                                    .CLIPBOARD_SERVICE);
                                            android.content.ClipData clip =
                                                    android.content.ClipData.newPlainText(
                                                            "App Info", info.toString());
                                            clipboard.setPrimaryClip(clip);
                                            smartToast("Info disalin ke clipboard");
                                        })
                                .show();

                    } catch (Exception e) {
                        smartToast("Gagal memuat informasi");
                    }
                });

        dialogBinding.btnResetApp.setOnClickListener(
                v -> {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                    requireContext())
                            .setTitle("Reset Aplikasi?")
                            .setIcon(R.drawable.ic_warning)
                            .setMessage(
                                    "Semua data transaksi, produk, dan pengaturan akan dihapus permanen. Aplikasi akan ditutup otomatis.")
                            .setPositiveButton(
                                    "RESET TOTAL",
                                    (d, w) -> {
                                        resetAplikasiTotal();
                                    })
                            .setNegativeButton("Batal", null)
                            .show();
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
            Toast.makeText(getContext(), pesan, Toast.LENGTH_SHORT).show();
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
