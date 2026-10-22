package com.homney.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Muestra el recordatorio de tareas a la hora elegida (sin red, solo con sesión y permiso)
 * y programa el del día siguiente.
 */
public class RecordatorioWorker extends Worker {

    private static final int NOTIF_ID = 1001;
    /** Si el móvil estuvo apagado y el aviso llega con más retraso que esto, no se muestra. */
    private static final long MAX_RETRASO_MS = TimeUnit.HOURS.toMillis(3);

    public RecordatorioWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (!Recordatorios.estaActivado(ctx)) return Result.success();

        try {
            long objetivo = getInputData().getLong(Recordatorios.DATO_OBJETIVO, 0);
            boolean aTiempo = objetivo == 0
                    || System.currentTimeMillis() - objetivo <= MAX_RETRASO_MS;
            if (aTiempo) mostrarNotificacion(ctx);
        } finally {
            // Siempre deja programado el aviso de mañana (como último paso)
            Recordatorios.programar(ctx, true);
        }
        return Result.success();
    }

    @SuppressLint("MissingPermission") // se comprueba con Recordatorios.tienePermisoNotificaciones
    private void mostrarNotificacion(Context ctx) {
        int idUsuario = ctx.getSharedPreferences("sesion", Context.MODE_PRIVATE)
                .getInt("id_usuario", -1);
        if (idUsuario == -1) return;                                  // sin sesión
        if (!Recordatorios.tienePermisoNotificaciones(ctx)) return;   // sin permiso

        // MainActivity ya navega al destino recibido en el extra "destino"
        Intent intent = new Intent(ctx, MainActivity.class)
                .putExtra("destino", R.id.fragmento3)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(ctx, Recordatorios.CANAL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(ctx.getString(R.string.notif_titulo))
                .setContentText(ctx.getString(R.string.notif_texto_recordatorio))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n);
    }
}
