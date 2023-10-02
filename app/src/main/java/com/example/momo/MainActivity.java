package com.example.momo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.security.Permission;
import java.util.ArrayList;

import static com.example.momo.R.id.by_name;

public class MainActivity extends AppCompatActivity implements androidx.appcompat.widget.SearchView.OnQueryTextListener {

    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    static boolean shuffleBoolean=false, repeatBoolean = false;
    static ArrayList<MusicFiles> albums= new ArrayList<>();
    private String MY_SORT_PREF="SortOrder";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
    }

    private void permission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            musicFiles = getAllAudio(this);
            initViewPager();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                musicFiles = getAllAudio(this);
                initViewPager();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    private void initViewPager() {
        ViewPager2 viewPager2 = findViewById(R.id.viewpager);
        TabLayout tablayout = findViewById(R.id.tab_layout);
        ViewPagerFragmentAdapter viewPagerFragmentAdapter= new ViewPagerFragmentAdapter(this);
        String[] titles={"Songs","Albums"};
        viewPager2.setAdapter(viewPagerFragmentAdapter);
        new TabLayoutMediator(tablayout,viewPager2,(((tab, position) -> tab.setText(titles[position])))).attach();

    }

    public ArrayList<MusicFiles> getAllAudio(Context context)
    {
        SharedPreferences preferences=getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting","sortByName");
        ArrayList<String> duplicate = new ArrayList<>();
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        String order= null;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        switch (sortOrder)
        {
            case "sortByName":
                { order = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
                break;
                }
            case "sortByDate":{
                order = MediaStore.MediaColumns.DATE_ADDED + " ASC";
                break;
            }
            case "sortBySize": {
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
            }

        }
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,//for path
                MediaStore.Audio.Media.ARTIST
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null,order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String album = cursor.getString(0);
                String title = cursor.getString(1);
                String duration = cursor.getString(2);
                String path = cursor.getString(3);
                String artist = cursor.getString(4);

                MusicFiles musicFiles = new MusicFiles(path, title, artist, album, duration );
                // Take log e for check
                Log.d("path: " + path, "album: " + album);
                tempAudioList.add(musicFiles);
                if(!(duplicate.contains(album)))
                {
                    albums.add(musicFiles);
                    duplicate.add(album);
                }

            }
            cursor.close();
        }
        return tempAudioList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem menuItem= menu.findItem(R.id.search_option);
        androidx.appcompat.widget.SearchView searchView= (androidx.appcompat.widget.SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

   @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

  @Override
    public boolean onQueryTextChange(String newText)
    {
       String userInput= newText.toLowerCase();
        ArrayList<MusicFiles> myFiles= new ArrayList<>();
        for(MusicFiles song:musicFiles)
        {
            if(song.getTitle().toLowerCase().contains(userInput))
            {
                myFiles.add(song);
            }
        }
        SongsFragment.musicAdapter.updateList(myFiles);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor= getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE).edit();
        switch (item.getItemId())
        {
            case R.id.by_name:
                editor.putString("sorting","sortByName");
                editor.apply();
                this.recreate();
                break;
            case R.id.by_date:
                editor.putString("sorting","sortByDate");
                editor.apply();
                this.recreate();
                break;
            case R.id.by_size:
                editor.putString("sorting","sortBySize");
                editor.apply();
                this.recreate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}