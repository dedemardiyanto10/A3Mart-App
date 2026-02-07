package com.a3mart.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.a3mart.app.ui.produk.ProdukFragment;
import com.a3mart.app.ui.transaksi.TransaksiFragment;
import com.a3mart.app.ui.selisih.SelisihFragment;

public class MainPagerAdapter extends FragmentStateAdapter {
    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new TransaksiFragment();
            case 1:
                return new ProdukFragment();
            case 2:
                return new SelisihFragment();
            default:
                return new TransaksiFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
