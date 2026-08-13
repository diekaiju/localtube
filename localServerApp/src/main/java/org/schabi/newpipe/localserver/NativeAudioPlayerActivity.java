package org.schabi.newpipe.localserver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NativeAudioPlayerActivity extends AppCompatActivity {

    private ImageView audioCover;
    private TextView audioTitle;
    private TextView audioArtist;
    private SeekBar audioSeekBar;
    private TextView audioTimeCurrent;
    private TextView audioTimeDuration;
    private ImageButton audioBtnPlay;
    private ImageButton audioBtnPrev;
    private ImageButton audioBtnNext;
    private ImageButton btnBack;

    private ServerService serverService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isTrackingSeekBar = false;

    private String streamUrl;
    private String title;
    private String artist;
    private String avatarUrl;
    private String mediaUrl;
    private int serviceId = 0;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            serverService = binder.getService();
            isBound = true;

            // Start playing the audio stream natively through the service
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                loadAudioStream(serviceId, mediaUrl);
            } else if (streamUrl != null) {
                serverService.playNativeAudio(streamUrl, title, artist);
            }
            updatePlayButtonState();
            startProgressUpdater();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serverService = null;
            isBound = false;
        }
    };

    private void loadAudioStream(int serviceId, String mediaUrl) {
        new Thread(() -> {
            try {
                org.schabi.newpipe.extractor.StreamingService service = 
                    org.schabi.newpipe.extractor.NewPipe.getService(serviceId);
                org.schabi.newpipe.extractor.stream.StreamInfo info = 
                    org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(service, mediaUrl);

                title = info.getName();
                artist = info.getUploaderName();
                
                if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
                    avatarUrl = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
                }

                // Extract audio stream URL
                String audioStreamUrl = "";
                if (info.getAudioStreams() != null && !info.getAudioStreams().isEmpty()) {
                    audioStreamUrl = info.getAudioStreams().get(0).getUrl();
                } else if (info.getVideoStreams() != null && !info.getVideoStreams().isEmpty()) {
                    audioStreamUrl = info.getVideoStreams().get(0).getUrl();
                }

                final String finalAudioUrl = audioStreamUrl;

                runOnUiThread(() -> {
                    audioTitle.setText(title);
                    audioArtist.setText(artist);
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        downloadCoverArt(avatarUrl);
                    }
                    if (isBound && serverService != null && finalAudioUrl != null && !finalAudioUrl.isEmpty()) {
                        serverService.playNativeAudio(finalAudioUrl, title, artist);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (isBound && serverService != null && !isTrackingSeekBar) {
                int currentPos = serverService.getAudioPosition();
                int duration = serverService.getAudioDuration();

                audioSeekBar.setMax(duration);
                audioSeekBar.setProgress(currentPos);

                audioTimeCurrent.setText(formatTime(currentPos));
                audioTimeDuration.setText(formatTime(duration));

                updatePlayButtonState();
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_audio_player);

        // Bind layout views
        audioCover = findViewById(R.id.audio_cover);
        audioTitle = findViewById(R.id.audio_title);
        audioArtist = findViewById(R.id.audio_artist);
        audioSeekBar = findViewById(R.id.audio_seekbar);
        audioTimeCurrent = findViewById(R.id.audio_time_current);
        audioTimeDuration = findViewById(R.id.audio_time_duration);
        audioBtnPlay = findViewById(R.id.audio_btn_play);
        audioBtnPrev = findViewById(R.id.audio_btn_prev);
        audioBtnNext = findViewById(R.id.audio_btn_next);
        btnBack = findViewById(R.id.btn_back);

        // Extract metadata from Intent
        Intent intent = getIntent();
        if (intent != null) {
            streamUrl = intent.getStringExtra("url");
            title = intent.getStringExtra("title");
            artist = intent.getStringExtra("artist");
            avatarUrl = intent.getStringExtra("avatar");
            mediaUrl = intent.getStringExtra("mediaUrl");
            serviceId = intent.getIntExtra("serviceId", 0);
        }

        if (title != null) audioTitle.setText(title);
        if (artist != null) audioArtist.setText(artist);

        // Download cover artwork asynchronously
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            downloadCoverArt(avatarUrl);
        }

        // Set up click listeners
        btnBack.setOnClickListener(v -> finish());

        audioBtnPlay.setOnClickListener(v -> {
            if (isBound && serverService != null) {
                if (serverService.isAudioPlaying()) {
                    serverService.pauseNativeAudio();
                } else {
                    serverService.resumeNativeAudio();
                }
                updatePlayButtonState();
            }
        });

        audioBtnPrev.setOnClickListener(v -> {
            if (isBound && serverService != null) {
                int currentPos = serverService.getAudioPosition();
                serverService.seekNativeAudio(Math.max(0, currentPos - 10000)); // Rewind 10s
            }
        });

        audioBtnNext.setOnClickListener(v -> {
            if (isBound && serverService != null) {
                int currentPos = serverService.getAudioPosition();
                int duration = serverService.getAudioDuration();
                serverService.seekNativeAudio(Math.min(duration, currentPos + 10000)); // Fast Forward 10s
            }
        });

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioTimeCurrent.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingSeekBar = false;
                if (isBound && serverService != null) {
                    serverService.seekNativeAudio(seekBar.getProgress());
                }
            }
        });

        // Bind to the Server Foreground Service
        Intent serviceIntent = new Intent(this, ServerService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private void updatePlayButtonState() {
        if (isBound && serverService != null && serverService.isAudioPlaying()) {
            audioBtnPlay.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            audioBtnPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void startProgressUpdater() {
        handler.post(progressUpdater);
    }

    private void stopProgressUpdater() {
        handler.removeCallbacks(progressUpdater);
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void downloadCoverArt(String urlStr) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream input = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                runOnUiThread(() -> audioCover.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        stopProgressUpdater();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        super.onDestroy();
    }
}
