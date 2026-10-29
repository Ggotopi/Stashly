package com.stashly.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private static final int FC = 7;
    private static final String NOTIF = "android.permission.POST_NOTIFICATIONS";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(NOTIF) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{NOTIF}, 1);
        }
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    startActivityForResult(p.createIntent(), FC);
                } catch (Exception e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
        web.loadUrl("file:///android_asset/Stashly.html");
        setContentView(web);
    }

    @Override protected void onActivityResult(int rq, int rs, Intent d) {
        if (rq == FC && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(rs, d));
            fileCb = null;
        } else {
            super.onActivityResult(rq, rs, d);
        }
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript("(function(){var s=document.getElementById('sh');if(s&&!s.hidden){s.hidden=true;s.innerHTML='';return 1}return 0})()",
                new ValueCallback<String>() {
                    @Override public void onReceiveValue(String v) { if (!"1".equals(v)) finish(); }
                });
    }

    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
