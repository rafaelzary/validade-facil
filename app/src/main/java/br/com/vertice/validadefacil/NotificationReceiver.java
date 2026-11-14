package br.com.vertice.validadefacil;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class NotificationReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        int id = intent.getIntExtra("notificationId", (int)System.currentTimeMillis());
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, "validade_alerts") : new Notification.Builder(context);
        Notification n = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null ? "Validade Fácil" : title)
                .setContentText(body == null ? "Confira seu estoque." : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "Confira seu estoque." : body))
                .setAutoCancel(true).setContentIntent(content).build();
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
    }
}
