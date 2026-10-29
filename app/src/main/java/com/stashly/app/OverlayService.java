package com.stashly.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    public static volatile boolean running = false;
    private WindowManager wm;
    private TextView bubble;
    private FrameLayout panel;
    private WebView web;
    private WindowManager.LayoutParams bp, pp;
    private boolean open = false;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            cleanup();
            stopSelf();
            return START_NOT_STICKY;
        }
        foreground();
        if (bubble == null) setup();
        running = true;
        return START_STICKY;
    }

    @Override public void onDestroy() { cleanup(); super.onDestroy(); }

    private void foreground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("stashly", "Stashly bubble", NotificationManager.IMPORTANCE_LOW));
        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, "stashly")
                .setContentTitle("Stashly bubble is on")
                .setContentText("Tap Stop to remove the bubble")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .addAction(android.R.drawable.ic_delete, "Stop", pi)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    private void setup() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final float d = getResources().getDisplayMetrics().density;
        DisplayMetrics m = getResources().getDisplayMetrics();
        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
        web.loadUrl("file:///android_asset/Stashly.html");
        panel = new FrameLayout(this);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(Color.WHITE);
        pbg.setCornerRadius(16 * d);
        panel.setBackground(pbg);
        panel.setClipToOutline(true);
        panel.addView(web, new FrameLayout.LayoutParams(-1, -1));
        pp = new WindowManager.LayoutParams((int) (m.widthPixels * 0.94f), (int) (m.heightPixels * 0.7f),
                type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        pp.gravity = Gravity.TOP | Gravity.START;
        pp.x = (int) (m.widthPixels * 0.03f);
        pp.y = (int) (90 * d);
        pp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        bubble = new TextView(this);
        bubble.setText("S");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(20);
        bubble.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0xFF2F6F5E);
        bubble.setBackground(g);
        bubble.setElevation(8 * d);
        bp = new WindowManager.LayoutParams((int) (56 * d), (int) (56 * d), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bp.gravity = Gravity.TOP | Gravity.START;
        bp.x = (int) (m.widthPixels - 70 * d);
        bp.y = (int) (200 * d);
        bubble.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy;
            float tx, ty;
            boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = bp.x; sy = bp.y; tx = e.getRawX(); ty = e.getRawY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - tx, dy = e.getRawY() - ty;
                        if (Math.abs(dx) > 10 * d || Math.abs(dy) > 10 * d) moved = true;
                        if (moved) {
                            bp.x = (int) (sx + dx);
                            bp.y = (int) (sy + dy);
                            try { wm.updateViewLayout(bubble, bp); } catch (Exception ex) { }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggle();
                        return true;
                }
                return false;
            }
        });
        wm.addView(bubble, bp);
    }

    private void toggle() {
        try {
            if (open) {
                wm.removeView(panel);
                open = false;
            } else {
                wm.addView(panel, pp);
                open = true;
                wm.removeView(bubble);
                wm.addView(bubble, bp);
            }
        } catch (Exception e) { }
    }

    private void cleanup() {
        try { if (open && panel != null) wm.removeView(panel); } catch (Exception e) { }
        open = false;
        try { if (bubble != null) wm.removeView(bubble); } catch (Exception e) { }
        bubble = null;
        if (web != null) { try { web.destroy(); } catch (Exception e) { } web = null; }
        running = false;
    }
}
