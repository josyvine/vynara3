package com.example.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.R;
import com.example.utils.VynaraLogger;

import java.util.List;

public class InAppFloatingConsoleView extends FrameLayout implements VynaraLogger.LogListener {

    private View minimizedView;
    private View expandedView;
    private ScrollView scrollView;
    private TextView tvLogOutput;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragEnabled = true;

    public InAppFloatingConsoleView(@NonNull Context context) {
        super(context);
        init();
    }

    public InAppFloatingConsoleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InAppFloatingConsoleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Inflate layout contents directly into this custom FrameLayout container
        LayoutInflater.from(getContext()).inflate(R.layout.layout_inapp_floating_console, this, true);

        minimizedView = findViewById(R.id.layout_console_minimized);
        expandedView = findViewById(R.id.layout_console_expanded);
        scrollView = findViewById(R.id.scroll_console_output);
        tvLogOutput = findViewById(R.id.tv_console_output);

        // Bind interactive minimize / expand state toggles
        if (minimizedView != null) {
            minimizedView.setOnClickListener(v -> setExpandedState(true));
            setupDragGesture(minimizedView);
        }

        if (expandedView != null) {
            View btnMinimize = findViewById(R.id.btn_console_minimize);
            if (btnMinimize != null) {
                btnMinimize.setOnClickListener(v -> setExpandedState(false));
            }

            View btnClear = findViewById(R.id.btn_console_clear);
            if (btnClear != null) {
                btnClear.setOnClickListener(v -> VynaraLogger.clear());
            }

            // Safe dynamic lookup for copy button so missing XML ID never breaks compilation
            int copyResId = getResources().getIdentifier("btn_console_copy", "id", getContext().getPackageName());
            View btnCopy = (copyResId != 0) ? findViewById(copyResId) : null;
            if (btnCopy != null) {
                btnCopy.setOnClickListener(v -> copyLogsToClipboard());
            }

            // Long-press log output area to copy all logs to clipboard
            if (tvLogOutput != null) {
                tvLogOutput.setOnLongClickListener(v -> {
                    copyLogsToClipboard();
                    return true;
                });
            }

            View dragHeader = findViewById(R.id.layout_console_header);
            if (dragHeader != null) {
                setupDragGesture(dragHeader);
            }
        }

        // Initialize view state as minimized on app startup
        setExpandedState(false);
    }

    /**
     * Copies all formatted log history with timestamps directly to the Android clipboard.
     */
    public void copyLogsToClipboard() {
        String logs = VynaraLogger.getAllLogsAsString();

        if ((logs == null || logs.trim().isEmpty()) && tvLogOutput != null && tvLogOutput.getText() != null) {
            logs = tvLogOutput.getText().toString();
        }

        if (logs == null || logs.trim().isEmpty()) {
            Toast.makeText(getContext(), "Console log is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Vynara Diagnostic Logs", logs);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Diagnostic logs copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setExpandedState(boolean expanded) {
        if (expanded) {
            if (minimizedView != null) minimizedView.setVisibility(View.GONE);
            if (expandedView != null) {
                expandedView.setVisibility(View.VISIBLE);
                reloadAllLogs();
            }
        } else {
            if (expandedView != null) expandedView.setVisibility(View.GONE);
            if (minimizedView != null) minimizedView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Implements coordinate touch dragging physics, updating FrameLayout margins in real-time.
     */
    private void setupDragGesture(View targetView) {
        if (targetView == null) return;

        targetView.setOnTouchListener((v, event) -> {
            if (!isDragEnabled) return false;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    return false;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - lastTouchX;
                    float deltaY = event.getRawY() - lastTouchY;

                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
                        marginParams.leftMargin += (int) deltaX;
                        marginParams.topMargin += (int) deltaY;
                        setLayoutParams(marginParams);
                    }

                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    return true;
            }
            return false;
        });
    }

    private void reloadAllLogs() {
        if (tvLogOutput == null) return;
        tvLogOutput.setText("");

        List<VynaraLogger.LogEntry> history = VynaraLogger.getCopyOfLogs();
        for (VynaraLogger.LogEntry entry : history) {
            appendLog(entry);
        }
    }

    private void appendLog(VynaraLogger.LogEntry entry) {
        if (tvLogOutput == null || entry == null) return;

        String hexColor = getHexColorForLog(entry);
        String prefix = entry.getFormattedTime() + " " + (entry.getTag() != null ? entry.getTag().name() : "LOG");
        String htmlLine = "<font color=\"" + hexColor + "\"><b>" + prefix + "</b>: " + entry.getMessage() + "</font><br/>";

        tvLogOutput.append(Html.fromHtml(htmlLine));

        // Defer scroll calculations to post-layout tick for kinetic auto-scroll
        if (scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private String getHexColorForLog(VynaraLogger.LogEntry entry) {
        if (entry.getLevel() == VynaraLogger.LogLevel.ERROR) {
            return "#FF5252"; // High Alert Red
        }
        if (entry.getLevel() == VynaraLogger.LogLevel.WARNING) {
            return "#FFD700"; // Dynamic Alert Gold/Yellow
        }

        if (entry.getTag() == null) return "#FFFFFF";

        switch (entry.getTag()) {
            case SYSTEM:
                return "#90A4AE"; // Cool Gray
            case GEMINI:
                return "#E040FB"; // Deep Fuchsia/Magenta
            case AI:
                return "#00E5FF"; // Electric Cyan
            case KNOWLEDGE:
                return "#7C4DFF"; // Indigo
            case TOOL_MANIFEST:
            case MAPPER:
                return "#00E676"; // Connection Green
            case TASK:
            case EXECUTION:
                return "#E0E0E0"; // Off-white
            case GENERATOR:
            case MATERIAL:
                return "#FFB74D"; // Light Amber/Orange
            case VALIDATION:
                return "#81C784"; // Light Green
            case CLOUD:
                return "#40C4FF"; // Vibrant Cloud Cyan/Blue
            default:
                return "#FFFFFF";
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        VynaraLogger.registerListener(this);
        reloadAllLogs();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        VynaraLogger.unregisterListener(this);
    }

    @Override
    public void onLogAdded(VynaraLogger.LogEntry entry) {
        post(() -> {
            if (isAttachedToWindow()) {
                appendLog(entry);
            }
        });
    }

    @Override
    public void onLogsCleared() {
        post(() -> {
            if (tvLogOutput != null && isAttachedToWindow()) {
                tvLogOutput.setText("");
            }
        });
    }

    public void setDragEnabled(boolean enabled) {
        this.isDragEnabled = enabled;
    }
}