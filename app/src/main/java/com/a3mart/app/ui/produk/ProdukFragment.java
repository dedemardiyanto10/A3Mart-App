package com.a3mart.app.ui.produk;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3mart.app.R;
import com.a3mart.app.ScannerActivity;
import com.a3mart.app.databinding.DialogTambahProdukBinding;
import com.a3mart.app.databinding.FragmentProdukBinding;
import com.a3mart.app.utils.DialogUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProdukFragment extends Fragment {

    private FragmentProdukBinding binding;
    private ProdukViewModel viewModel;
    private ProdukAdapter adapter;
    private boolean isFabOpen = false;
    private static final int SCANNER_REQUEST_CODE = 10;
    private int editingPosition = -1;
    private ActivityResultLauncher<Intent> scannerLauncher;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProdukBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProdukViewModel.class);
        binding.rvProduk.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProdukAdapter(new ArrayList<>());
        adapter.setOnItemLongClickListener(
                (produk, position) -> {
                    showBottomSheet(produk, position, null);
                });

        binding.rvProduk.setAdapter(adapter);

        adapter.setOnQuickEditListener(
                (produk, newStok) -> {
                    viewModel.updateStok(produk.getNama(), newStok, true);

                    if (newStok == 0) {
                        smartToast("Peringatan: Stok " + produk.getNama() + " habis!");
                    }
                });

        scannerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == android.app.Activity.RESULT_OK
                                    && result.getData() != null) {
                                String code = result.getData().getStringExtra("result");
                                if (code != null) {
                                    onBarcodeDetected(code);
                                }
                            }
                        });

        viewModel
                .getProdukList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list == null) return;

                            adapter.updateData(list);
                        });
        setupToolbarMenu();
        setupFabSpeedDial();

        binding.rvProduk.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {

                        if (isFabOpen) return;

                        if (dy > 0 && binding.fabMainProduk.isShown()) {
                            binding.fabMainProduk.hide();
                        } else if (dy < 0 && !binding.fabMainProduk.isShown()) {
                            binding.fabMainProduk.show();
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
                            if (!recyclerView.canScrollVertically(1)
                                    && !recyclerView.canScrollVertically(-1)) {
                                binding.fabMainProduk.show();
                            }
                        }
                    }
                });
    }

    private void setupToolbarMenu() {
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(
                new MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                        menuInflater.inflate(R.menu.produk_menu, menu);

                        MenuItem searchItem = menu.findItem(R.id.action_search);
                        if (searchItem != null) {
                            androidx.appcompat.widget.SearchView searchView =
                                    (androidx.appcompat.widget.SearchView)
                                            searchItem.getActionView();

                            searchView.setQueryHint("Cari nama atau barcode...");
                            searchView.setOnQueryTextListener(
                                    new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                                        @Override
                                        public boolean onQueryTextSubmit(String query) {
                                            return false;
                                        }

                                        @Override
                                        public boolean onQueryTextChange(String newText) {
                                            if (adapter != null) adapter.filter(newText);
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
                                            if (viewModel.getProdukList().getValue() != null) {
                                                adapter.updateData(
                                                        viewModel.getProdukList().getValue());
                                            }
                                            return true;
                                        }
                                    });
                        }
                    }

                    @Override
                    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                        return false;
                    }
                },
                getViewLifecycleOwner(),
                Lifecycle.State.RESUMED);
    }

    private void setupFabSpeedDial() {
        binding.fabMainProduk.setOnClickListener(
                v -> {
                    if (!isFabOpen) {
                        showFabMenu();
                    } else {
                        closeFabMenu();
                    }
                });

        binding.fabAddItem.setOnClickListener(
                v -> {
                    closeFabMenu();
                    showBottomSheet(null, -1, null);
                });

        binding.fabAddBarcode.setOnClickListener(
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

        binding.fabAddItem.show();
        binding.fabAddBarcode.show();

        binding.fabMainProduk.animate().rotation(45f).setDuration(200).start();
    }

    private void closeFabMenu() {
        isFabOpen = false;

        binding.fabAddItem.hide();
        binding.fabAddBarcode.hide();

        binding.fabMainProduk.show();
        binding.fabMainProduk.animate().rotation(0f).setDuration(200).start();
    }

    private void onBarcodeDetected(String code) {
        if (editingPosition != -1) {
            List<Produk> list = viewModel.getProdukList().getValue();
            if (list != null && editingPosition < list.size()) {
                Produk p = list.get(editingPosition);
                showBottomSheet(p, editingPosition, code);
            }
            return;
        }

        List<Produk> list = viewModel.getProdukList().getValue();
        int foundIndex = -1;
        Produk foundProduk = null;

        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (code.equals(list.get(i).getBarcode())) {
                    foundIndex = i;
                    foundProduk = list.get(i);
                    break;
                }
            }
        }

        if (foundProduk != null) {
            showBottomSheet(foundProduk, foundIndex, null);
        } else {
            showBottomSheet(null, -1, code);
        }
    }

    private void showBottomSheet(
            @Nullable Produk produk, int position, @Nullable String scanResult) {
        this.editingPosition = position;

        DialogTambahProdukBinding sBinding = DialogTambahProdukBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        sBinding.tilBarcodeProduk.setEndIconOnClickListener(
                v -> {
                    dialog.dismiss();

                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    requireContext(), android.Manifest.permission.CAMERA)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class));

                    } else {
                        smartToast("Izin kamera belum diaktifkan. Silakan cek pengaturan HP.");
                    }
                });

        if (produk != null) {
            sBinding.tvJudulSheet.setText("Update Produk");
            sBinding.btnHapus.setVisibility(View.VISIBLE);
            sBinding.btnSimpan.setText("Update");

            sBinding.etBarcodeProduk.setText(scanResult != null ? scanResult : produk.getBarcode());

            sBinding.etNamaProduk.setText(produk.getNama());
            sBinding.etHargaProduk.setText(String.valueOf(produk.getHarga()));
            sBinding.etStokProduk.setText(String.valueOf(produk.getStok()));
        } else {
            sBinding.tvJudulSheet.setText("Tambah Produk");
            sBinding.btnHapus.setVisibility(View.GONE);
            sBinding.btnSimpan.setText("Simpan");

            if (scanResult != null) {
                sBinding.etBarcodeProduk.setText(scanResult);
                sBinding.etNamaProduk.requestFocus();
            }
        }

        sBinding.btnSimpan.setOnClickListener(
                v -> {
                    String barcode = sBinding.etBarcodeProduk.getText().toString().trim();
                    String nama = sBinding.etNamaProduk.getText().toString().trim();
                    String hrgStr = sBinding.etHargaProduk.getText().toString().trim();
                    String stkStr = sBinding.etStokProduk.getText().toString().trim();

                    if (nama.isEmpty() || hrgStr.isEmpty() || stkStr.isEmpty()) {
                        smartToast("Lengkapi data wajib!");
                        return;
                    }

                    try {
                        int harga = Integer.parseInt(hrgStr);
                        int stok = Integer.parseInt(stkStr);

                        if (produk == null) {
                            if (!barcode.isEmpty() && isBarcodeSudahAda(barcode)) {
                                smartToast("Barcode sudah terdaftar!");
                                return;
                            }
                            viewModel.tambahProduk(barcode, nama, harga, stok);
                            smartToast("Produk berhasil ditambahkan!");
                        } else {
                            if (!barcode.isEmpty()
                                    && isBarcodeDigunakanProdukLain(barcode, position)) {
                                smartToast("Barcode sudah dipakai produk lain!");
                                return;
                            }
                            viewModel.updateProduk(position, barcode, nama, harga, stok);
                            smartToast("Produk diperbarui!");
                        }

                        editingPosition = -1;
                        dialog.dismiss();
                    } catch (NumberFormatException e) {

                    }
                });

        sBinding.btnHapus.setOnClickListener(
                v -> {
                    androidx.appcompat.app.AlertDialog confirmDialog =
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setIcon(R.drawable.ic_delete)
                                    .setTitle("Hapus Produk")
                                    .setMessage("Yakin ingin menghapus " + produk.getNama() + "?")
                                    .setNegativeButton("Batal", null)
                                    .setPositiveButton(
                                            "Hapus",
                                            (d, i) -> {
                                                viewModel.hapusProduk(position);
                                                editingPosition = -1;
                                                dialog.dismiss();
                                            })
                                    .create();

                    DialogUtils.terapkanEfekMewah(confirmDialog);

                    confirmDialog.show();
                });

        dialog.setOnCancelListener(d -> editingPosition = -1);

        dialog.show();
    }

    private boolean isBarcodeSudahAda(String barcode) {
        List<Produk> list = viewModel.getProdukList().getValue();
        if (list != null) {
            for (Produk p : list) {
                if (barcode.equals(p.getBarcode())) return true;
            }
        }
        return false;
    }

    private boolean isBarcodeDigunakanProdukLain(String barcode, int currentPos) {
        List<Produk> list = viewModel.getProdukList().getValue();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (i == currentPos) continue;
                if (barcode.equals(list.get(i).getBarcode())) return true;
            }
        }
        return false;
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
