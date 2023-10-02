package com.example.momo;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.example.momo.MainActivity.albums;
import static com.example.momo.MainActivity.musicFiles;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link albumsFragment} factory method to
 * create an instance of this fragment.
 */
public class  albumsFragment extends Fragment {

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_albums, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        if (!(albums.size() < 1)) {
            albumAdapter = new AlbumAdapter(getContext(), albums);
            recyclerView.setAdapter(albumAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        return view;
    }
}