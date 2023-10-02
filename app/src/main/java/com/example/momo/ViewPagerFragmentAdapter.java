package com.example.momo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


public class ViewPagerFragmentAdapter extends FragmentStateAdapter {
    private String[] titles={"Songs","Albums"};


     public ViewPagerFragmentAdapter(@NonNull  FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0:
                return new SongsFragment();
            case 1:
                return new albumsFragment();
        }
        return new SongsFragment();
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}
