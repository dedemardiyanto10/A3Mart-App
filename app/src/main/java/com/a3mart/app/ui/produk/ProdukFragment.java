package com.a3mart.app.ui.produk;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;
import com.a3mart.app.R;
import com.a3mart.app.databinding.FragmentProdukBinding;
import com.a3mart.app.databinding.DialogTambahProdukBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ProdukFragment extends Fragment {

    private FragmentProdukBinding binding;
    private ProdukViewModel viewModel;

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

        viewModel
                .getProdukList()
                .observe(
                        getViewLifecycleOwner(),
                        list -> {
                            if (list == null) return;

                            ProdukAdapter adapter = new ProdukAdapter(list);
                            adapter.setOnItemLongClickListener(this::showBottomSheet);
                            binding.rvProduk.setAdapter(adapter);
                        });

        binding.fabAddProduk.setOnClickListener(
                v -> {
                    showBottomSheet(null, -1);
                });

        binding.rvProduk.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (dy > 0 && binding.fabAddProduk.isShown()) {
                            binding.fabAddProduk.hide();
                        } else if (dy < 0 && !binding.fabAddProduk.isShown()) {
                            binding.fabAddProduk.show();
                        }
                    }

                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            if (!recyclerView.canScrollVertically(1)
                                    && !recyclerView.canScrollVertically(-1)) {
                                binding.fabAddProduk.show();
                            }
                        }
                    }
                });
    }

    private void showBottomSheet(@Nullable Produk produk, int position) {
        DialogTambahProdukBinding sBinding = DialogTambahProdukBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sBinding.getRoot());

        if (produk != null) {
            sBinding.tvJudulSheet.setText("Update Produk");
            sBinding.etNamaProduk.setText(produk.getNama());
            sBinding.etHargaProduk.setText(String.valueOf(produk.getHarga()));
            sBinding.etStokProduk.setText(String.valueOf(produk.getStok()));
            sBinding.btnHapus.setVisibility(View.VISIBLE);
            sBinding.btnSimpan.setText("Update");
        }

        sBinding.btnSimpan.setOnClickListener(
                v -> {
                    String nama = sBinding.etNamaProduk.getText().toString().trim();
                    String hrg = sBinding.etHargaProduk.getText().toString().trim();
                    String stk = sBinding.etStokProduk.getText().toString().trim();

                    if (nama.isEmpty() || hrg.isEmpty() || stk.isEmpty()) {
                        Toast.makeText(getContext(), "Lengkapi data!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (produk == null) {
                        viewModel.tambahProduk(nama, Integer.parseInt(hrg), Integer.parseInt(stk));
                    } else {
                        viewModel.updateProduk(
                                position, nama, Integer.parseInt(hrg), Integer.parseInt(stk));
                    }
                    dialog.dismiss();
                });

        sBinding.btnHapus.setOnClickListener(
                v -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Hapus Produk")
                            .setMessage("Yakin hapus " + produk.getNama() + "?")
                            .setNegativeButton("Batal", null)
                            .setPositiveButton(
                                    "Hapus",
                                    (d, i) -> {
                                        viewModel.hapusProduk(position);
                                        dialog.dismiss();
                                    })
                            .show();
                });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
