package com.homney.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Recordatorio diario de tareas: canal, preferencias del usuario (activado + hora),
 * programación y lógica que se ejecuta tras una acción positiva.
 *
 * El aviso se programa como trabajo de UNA sola ejecución que se reprograma a sí mismo
 * al terminar (calculando la próxima hora local con Calendar), así siempre salta a la hora
 * elegida aunque el móvil cambie de horario de verano a invierno.
 */
public final class Recordatorios {

    public static final String CANAL_ID = "recordatorios";
    static final String DATO_OBJETIVO = "objetivo_ms";

    private static final String TRABAJO_DIARIO = "recordatorio_diario_v2";
    private static final String TRABAJO_ANTIGUO = "recordatorio_diario";
    private static final String PREFS = "recordatorios";
    private static final int HORA_POR_DEFECTO = 20;
    private static final int MINUTO_POR_DEFECTO = 0;

    private Recordatorios() {}

    /* ── Canal ─────────────────────────────────────────────── */

    /** Crea el canal (solo API 26+; el minSdk del proyecto es 25). */
    public static void crearCanal(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel canal = new NotificationChannel(
                CANAL_ID,
                ctx.getString(R.string.notif_canal_nombre),
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(canal);
    }

    /* ── Preferencias ──────────────────────────────────────── */

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean estaActivado(Context ctx) {
        return prefs(ctx).getBoolean("activado", false);
    }

    public static int getHora(Context ctx) {
        return prefs(ctx).getInt("hora", HORA_POR_DEFECTO);
    }

    public static int getMinuto(Context ctx) {
        return prefs(ctx).getInt("minuto", MINUTO_POR_DEFECTO);
    }

    public static boolean dialogoMostrado(Context ctx) {
        return prefs(ctx).getBoolean("dialogo_mostrado", false);
    }

    public static void marcarDialogoMostrado(Context ctx) {
        prefs(ctx).edit().putBoolean("dialogo_mostrado", true).apply();
    }

    public static String formatoHora(int hora, int minuto) {
        return String.format(Locale.getDefault(), "%02d:%02d", hora, minuto);
    }

    /** true si se puede publicar notificaciones (en API < 33 no hace falta permiso). */
    public static boolean tienePermisoNotificaciones(Context ctx) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Guarda activado + hora y reprograma (o cancela) el aviso. */
    public static void guardar(Context ctx, boolean activado, int hora, int minuto) {
        prefs(ctx).edit()
                .putBoolean("activado", activado)
                .putInt("hora", hora)
                .putInt("minuto", minuto)
                .apply();
        programar(ctx, true);
    }

    public static void guardarActivado(Context ctx, boolean activado) {
        prefs(ctx).edit().putBoolean("activado", activado).apply();
        programar(ctx, true);
    }

    public static void guardarHora(Context ctx, int hora, int minuto) {
        prefs(ctx).edit().putInt("hora", hora).putInt("minuto", minuto).apply();
        programar(ctx, true);
    }

    /* ── Programación ──────────────────────────────────────── */

    /**
     * Programa el próximo aviso a la hora elegida, o cancela si está desactivado.
     * @param reemplazar true al cambiar la hora/estado o al reprogramar desde el Worker;
     *                   false en el arranque de la app (si ya hay uno pendiente, lo respeta).
     */
    public static void programar(Context ctx, boolean reemplazar) {
        WorkManager wm = WorkManager.getInstance(ctx.getApplicationContext());
        wm.cancelUniqueWork(TRABAJO_ANTIGUO);

        if (!estaActivado(ctx)) {
            wm.cancelUniqueWork(TRABAJO_DIARIO);
            return;
        }

        Calendar ahora = Calendar.getInstance();
        Calendar objetivo = (Calendar) ahora.clone();
        objetivo.set(Calendar.HOUR_OF_DAY, getHora(ctx));
        objetivo.set(Calendar.MINUTE, getMinuto(ctx));
        objetivo.set(Calendar.SECOND, 0);
        objetivo.set(Calendar.MILLISECOND, 0);
        if (!objetivo.after(ahora)) objetivo.add(Calendar.DAY_OF_YEAR, 1);

        long retraso = objetivo.getTimeInMillis() - ahora.getTimeInMillis();
        OneTimeWorkRequest peticion = new OneTimeWorkRequest.Builder(RecordatorioWorker.class)
                .setInitialDelay(retraso, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putLong(DATO_OBJETIVO, objetivo.getTimeInMillis())
                        .build())
                .build();
        wm.enqueueUniqueWork(TRABAJO_DIARIO,
                reemplazar ? ExistingWorkPolicy.REPLACE : ExistingWorkPolicy.KEEP, peticion);
    }

    /* ── Momento positivo ──────────────────────────────────── */

    /**
     * Llamar tras un momento positivo (tarea completada, gasto creado).
     * La primera vez ofrece configurar el recordatorio; si no, intenta la reseña.
     * Nunca muestra los dos diálogos a la vez.
     */
    public static void alCompletarAccionPositiva(Activity activity) {
        boolean mostroDialogo = activity instanceof MainActivity
                && ((MainActivity) activity).mostrarDialogoRecordatorioSiProcede();
        ResenaHelper.accionPositiva(activity, !mostroDialogo);
    }
}
