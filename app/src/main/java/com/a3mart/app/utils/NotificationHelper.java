package com.a3mart.app.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.a3mart.app.MainActivity;
import com.a3mart.app.R;

public class NotificationHelper {
    public static void kirimNotifikasiLunas(Context context, String nama, long nominal) {
        android.content.SharedPreferences pref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        if (!pref.getBoolean("notif_lunas", true)) return;

        boolean isDeposit = nominal < 0;
        String judulNotif = isDeposit ? "Deposit Berhasil! 💰" : "Pembayaran Lunas! ✅";
        String labelStatus = isDeposit ? "Status: Deposit Masuk" : "Status: Lunas";
        String pesanAwal = isDeposit ? "Penerimaan saldo sebesar " : "Dana sebesar ";

        String channelId = "transaksi_channel_v6"; 
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    isDeposit ? "Notifikasi Deposit" : "Notifikasi Pembayaran",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0}); 

            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.suara_lunas);
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(soundUri, attr);

            if (manager != null) manager.createNotificationChannel(channel);
        }

        String formattedNominal = FormatterUtils.formatRupiah(Math.abs(nominal));

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.logo)
                .setColor(Color.parseColor(isDeposit ? "#FF9800" : "#4CAF50"))
                .setContentTitle(isDeposit ? "Deposit Baru" : "Pembayaran Lunas")
                .setContentText(nama + " - " + formattedNominal)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(judulNotif)
                        .bigText(pesanAwal + formattedNominal + " dari " + nama + ".\n" + labelStatus + "."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSound(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.suara_lunas)) 
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(nama.hashCode(), builder.build()); 
        }
    }

    public static void kirimNotifikasiStokTipis(Context context, String namaProduk, String pesan) {
        android.content.SharedPreferences pref =
                context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        if (!pref.getBoolean("low_stock_alert", true)) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "stok_alert_channel_v4";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            channelId, "Peringatan Stok", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle("Peringatan Stok! ⚠️")
                        .setContentText(pesan)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true);

        if (manager != null) {
            manager.notify(namaProduk.hashCode(), builder.build());
        }
    }
}
