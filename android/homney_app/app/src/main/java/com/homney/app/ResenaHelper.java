package com.homney.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Pide la reseña con la In-App Review API de Google Play.
 * Condiciones: >= 5 acciones positivas, >= 3 días desde la primera, y >= 60 días desde la última petición.
 * Google decide en última instancia si muestra el diálogo (hay cuota).
 */
public final class ResenaHelper {

    private static final String PREFS = "resenas"; // aparte de "sesion": no se borra al cerrar sesión
    private static final long DIA_MS = 24L * 3600 * 1000;
    private static final int MIN_ACCIONES = 5;
    private static final long MIN_DIAS_DESDE_PRIMERA = 3;
    private static final long MIN_DIAS_ENTRE_PETICIONES = 60;

    private ResenaHelper() {}

    public static void accionPositiva(Activity activity, boolean puedeMostrar) {
        SharedPreferences p = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long ahora = System.currentTimeMillis();

        long primera = p.getLong("primera_vez", 0);
        if (primera == 0) {
            primera = ahora;
            p.edit().putLong("primera_vez", ahora).apply();
        }
        int acciones = p.getInt("acciones", 0) + 1;
        p.edit().putInt("acciones", acciones).apply();

        if (!puedeMostrar) return;
        if (acciones < MIN_ACCIONES) return;
        if (ahora - primera < MIN_DIAS_DESDE_PRIMERA * DIA_MS) return;
        if (ahora - p.getLong("ultima_peticion", 0) < MIN_DIAS_ENTRE_PETICIONES * DIA_MS) return;

        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(tarea -> {
            if (!tarea.isSuccessful() || activity.isFinishing()) return;
            manager.launchReviewFlow(activity, tarea.getResult())
                    .addOnCompleteListener(fin -> p.edit()
                            .putLong("ultima_peticion", System.currentTimeMillis()).apply());
        });
    }
}
