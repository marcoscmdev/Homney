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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Paso 2 del wizard de registro.
 * El usuario selecciona/añade las estancias de su hogar.
 * Cada estancia tiene un nombre visible y un tipo que encaja con el enum de la BD:
 *   generica | cocina | aseo | garaje | exterior | dormitorio | infantil |
 *   comedor  | salon  | oficina | trastero | recibidor | terraza | deportiva
 */
public class Activity2_registro_config_hogar extends AppCompatActivity {

    /* ── Modelo de estancia ───────────────────────────────── */
    private static class Estancia {
        final String nombre;
        final String tipo;
        Estancia(String nombre, String tipo) {
            this.nombre = nombre;
            this.tipo   = tipo;
        }
    }

    /* ── Sugerencias predefinidas (nombre visible → tipo enum) ─ */
    private static final String[] SUGERENCIAS_NOMBRE = {
        "Cocina", "Baño", "Salón", "Comedor",
        "Dormitorio", "Infantil", "Terraza", "Exterior",
        "Garaje", "Despacho", "Recibidor", "Trastero",
        "Deportiva", "Genérica"
    };
    private static final String[] SUGERENCIAS_TIPO = {
        "cocina", "aseo", "salon", "comedor",
        "dormitorio", "infantil", "terraza", "exterior",
        "garaje", "oficina", "recibidor", "trastero",
        "deportiva", "generica"
    };

    /** Todas las opciones del enum para el spinner del diálogo de tipo personalizado. */
    private static final String[] TIPO_OPCIONES_NOMBRE = {
        "Genérica", "Cocina", "Baño / Aseo", "Garaje", "Exterior",
        "Dormitorio", "Habitación infantil", "Comedor", "Salón",
        "Oficina / Despacho", "Trastero", "Recibidor", "Terraza", "Sala deportiva"
    };
    private static final String[] TIPO_OPCIONES_VALOR = {
        "generica", "cocina", "aseo", "garaje", "exterior",
        "dormitorio", "infantil", "comedor", "salon",
        "oficina", "trastero", "recibidor", "terraza", "deportiva"
    };

    /* ── Vistas ─────────────────────────────────────────────── */
    private LinearLayout llChipsFila1, llChipsFila2, llChipsFila3, llChipsFila4;
    private LinearLayout llSelectedRooms;
    private TextView     tvSinSeleccion;
    private EditText     etCustomRoom;
    private Button       btnAddCustom, btnSiguiente;

    /* ── Estado ─────────────────────────────────────────────── */
    private final List<Estancia> habitacionesSeleccionadas = new ArrayList<>();
    private final View[]         chipViews = new View[SUGERENCIAS_NOMBRE.length];

    private int  idHogar  = -1;
    private LoadingDialog loadingDialog;

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity2_registro_config_hogar);

        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar = prefs.getInt("id_hogar", -1);

        loadingDialog = new LoadingDialog(this);

        llChipsFila1   = findViewById(R.id.ll_chips_fila1);
        llChipsFila2   = findViewById(R.id.ll_chips_fila2);
        llChipsFila3   = findViewById(R.id.ll_chips_fila3);
        llChipsFila4   = findViewById(R.id.ll_chips_fila4);
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
       CHIPS DE SUGERENCIAS  (4 + 4 + 3 + 3)
    ════════════════════════════════════════════════════════ */

    private void construirChips() {
        int gap = dp(8);
        LinearLayout[] filas = { llChipsFila1, llChipsFila2, llChipsFila3, llChipsFila4 };

        for (int i = 0; i < SUGERENCIAS_NOMBRE.length; i++) {
            final int idx    = i;
            final String nom = SUGERENCIAS_NOMBRE[i];

            TextView chip = new TextView(this);
            chip.setText(nom);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            chip.setBackground(crearChipDrawable(false));
            chip.setTextColor(getResources().getColor(R.color.text, null));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin  = gap;
            lp.bottomMargin = gap / 2;
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> toggleChip(idx, chip));

            // Distribuir: 4 / 4 / 3 / 3
            int fila = i < 4 ? 0 : i < 8 ? 1 : i < 11 ? 2 : 3;
            filas[fila].addView(chip);
            chipViews[idx] = chip;
        }
    }

    private void toggleChip(int idx, TextView chip) {
        String nombre = SUGERENCIAS_NOMBRE[idx];
        String tipo   = SUGERENCIAS_TIPO[idx];

        // ¿ya está seleccionada?
        Estancia yaSeleccionada = buscarEstanciaPorNombre(nombre);
        if (yaSeleccionada != null) {
            habitacionesSeleccionadas.remove(yaSeleccionada);
            chip.setBackground(crearChipDrawable(false));
            chip.setTextColor(getResources().getColor(R.color.text, null));
        } else {
            habitacionesSeleccionadas.add(new Estancia(nombre, tipo));
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

        // Evitar duplicados
        if (buscarEstanciaPorNombre(nombre) != null) {
            etCustomRoom.setText("");
            return;
        }

        String tipoDeducido = resolverTipo(nombre);
        if (tipoDeducido != null) {
            // Coincide con algún tipo conocido → añadir directamente
            habitacionesSeleccionadas.add(new Estancia(nombre, tipoDeducido));
            actualizarListaSeleccionadas();
            etCustomRoom.setText("");
        } else {
            // Nombre desconocido → pedir al usuario que elija el tipo
            mostrarDialogElegirTipo(nombre);
        }
    }

    /**
     * Intenta deducir el tipo enum a partir del nombre escrito por el usuario.
     * Comprueba primero si el nombre en minúsculas ES un valor del enum,
     * luego si coincide con alguno de los nombres de sugerencia.
     * Devuelve null si no se puede determinar.
     */
    private String resolverTipo(String nombre) {
        String norm = nombre.toLowerCase(Locale.getDefault()).trim();

        // Comprobación directa contra los valores del enum
        for (String val : TIPO_OPCIONES_VALOR) {
            if (norm.equals(val)) return val;
        }

        // Comprobación contra los nombres de sugerencia (display names)
        for (int i = 0; i < SUGERENCIAS_NOMBRE.length; i++) {
            if (norm.equals(SUGERENCIAS_NOMBRE[i].toLowerCase(Locale.getDefault()))) {
                return SUGERENCIAS_TIPO[i];
            }
        }

        // Comprobaciones parciales habituales
        if (norm.contains("cocin"))          return "cocina";
        if (norm.contains("baño") ||
            norm.contains("aseo") ||
            norm.contains("bano"))           return "aseo";
        if (norm.contains("garaje") ||
            norm.contains("parking"))        return "garaje";
        if (norm.contains("jard") ||
            norm.contains("patio") ||
            norm.contains("exterior"))       return "exterior";
        if (norm.contains("dormit") ||
            norm.contains("habitaci") ||
            norm.contains("cuarto") ||
            norm.contains("alcoba"))         return "dormitorio";
        if (norm.contains("infantil") ||
            norm.contains("niño") ||
            norm.contains("nino"))           return "infantil";
        if (norm.contains("comedor"))        return "comedor";
        if (norm.contains("salon") ||
            norm.contains("salón") ||
            norm.contains("living"))         return "salon";
        if (norm.contains("oficin") ||
            norm.contains("despacho") ||
            norm.contains("estudio"))        return "oficina";
        if (norm.contains("trastero") ||
            norm.contains("almacen") ||
            norm.contains("almacén"))        return "trastero";
        if (norm.contains("recibidor") ||
            norm.contains("pasillo") ||
            norm.contains("entrada") ||
            norm.contains("hall"))           return "recibidor";
        if (norm.contains("terraza") ||
            norm.contains("balcon") ||
            norm.contains("balcón"))         return "terraza";
        if (norm.contains("gym") ||
            norm.contains("gimnasio") ||
            norm.contains("deport"))         return "deportiva";

        return null; // Desconocido → el usuario deberá elegir
    }

    /**
     * Muestra un AlertDialog con un Spinner para que el usuario elija el tipo
     * de la estancia personalizada que escribió.
     */
    private void mostrarDialogElegirTipo(String nombreEstancia) {
        // Spinner con los nombres amigables de los tipos
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TIPO_OPCIONES_NOMBRE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Margen para que el Spinner no quede pegado al borde del diálogo
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), dp(4));
        container.addView(spinner);

        new AlertDialog.Builder(this)
                .setTitle("¿Qué tipo es " + nombreEstancia + "?")
                .setMessage("Elige el tipo que mejor describe esta estancia:")
                .setView(container)
                .setPositiveButton("Añadir", (dialog, which) -> {
                    int selIdx = spinner.getSelectedItemPosition();
                    String tipoElegido = TIPO_OPCIONES_VALOR[selIdx];
                    habitacionesSeleccionadas.add(new Estancia(nombreEstancia, tipoElegido));
                    actualizarListaSeleccionadas();
                    etCustomRoom.setText("");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* ════════════════════════════════════════════════════════
       LISTA DE SELECCIONADAS
    ════════════════════════════════════════════════════════ */

    private void actualizarListaSeleccionadas() {
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
        for (Estancia e : habitacionesSeleccionadas) {
            llSelectedRooms.addView(crearTagSeleccionado(e));
        }
    }

    /** Fila "Nombre del usuario  ·  tipo  ✕" */
    private View crearTagSeleccionado(Estancia estancia) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        filaLp.bottomMargin = dp(6);
        fila.setLayoutParams(filaLp);

        // Nombre visible elegido por el usuario
        TextView tvNombre = new TextView(this);
        tvNombre.setText(estancia.nombre);
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        fila.addView(tvNombre);

        // Tipo (muted, más pequeño)
        TextView tvTipo = new TextView(this);
        tvTipo.setText(estancia.tipo);
        tvTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTipo.setTextColor(getResources().getColor(R.color.muted, null));
        tvTipo.setPadding(0, 0, dp(6), 0);
        fila.addView(tvTipo);

        // Botón eliminar "✕"
        TextView tvRemove = new TextView(this);
        tvRemove.setText("✕");
        tvRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvRemove.setTextColor(getResources().getColor(R.color.danger, null));
        tvRemove.setTypeface(null, Typeface.BOLD);
        tvRemove.setPadding(dp(8), 0, 0, 0);
        tvRemove.setOnClickListener(v -> eliminarSeleccionada(estancia));
        fila.addView(tvRemove);

        return fila;
    }

    private void eliminarSeleccionada(Estancia estancia) {
        habitacionesSeleccionadas.remove(estancia);

        // Desactivar chip si coincide con una sugerencia predefinida
        for (int i = 0; i < SUGERENCIAS_NOMBRE.length; i++) {
            if (SUGERENCIAS_NOMBRE[i].equals(estancia.nombre)
                    && chipViews[i] instanceof TextView) {
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

        AtomicInteger pendiente = new AtomicInteger(habitacionesSeleccionadas.size());
        AtomicInteger errores   = new AtomicInteger(0);

        for (Estancia e : habitacionesSeleccionadas) {
            crearHabitacion(e, pendiente, errores);
        }
    }

    private void crearHabitacion(Estancia estancia,
                                  AtomicInteger pendiente, AtomicInteger errores) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre",   estancia.nombre);
            body.put("tipo",     estancia.tipo);       // valor real del enum
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

            startActivity(new Intent(this, Activity3_gastos_recurrentes.class));
        });
    }

    /* ════════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════════ */

    private Estancia buscarEstanciaPorNombre(String nombre) {
        for (Estancia e : habitacionesSeleccionadas) {
            if (e.nombre.equals(nombre)) return e;
        }
        return null;
    }

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
