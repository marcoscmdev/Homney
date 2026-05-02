package com.homney.app.login_registro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PASO 1 del wizard de configuración del hogar.
 * Equivale al Paso 1 de la web: chips de sugerencia para habitaciones,
 * lista de seleccionadas y campo personalizado.
 *
 * Al pulsar "Siguiente" se crean las habitaciones vía API y se lanza Activity3 (gastos recurrentes).
 */
public class Activity2_registro_config_hogar extends AppCompatActivity {

    /* ── Sugerencias (igual que ROOM_SUGG de la web) ─────── */
    private static final String[] SUGERENCIAS = {
        "Cocina", "Baño", "Salón", "Comedor",
        "Dormitorio", "Despacho", "Pasillo",
        "Terraza", "Garaje", "Cuarto de limpieza"
    };

    /* ── Vistas ─────────────────────────────────────────── */
    private LinearLayout llChipsFila1, llChipsFila2, llChipsFila3;
    private LinearLayout llSelectedRooms;
    private TextView     tvSinSeleccion;
    private EditText     etCustomRoom;
    private Button       btnAddCustom, btnSiguiente;

    /* ── Estado ─────────────────────────────────────────── */
    /** Estancias seleccionadas (nombre). */
    private final List<String>  habitacionesSeleccionadas = new ArrayList<>();
    /** Chips activos (índice → View) para toggling. */
    private final View[]        chipViews = new View[SUGERENCIAS.length];

    private int  idHogar  = -1;
    private LoadingDialog loadingDialog;
    private static final String TAG = "WZ_ESTANCIAS";

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity2_registro_config_hogar);

        // Leer sesión
        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar = prefs.getInt("id_hogar", -1);

        loadingDialog = new LoadingDialog(this);

        // Enlazar vistas
        llChipsFila1   = findViewById(R.id.ll_chips_fila1);
        llChipsFila2   = findViewById(R.id.ll_chips_fila2);
        llChipsFila3   = findViewById(R.id.ll_chips_fila3);
        llSelectedRooms = findViewById(R.id.ll_selected_rooms);
        tvSinSeleccion  = findViewById(R.id.tv_sin_seleccion);
        etCustomRoom    = findViewById(R.id.et_custom_room);
        btnAddCustom    = findViewById(R.id.btn_add_custom);
        btnSiguiente    = findViewById(R.id.btn_siguiente);

        construirChips();

        btnAddCustom.setOnClickListener(v -> añadirEstanciaPersonalizada());
        btnSiguiente.setOnClickListener(v -> guardarYContinuar());
    }

    /* ════════════════════════════════════════════════════════
       CHIPS DE SUGERENCIAS
       Distribuidos en 3 filas (4 + 4 + 2)
    ════════════════════════════════════════════════════════ */

    private void construirChips() {
        int gap = dp(8);
        LinearLayout[] filas = { llChipsFila1, llChipsFila2, llChipsFila3 };

        for (int i = 0; i < SUGERENCIAS.length; i++) {
            final int idx = i;
            final String nombre = SUGERENCIAS[i];

            TextView chip = new TextView(this);
            chip.setText(nombre);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            chip.setBackground(crearChipDrawable(false));
            chip.setTextColor(getResources().getColor(R.color.text, null));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin  = gap;
            lp.bottomMargin = gap / 2;
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> toggleChip(idx, nombre, chip));

            // Distribuir: filas de 4 / 4 / 2
            int fila = i < 4 ? 0 : i < 8 ? 1 : 2;
            filas[fila].addView(chip);
            chipViews[idx] = chip;
        }
    }

    private void toggleChip(int idx, String nombre, TextView chip) {
        if (habitacionesSeleccionadas.contains(nombre)) {
            habitacionesSeleccionadas.remove(nombre);
            chip.setBackground(crearChipDrawable(false));
            chip.setTextColor(getResources().getColor(R.color.text, null));
        } else {
            habitacionesSeleccionadas.add(nombre);
            chip.setBackground(crearChipDrawable(true));
            chip.setTextColor(Color.WHITE);
        }
        actualizarListaSeleccionadas();
    }

    /* ════════════════════════════════════════════════════════
       CAMPO PERSONALIZADO
    ════════════════════════════════════════════════════════ */

    private void añadirEstanciaPersonalizada() {
        String nombre = etCustomRoom.getText().toString().trim();
        if (nombre.isEmpty()) return;
        if (!habitacionesSeleccionadas.contains(nombre)) {
            habitacionesSeleccionadas.add(nombre);
            actualizarListaSeleccionadas();
        }
        etCustomRoom.setText("");
    }

    /* ════════════════════════════════════════════════════════
       LISTA DE SELECCIONADAS
    ════════════════════════════════════════════════════════ */

    private void actualizarListaSeleccionadas() {
        // Quitar todo excepto tvSinSeleccion
        int childCount = llSelectedRooms.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View v = llSelectedRooms.getChildAt(i);
            if (v != tvSinSeleccion) llSelectedRooms.removeViewAt(i);
        }

        if (habitacionesSeleccionadas.isEmpty()) {
            tvSinSeleccion.setVisibility(View.VISIBLE);
            return;
        }

        tvSinSeleccion.setVisibility(View.GONE);
        for (String hab : habitacionesSeleccionadas) {
            llSelectedRooms.addView(crearTagSeleccionado(hab));
        }
    }

    /** Tag visual "Cocina  ✕" para la lista de seleccionadas. */
    private View crearTagSeleccionado(String nombre) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        filaLp.bottomMargin = dp(6);
        fila.setLayoutParams(filaLp);

        // Nombre
        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre);
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        fila.addView(tvNombre);

        // Botón eliminar "✕"
        TextView tvRemove = new TextView(this);
        tvRemove.setText("✕");
        tvRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvRemove.setTextColor(getResources().getColor(R.color.danger, null));
        tvRemove.setTypeface(null, Typeface.BOLD);
        tvRemove.setPadding(dp(8), 0, 0, 0);
        tvRemove.setOnClickListener(v -> eliminarSeleccionada(nombre));
        fila.addView(tvRemove);

        return fila;
    }

    private void eliminarSeleccionada(String nombre) {
        habitacionesSeleccionadas.remove(nombre);

        // Desactivar chip si era sugerencia
        for (int i = 0; i < SUGERENCIAS.length; i++) {
            if (SUGERENCIAS[i].equals(nombre) && chipViews[i] instanceof TextView) {
                TextView chip = (TextView) chipViews[i];
                chip.setBackground(crearChipDrawable(false));
                chip.setTextColor(getResources().getColor(R.color.text, null));
                break;
            }
        }
        actualizarListaSeleccionadas();
    }

    /* ════════════════════════════════════════════════════════
       GUARDAR Y CONTINUAR → Activity3 (Gastos recurrentes)
    ════════════════════════════════════════════════════════ */

    private void guardarYContinuar() {
        if (habitacionesSeleccionadas.isEmpty()) {
            Toast.makeText(this, "Añade al menos una estancia", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idHogar == -1) {
            Toast.makeText(this, "Error: hogar no identificado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Utilidades.hayConexionInternet(this)) {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSiguiente.setEnabled(false);
        btnSiguiente.setText("Guardando…");
        loadingDialog.show();

        // Crear todas las habitaciones en paralelo
        AtomicInteger pendiente   = new AtomicInteger(habitacionesSeleccionadas.size());
        AtomicInteger errores     = new AtomicInteger(0);

        for (String nombre : habitacionesSeleccionadas) {
            crearHabitacion(nombre, pendiente, errores);
        }
    }

    private void crearHabitacion(String nombre,
                                  AtomicInteger pendiente, AtomicInteger errores) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre);
            body.put("tipo", "generica");   // tipo por defecto; el usuario puede editarlo después
            body.put("id_hogar", idHogar);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                    Request.Method.POST, WebService.URL_Habitacion, body,
                    response -> {
                        try {
                            if (!response.getString(WebService.JSON.STATUS)
                                    .equals(WebService.JSON.SUCCESS)) {
                                errores.incrementAndGet();
                            }
                        } catch (JSONException e) {
                            errores.incrementAndGet();
                        }
                        if (pendiente.decrementAndGet() == 0) onTodasCreadas(errores.get());
                    },
                    error -> {
                        errores.incrementAndGet();
                        if (pendiente.decrementAndGet() == 0) onTodasCreadas(errores.get());
                    }
            ));
        } catch (JSONException e) {
            errores.incrementAndGet();
            if (pendiente.decrementAndGet() == 0) onTodasCreadas(errores.get());
        }
    }

    private void onTodasCreadas(int errores) {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            btnSiguiente.setEnabled(true);
            btnSiguiente.setText("Siguiente →");

            if (errores > 0) {
                Toast.makeText(this,
                        errores + " estancia(s) no pudieron guardarse. Puedes añadirlas después.",
                        Toast.LENGTH_LONG).show();
            }

            // Continuar al paso siguiente: Gastos recurrentes
            startActivity(new Intent(this, Activity3_gastos_recurrentes.class));
        });
    }

    /* ════════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════════ */

    /** Chip de sugerencia: fondo amarillo si activo, borde fino si inactivo. */
    private GradientDrawable crearChipDrawable(boolean activo) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(20));
        if (activo) {
            gd.setColor(getResources().getColor(R.color.accent_dark, null));
        } else {
            gd.setColor(getResources().getColor(R.color.surface, null));
            gd.setStroke(dp(1), getResources().getColor(R.color.border, null));
        }
        return gd;
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}
