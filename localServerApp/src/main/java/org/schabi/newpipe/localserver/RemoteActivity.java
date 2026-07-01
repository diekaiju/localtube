package org.schabi.newpipe.localserver;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RemoteActivity extends AppCompatActivity {

    private View touchpadSurface;
    private Button remoteClick, remoteScrollUp, remoteScrollDown, remoteBack;
    private Button remoteRewind, remotePlayPause, remoteForward;
    private ImageButton btnBack;
    private TextView touchpadLabel;
    
    private float touchLastX, touchLastY;
    private boolean touchMoved;

    private final LocalHttpServer.LockStatusListener lockStatusListener = new LocalHttpServer.LockStatusListener() {
        @Override
        public void onLockStatusChanged() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateLockUi();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        btnBack = findViewById(R.id.btn_back);
        touchpadSurface = findViewById(R.id.touchpad_surface);
        remoteClick = findViewById(R.id.remote_click);
        remoteScrollUp = findViewById(R.id.remote_scroll_up);
        remoteScrollDown = findViewById(R.id.remote_scroll_down);
        remoteBack = findViewById(R.id.remote_back);
        remoteRewind = findViewById(R.id.remote_rewind);
        remotePlayPause = findViewById(R.id.remote_play_pause);
        remoteForward = findViewById(R.id.remote_forward);

        // Native back button on toolbar
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Setup Touchpad
        if (touchpadSurface != null) {
            touchpadSurface.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!LocalHttpServer.isLocked()) return false;
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            touchLastX = event.getX();
                            touchLastY = event.getY();
                            touchMoved = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getX() - touchLastX;
                            float dy = event.getY() - touchLastY;
                            float absDx = Math.abs(dx);
                            float absDy = Math.abs(dy);
                            if (absDx > 1f || absDy > 1f) {
                                float scale = 2.5f;
                                int sdx = Math.round(dx * scale);
                                int sdy = Math.round(dy * scale);
                                LocalHttpServer.addPendingCommand("pointer_move:" + sdx + "," + sdy);
                                touchMoved = true;
                            }
                            touchLastX = event.getX();
                            touchLastY = event.getY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!touchMoved) {
                                LocalHttpServer.addPendingCommand("pointer_click");
                            }
                            return true;
                    }
                    return false;
                }
            });
        }

        setupRemoteButton(remoteClick, "pointer_click");
        setupRemoteButton(remoteScrollUp, "pointer_scroll:-120");
        setupRemoteButton(remoteScrollDown, "pointer_scroll:120");
        setupRemoteButton(remoteBack, "back");
        setupRemoteButton(remoteRewind, "rewind");
        setupRemoteButton(remotePlayPause, "play_pause");
        setupRemoteButton(remoteForward, "forward");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalHttpServer.setLockStatusListener(lockStatusListener);
        updateLockUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalHttpServer.setLockStatusListener(null);
    }

    private void setupRemoteButton(Button btn, final String command) {
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (LocalHttpServer.isLocked()) {
                        LocalHttpServer.addPendingCommand(command);
                        Toast.makeText(RemoteActivity.this, "Sent: " + command, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void updateLockUi() {
        boolean locked = LocalHttpServer.isLocked();
        
        if (touchpadSurface != null) {
            touchpadSurface.setAlpha(locked ? 1.0f : 0.4f);
            touchpadSurface.setClickable(locked);
        }
        
        setButtonEnabled(remoteClick, locked);
        setButtonEnabled(remoteScrollUp, locked);
        setButtonEnabled(remoteScrollDown, locked);
        setButtonEnabled(remoteBack, locked);
        setButtonEnabled(remoteRewind, locked);
        setButtonEnabled(remotePlayPause, locked);
        setButtonEnabled(remoteForward, locked);

        if (!locked) {
            Toast.makeText(this, "Remote Disconnected: TV is currently idle", Toast.LENGTH_SHORT).show();
        }
    }

    private void setButtonEnabled(Button btn, boolean enabled) {
        if (btn != null) {
            btn.setEnabled(enabled);
            btn.setClickable(enabled);
        }
    }
}
