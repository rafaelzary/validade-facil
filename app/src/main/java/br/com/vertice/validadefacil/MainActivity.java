package br.com.vertice.validadefacil;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.WebSettings;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int REQ_NOTIFICATIONS = 501;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(16,21,28));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(16,21,28));
        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(16,21,28));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        NotificationScheduler.createChannel(this);
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationScheduler.rescheduleSaved(this);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    public class AndroidBridge {
        private final Context context;
        AndroidBridge(Context c) { context = c.getApplicationContext(); }

        @JavascriptInterface public void scheduleAlerts(String json) {
            getSharedPreferences(NotificationScheduler.PREFS, MODE_PRIVATE).edit().putString(NotificationScheduler.DATA_KEY, json).apply();
            NotificationScheduler.schedule(context, json);
        }

        @JavascriptInterface public void requestPermissions() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
                } else {
                    Toast.makeText(MainActivity.this, "Permissão de notificações verificada.", Toast.LENGTH_SHORT).show();
                }
                if (Build.VERSION.SDK_INT >= 31) {
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (!am.canScheduleExactAlarms()) {
                        try {
                            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                            startActivity(i);
                        } catch (Exception ignored) { }
                    }
                }
                NotificationScheduler.rescheduleSaved(MainActivity.this);
            });
        }
    }
}
