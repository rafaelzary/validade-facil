package br.com.vertice.validadefacil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public final class NotificationScheduler {
    static final String PREFS = "validade_facil_native";
    static final String DATA_KEY = "scheduled_food_json";
    private static final String IDS_KEY = "scheduled_alarm_ids";
    private static final String CHANNEL = "validade_alerts";
    private NotificationScheduler() {}

    static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Alertas de validade", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Avisos locais sobre alimentos próximos do vencimento e vencidos.");
            ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    static void rescheduleSaved(Context c) {
        String json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DATA_KEY, "");
        if (json != null && !json.isEmpty()) schedule(c, json);
    }

    static void schedule(Context c, String json) {
        AlarmManager manager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        android.content.SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> old = prefs.getStringSet(IDS_KEY, new HashSet<>());
        if (old != null) for (String id : old) { PendingIntent previous = pending(c, id); if (previous != null) manager.cancel(previous); }
        Set<String> ids = new HashSet<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray foods = root.optJSONArray("foods");
            if (foods != null) {
                int[] offsets = new int[]{15,7,3,1,0,-1};
                for (int i=0; i<foods.length(); i++) {
                    JSONObject f = foods.optJSONObject(i);
                    if (f == null || f.optString("done", "").length() > 0) continue;
                    String name = f.optString("name", "Alimento");
                    String expiry = f.optString("expiry", "");
                    String foodId = f.optString("id", String.valueOf(i));
                    double qty = f.optDouble("qty", 1);
                    String unit = f.optString("unit", "unidade(s)");
                    Calendar expiryCal = Calendar.getInstance();
                    try {
                        Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(expiry);
                        if (parsed == null) continue;
                        expiryCal.setTime(parsed);
                    } catch (Exception badDate) { continue; }
                    for (int offset : offsets) {
                        Calendar alertCal = (Calendar) expiryCal.clone();
                        if (offset == -1) alertCal.add(Calendar.DAY_OF_YEAR, 1);
                        else alertCal.add(Calendar.DAY_OF_YEAR, -offset);
                        alertCal.set(Calendar.HOUR_OF_DAY, 9);
                        alertCal.set(Calendar.MINUTE, 0);
                        alertCal.set(Calendar.SECOND, 0);
                        alertCal.set(Calendar.MILLISECOND, 0);
                        long at = alertCal.getTimeInMillis();
                        String key = foodId + "_" + offset + "_" + expiry;
                        if (at <= System.currentTimeMillis()) continue;
                        Intent intent = new Intent(c, NotificationReceiver.class);
                        intent.putExtra("title", offset > 0 ? "Produto vence em " + offset + " dia(s)" : offset == 0 ? "Produto vence hoje" : "Produto vencido");
                        intent.putExtra("body", name + " · " + qty + " " + unit + (offset == -1 ? " · venceu ontem" : ""));
                        intent.putExtra("notificationId", key.hashCode());
                        PendingIntent pi = PendingIntent.getBroadcast(c, key.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        if (Build.VERSION.SDK_INT >= 31 && manager.canScheduleExactAlarms()) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                        else if (Build.VERSION.SDK_INT >= 23) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                        else manager.set(AlarmManager.RTC_WAKEUP, at, pi);
                        ids.add(key);
                    }
                }
            }
        } catch (Exception ignored) { }
        prefs.edit().putStringSet(IDS_KEY, ids).apply();
    }

    private static PendingIntent pending(Context c, String id) {
        Intent i = new Intent(c, NotificationReceiver.class);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }
}
