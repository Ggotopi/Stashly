package com.stashly.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import java.io.OutputStream;

public class Bridge {
    private final Context ctx;
    private final Handler h = new Handler(Looper.getMainLooper());

    public Bridge(Context c) { ctx = c.getApplicationContext(); }

    private void toast(final String m) {
        h.post(new Runnable() { public void run() { Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show(); } });
    }

    @JavascriptInterface public String paste() {
        ClipboardManager c = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (c != null && c.hasPrimaryClip() && c.getPrimaryClip().getItemCount() > 0) {
            CharSequence t = c.getPrimaryClip().getItemAt(0).coerceToText(ctx);
            return t == null ? "" : t.toString();
        }
        return "";
    }

    @JavascriptInterface public void copy(String t) {
        ClipboardManager c = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (c != null) c.setPrimaryClip(ClipData.newPlainText("stashly", t));
    }

    @JavascriptInterface public void save(String name, String b64) {
        try {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri u = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            OutputStream o = ctx.getContentResolver().openOutputStream(u);
            o.write(data);
            o.close();
            toast("Saved to Downloads: " + name);
        } catch (Exception e) {
            toast("Save failed");
        }
    }

    @JavascriptInterface public boolean bubbleOn() { return OverlayService.running; }

    @JavascriptInterface public void toggleBubble() {
        h.post(new Runnable() { public void run() {
            if (OverlayService.running) {
                ctx.startService(new Intent(ctx, OverlayService.class).setAction("STOP"));
                toast("Bubble off");
            } else if (!Settings.canDrawOverlays(ctx)) {
                toast("Allow \"Display over other apps\", then turn the bubble on again");
                ctx.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + ctx.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                OverlayService.running = true;
                ctx.startForegroundService(new Intent(ctx, OverlayService.class));
                toast("Bubble on");
            }
        } });
    }
}
