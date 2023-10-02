package com.example.momo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ComponentActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.palette.graphics.Palette;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaParser;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static com.example.momo.AlbumDetailsAdapter.albumFiles;
import static com.example.momo.ApplicationClass.ACTION_NEXT;
import static com.example.momo.ApplicationClass.ACTION_PLAY;
import static com.example.momo.ApplicationClass.ACTION_PREVIOUS;
import static com.example.momo.ApplicationClass.CHANNEL_ID_1;
import static com.example.momo.ApplicationClass.CHANNEL_ID_2;
import static com.example.momo.MainActivity.musicFiles;
import static com.example.momo.MainActivity.repeatBoolean;
import static com.example.momo.MainActivity.shuffleBoolean;
import static com.example.momo.MusicAdapter.mFiles;

public class PlayerActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    TextView song_name,artist_name,duration_played,duration_total;
    ImageView cover_art, nextBtn, prevBtn, backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playBtn;
    SeekBar seekBar;
    static Uri uri;
    int position = -1;
    //static MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    static ArrayList<MusicFiles> listSongs=new ArrayList<>();
    private Thread playThread,prevThread,nextThread;
    MusicService musicService;
    MediaSessionCompat mediaSessionCompat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setFullScreen();
        setContentView(R.layout.activity_player);
        //Objects.requireNonNull(getSupportActionBar()).hide();
        mediaSessionCompat = new MediaSessionCompat(getBaseContext(), "My Audio");
        initView();
        getIntentMethod();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(musicService!=null&&fromUser)
                {
                    musicService.seekTo(progress*1000);
                }
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService!=null)
                {
                    int mCurrentPosition= musicService.getCurrentPosition()/1000;
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        });
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shuffleBoolean)
                {
                    shuffleBoolean=false;
                    shuffleBtn.setImageResource(R.drawable.ic_round_shuffle_off);
                }
                else
                {
                    shuffleBoolean=true;
                    shuffleBtn.setImageResource(R.drawable.ic_baseline_shuffle);
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatBoolean)
                {
                    repeatBoolean=false;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat);
                }
                else
                {
                    repeatBoolean=true;
                    repeatBtn.setImageResource(R.drawable.ic_baseline_repeat_on);
                }

            }
        });

    }

    @Override
    protected void onResume() {

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void nextThreadBtn() {
        nextThread=new Thread()
        {
            @Override
            public void run() {
                super.run();
                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if(musicService.isPlaying())
        {
            musicService.stop();
            musicService.release();
            if(shuffleBoolean&& !repeatBoolean)
            {
                position=getRandom(listSongs.size()-1);
            }
            else if(!shuffleBoolean && !repeatBoolean)
            {
                position=((position+1)%listSongs.size());
            }
            //else position will not be changed
           // position=((position+1)%listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_baseline_pause_circle_filled_24);
            playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_circle_filled_24);
            musicService.start();
        }
        else
        {
            musicService.stop();
            musicService.release();
            if(shuffleBoolean&& !repeatBoolean)
            {
                position=getRandom(listSongs.size()-1);
            }
            else if(!shuffleBoolean && !repeatBoolean)
            {
                position=((position+1)%listSongs.size());
            }
            position=((position+1)%listSongs.size());
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_baseline_play);
            playBtn.setBackgroundResource(R.drawable.ic_baseline_play);
        }
    }

    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i+1);
    }

    private void playThreadBtn() {
        playThread=new Thread()
        {
            @Override
            public void run() {
                super.run();
                playBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });
            }
        };
        playThread.start();

    }

    public void playPauseBtnClicked() {
        if(musicService.isPlaying())
        {
            playBtn.setImageResource(R.drawable.ic_baseline_play);
            showNotification(R.drawable.ic_baseline_play);
            musicService.pause();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });

        }
        else
        {
            playBtn.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24);
            showNotification(R.drawable.ic_baseline_pause_circle_filled_24);
            musicService.start();
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private void prevThreadBtn() {
        prevThread=new Thread()
        {
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

     public void prevBtnClicked() {
        if(musicService.isPlaying())
        {
            musicService.stop();
            musicService.release();
            if(shuffleBoolean&& !repeatBoolean)
            {
                position=getRandom(listSongs.size()-1);
            }
            else if(!shuffleBoolean && !repeatBoolean) {
                position = ((position - 1) < 0 ? (listSongs.size() - 1) : (position - 1));
            }
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_baseline_pause_circle_filled_24);
            playBtn.setBackgroundResource(R.drawable.ic_baseline_pause_circle_filled_24);
            musicService.start();
        }
        else
        {
            musicService.stop();
            musicService.release();
            position=((position-1)<0 ? (listSongs.size()-1): (position-1));
            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());
            seekBar.setMax(musicService.getDuration()/1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService!=null)
                    {
                        int mCurrentPosition= musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();
            showNotification(R.drawable.ic_baseline_play);
            playBtn.setBackgroundResource(R.drawable.ic_baseline_play);
        }
    }

    private String formattedTime(int mCurrentPosition) {
        String totalOut= "";
        String totalNew="";
        String seconds = String.valueOf(mCurrentPosition%60);
        String minutes = String.valueOf(mCurrentPosition/60);
        totalOut= minutes+":"+seconds;
        totalNew=minutes+":"+"0"+seconds;
        if(seconds.length()==1)
        {
            return totalNew;
        }
        else
        {
              return totalOut;
        }



    }

    private void getIntentMethod() {
      position= getIntent().getIntExtra("position", -1);
      String sender= getIntent().getStringExtra("sender");
      if(sender!=null&& sender.equals("albumDetails"))
      {
          listSongs= albumFiles;
      }
      else {
          listSongs = mFiles;
      }
      if(listSongs!=null)
      {
          playBtn.setImageResource(R.drawable.ic_baseline_pause_circle_filled_24);
          uri = Uri.parse(listSongs.get(position).getPath());
      }
      showNotification(R.drawable.ic_baseline_pause_circle_filled_24);
      Intent intent = new Intent(this, MusicService.class);
      intent.putExtra("servicePosition", position);
      startService(intent);

    }

    private void initView() {
        song_name=findViewById(R.id.SongName);
        artist_name=findViewById(R.id.SongArtist);
        duration_played=findViewById(R.id.DurationPlayed);
        duration_total=findViewById(R.id.DurationTotal);
        cover_art=findViewById(R.id.cover_art);
        nextBtn=findViewById(R.id.id_next);
        prevBtn=findViewById(R.id.id_skip_previous);
        backBtn=findViewById(R.id.back_btn);
        shuffleBtn=findViewById(R.id.id_shuffle);
        repeatBtn=findViewById(R.id.id_repeat);
        playBtn=findViewById(R.id.play_pause);
        seekBar=findViewById(R.id.seek_bar);
    }

    private void metaData( Uri uri)
    {
        MediaMetadataRetriever retriever= new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        int durationTotal= Integer.parseInt(listSongs.get(position).getDuration())/1000;
        duration_total.setText(formattedTime(durationTotal));
        byte[] art=retriever.getEmbeddedPicture();
        Bitmap bitmap;
        if(art!=null)
        {
            bitmap= BitmapFactory.decodeByteArray(art,0,art.length);
            ImageAnimation(this, cover_art, bitmap);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable  Palette palette) {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if(swatch!=null)
                    {
                        ImageView gredient = findViewById(R.id.ImageViewGradient);
                        RelativeLayout mContainer= findViewById(R.id.mContainer);
                        gredient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable= new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{swatch.getRgb(), 0x00000000});
                        gredient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableRgb= new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{swatch.getRgb(), swatch.getRgb()});
                        mContainer.setBackground(gradientDrawableRgb);
                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                    }
                    else
                    {

                        ImageView gredient = findViewById(R.id.ImageViewGradient);
                        RelativeLayout mContainer= findViewById(R.id.mContainer);
                        gredient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);
                        GradientDrawable gradientDrawable= new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xff000000 , 0x00000000});
                        gredient.setBackground(gradientDrawable);
                        GradientDrawable gradientDrawableRgb= new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xff000000, 0xff000000});
                        mContainer.setBackground(gradientDrawableRgb);
                        song_name.setTextColor(Color.WHITE);
                        artist_name.setTextColor(Color.DKGRAY);
                    }
                }

            });
        }
        else
        {
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.download)
                    .into(cover_art);
            ImageView gredient = findViewById(R.id.ImageViewGradient);
            RelativeLayout mContainer= findViewById(R.id.mContainer);
            gredient.setBackgroundResource(R.drawable.gradient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);
            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
        }
    }
    public void ImageAnimation(Context context, ImageView imageview, Bitmap bitmap)
    {
        Animation animOut= AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        Animation animIn= AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(context).load(bitmap).into(imageview);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                imageview.startAnimation(animIn);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageview.startAnimation(animOut);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder= (MusicService.MyBinder) service;
        musicService = myBinder.getService();
        Toast.makeText(this,"Connected"+ musicService, Toast.LENGTH_SHORT).show();
        seekBar.setMax(musicService.getDuration()/1000);
        metaData(uri);
        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText((listSongs.get(position).getArtist()));
        musicService.OnCompleted();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

    void showNotification(int playPauseBtn)
    {

        Intent intent=new Intent(this, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        Intent prevIntent=new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent pauseIntent=new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent nextIntent=new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        byte[] picture=null;
        picture = getAlbumArt(musicFiles.get(position).getPath());
        Bitmap thumb = null;
        if( picture!=null)
        {
            thumb = BitmapFactory.decodeByteArray(picture,0,picture.length);
        }
        else
        {
            thumb = BitmapFactory.decodeResource(getResources(),R.drawable.download);
        }
        Notification notification= new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(playPauseBtn)
                .setLargeIcon(thumb)
                .setContentTitle(musicFiles.get(position).getTitle())
                .setContentText(musicFiles.get(position).getArtist())
                .addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", prevPending)
                .addAction(playPauseBtn,"Pause",pausePending)
                .addAction(R.drawable.ic_baseline_skip_next_24,"Next",nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .build();
        NotificationManager notificationManager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,notification);
        Log.d("notification", "showNotification: ");
    }

    private byte[] getAlbumArt(String uri) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(uri);
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();
            return art;
        } catch (Exception e) {
            return null;
        }
    }
    /*private void setFullScreen()
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }*/
}