package org.schabi.newpipe.localserver;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class LogActivity extends AppCompatActivity implements LogRepository.LogListener {

    private TextView textLogs;
    private ScrollView scrollLogs;
    private ImageButton btnBack;
    private ImageButton btnCopyLogs;
    private ImageButton btnClearLogs;

    private final LogRepository logRepository = LogRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        textLogs = findViewById(R.id.text_logs);
        scrollLogs = findViewById(R.id.scroll_logs);
        btnBack = findViewById(R.id.btn_back);
        btnCopyLogs = findViewById(R.id.btn_copy_logs);
        btnClearLogs = findViewById(R.id.btn_clear_logs);

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        if (btnCopyLogs != null) {
            btnCopyLogs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyLogsToClipboard();
                }
            });
        }

        if (btnClearLogs != null) {
            btnClearLogs.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logRepository.clear();
                    Toast.makeText(LogActivity.this, "Console logs cleared", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logRepository.setActiveListener(this);
        reloadLogs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        logRepository.setActiveListener(null);
    }

    private void reloadLogs() {
        List<String> logs = logRepository.getLogs();
        StringBuilder sb = new StringBuilder();
        for (String line : logs) {
            sb.append(line);
        }
        displayHtmlLogs(sb.toString());
    }

    private void displayHtmlLogs(String html) {
        if (textLogs == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textLogs.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            textLogs.setText(Html.fromHtml(html));
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (scrollLogs != null) {
            scrollLogs.post(new Runnable() {
                @Override
                public void run() {
                    scrollLogs.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

    private void copyLogsToClipboard() {
        if (textLogs == null) return;
        String rawLogs = textLogs.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("LocalTube Server Logs", rawLogs);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Logs copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLogAdded(final String formattedLine) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reloadLogs();
            }
        });
    }

    @Override
    public void onLogsCleared() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textLogs != null) {
                    textLogs.setText("Logs cleared.\n");
                }
            }
        });
    }
}
