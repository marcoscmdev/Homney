package com.homney.app.login_registro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
 *
 * NOVEDAD: Se pueden añadir MÚLTIPLES instancias del mismo tipo.
 * Cada instancia tiene un ALIAS editable (= el nombre guardado en la BD).
 * El tipo enum es interno y no modificable por el usuario desde las sugerencias.
 */
public class Activity2_registro_config_hogar extends AppCompatActivity {

    /* ── Modelo de estancia ─────────────────────────────── */
    private static class Estancia {
        String alias;         // nombre editable por el usuario (guardado en BD como `nombre`)
        final String tipo;    // valor del enum: cocina, aseo, salon…
        Estancia(String alias, String tipo) {
            this.alias = alias;
            this.tipo  = tipo;
        }
    }

    /* ── Sugerencias predefinidas (nombre visible → tipo enum) */
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

    /** Todas las opciones del enum para el selector de tipo personalizado. */
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

    /* ── Vistas ─────────────────────────────────────────── */
    private LinearLayout llChipsFila1, llChipsFila2, llChipsFila3, llChipsFila4;
    private LinearLayout llSelectedRooms;
    private TextView     tvSinSeleccion;
    private EditText     etCustomRoom;
    private Button       btnAddCustom, btnSiguiente;

    /* ── Estado ─────────────────────────────────────────── */
    private final List<Estancia> habitacionesSeleccionadas = new ArrayList<>();
    private final TextView[]     chipLabels = new TextView[SUGERENCIAS_NOMBRE.length];
    private final int[]          chipCounts = new int[SUGERENCIAS_NOMBRE.length]; // instancias activas por chip

    private int  idHogar  = -1;
    private LoadingDialog loadingDialog;

    /* ════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity2_registro_config_hogar);

        // Edge-to-edge forzado en Android 15/16: sin esto el contenido y el botón
        // "Siguiente" quedaban tapados por la status bar / barra de navegación.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View rootConfigHogar = findViewById(R.id.root_config_hogar);
        ViewCompat.setOnApplyWindowInsetsListener(rootConfigHogar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });

        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar = prefs.getInt("id_hogar", -1);
        loadingDialog = new LoadingDialog(this);

        llChipsFila1    = findViewById(R.id.ll_chips_fila1);
        llChipsFila2    = findViewById(R.id.ll_chips_fila2);
        llChipsFila3    = findViewById(R.id.ll_chips_fila3);
        llChipsFila4    = findViewById(R.id.ll_chips_fila4);
        llSelectedRooms = findViewById(R.id.ll_selected_rooms);
        tvSinSeleccion  = findViewById(R.id.tv_sin_seleccion);
        etCustomRoom    = findViewById(R.id.et_custom_room);
        btnAddCustom    = findViewById(R.id.btn_add_custom);
        btnSiguiente    = findViewById(R.id.btn_siguiente);

        construirChips();

        btnAddCustom.setOnClickListener(v -> añadirEstanciaPersonalizada());
        btnSiguiente.setOnClickListener(v -> guardarYContinuar());
    }

    /* ════════════════════════════════════════════════════
       NAVEGACIÓN — sin retroceso durante el registro
    ════════════════════════════════════════════════════ */

    @Override
    public void onBackPressed() {
        Toast.makeText(this, getString(R.string.error_completa_registro), Toast.LENGTH_SHORT).show();
    }

    /* ════════════════════════════════════════════════════
       CHIPS (4 + 4 + 3 + 3)  — cada clic añade una instancia
    ════════════════════════════════════════════════════ */

    private void construirChips() {
        int gap = dp(8);
        LinearLayout[] filas = { llChipsFila1, llChipsFila2, llChipsFila3, llChipsFila4 };

        for (int i = 0; i < SUGERENCIAS_NOMBRE.length; i++) {
            final int idx = i;

            TextView chip = new TextView(this);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setPadding(dp(10), dp(6), dp(10), dp(6));
            chip.setBackground(crearChipDrawable(false));
            chip.setTextColor(getResources().getColor(R.color.text, null));
            actualizarTextoChip(i, chip);  // establece texto inicial

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin  = gap;
            lp.bottomMargin = gap / 2;
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> añadirDesdeChip(idx, chip));

            int fila = i < 4 ? 0 : i < 8 ? 1 : i < 11 ? 2 : 3;
            filas[fila].addView(chip);
            chipLabels[idx] = chip;
        }
    }

    /** Cada clic en un chip AÑADE una nueva instancia (no toggle). */
    private void añadirDesdeChip(int idx, TextView chip) {
        String tipo  = SUGERENCIAS_TIPO[idx];
        chipCounts[idx]++;

        // Alias por defecto: "Dormitorio" si es el primero, "Dormitorio 2" si hay más
        String baseAlias = SUGERENCIAS_NOMBRE[idx];
        String alias = chipCounts[idx] == 1
                ? baseAlias
                : baseAlias + " " + chipCounts[idx];

        habitacionesSeleccionadas.add(new Estancia(alias, tipo));
        actualizarTextoChip(idx, chip);
        actualizarListaSeleccionadas();
    }

    /** Texto del chip: "Cocina" si 0 instancias, "Cocina ×2" si hay 2. */
    private void actualizarTextoChip(int idx, TextView chip) {
        int c = chipCounts[idx];
        String base = SUGERENCIAS_NOMBRE[idx];
        chip.setText(c > 0 ? base + " ×" + c : base);
        // Color del chip: activo (accent) si hay al menos una instancia
        chip.setBackground(crearChipDrawable(c > 0));
        chip.setTextColor(c > 0 ? Color.WHITE
                : getResources().getColor(R.color.text, null));
    }

    /* ════════════════════════════════════════════════════
       CAMPO PERSONALIZADO
    ════════════════════════════════════════════════════ */

    private void añadirEstanciaPersonalizada() {
        String alias = etCustomRoom.getText().toString().trim();
        if (alias.isEmpty()) return;

        String tipoDeducido = resolverTipo(alias);
        if (tipoDeducido != null) {
            habitacionesSeleccionadas.add(new Estancia(alias, tipoDeducido));
            actualizarListaSeleccionadas();
            etCustomRoom.setText("");
        } else {
            mostrarDialogElegirTipo(alias);
        }
    }

    /**
     * Intenta deducir el tipo enum a partir del alias escrito.
     * Devuelve null si no se puede determinar.
     */
    private String resolverTipo(String alias) {
        String norm = alias.toLowerCase(Locale.getDefault()).trim();

        for (String val : TIPO_OPCIONES_VALOR) {
            if (norm.equals(val)) return val;
        }
        for (int i = 0; i < SUGERENCIAS_NOMBRE.length; i++) {
            if (norm.equals(SUGERENCIAS_NOMBRE[i].toLowerCase(Locale.getDefault())))
                return SUGERENCIAS_TIPO[i];
        }

        if (norm.contains("cocin"))                             return "cocina";
        if (norm.contains("baño") || norm.contains("aseo") || norm.contains("bano")) return "aseo";
        if (norm.contains("garaje") || norm.contains("parking"))                     return "garaje";
        if (norm.contains("jard") || norm.contains("patio") || norm.contains("exterior")) return "exterior";
        if (norm.contains("dormit") || norm.contains("habitaci") ||
            norm.contains("cuarto") || norm.contains("alcoba"))                      return "dormitorio";
        if (norm.contains("infantil") || norm.contains("niño") || norm.contains("nino")) return "infantil";
        if (norm.contains("comedor"))                           return "comedor";
        if (norm.contains("salon") || norm.contains("salón") || norm.contains("living")) return "salon";
        if (norm.contains("oficin") || norm.contains("despacho") || norm.contains("estudio")) return "oficina";
        if (norm.contains("trastero") || norm.contains("almacen") || norm.contains("almacén")) return "trastero";
        if (norm.contains("recibidor") || norm.contains("pasillo") ||
            norm.contains("entrada") || norm.contains("hall"))                       return "recibidor";
        if (norm.contains("terraza") || norm.contains("balcon") || norm.contains("balcón")) return "terraza";
        if (norm.contains("gym") || norm.contains("gimnasio") || norm.contains("deport")) return "deportiva";
        return null;
    }

    /** Diálogo para que el usuario elija el tipo de una estancia personalizada. */
    private void mostrarDialogElegirTipo(String aliasEstancia) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TIPO_OPCIONES_NOMBRE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), dp(4));
        container.addView(spinner);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_tipo_estancia_titulo, aliasEstancia))
                .setMessage(getString(R.string.dialog_tipo_estancia_msg))
                .setView(container)
                .setPositiveButton(getString(R.string.a_adir), (dialog, which) -> {
                    int selIdx = spinner.getSelectedItemPosition();
                    habitacionesSeleccionadas.add(
                            new Estancia(aliasEstancia, TIPO_OPCIONES_VALOR[selIdx]));
                    actualizarListaSeleccionadas();
                    etCustomRoom.setText("");
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    /* ════════════════════════════════════════════════════
       LISTA DE SELECCIONADAS
    ════════════════════════════════════════════════════ */

    private void actualizarListaSeleccionadas() {
        // Limpiar vistas (menos tvSinSeleccion)
        for (int i = llSelectedRooms.getChildCount() - 1; i >= 0; i--) {
            View v = llSelectedRooms.getChildAt(i);
            if (v != tvSinSeleccion) llSelectedRooms.removeViewAt(i);
        }

        if (habitacionesSeleccionadas.isEmpty()) {
            tvSinSeleccion.setVisibility(View.VISIBLE);
            return;
        }
        tvSinSeleccion.setVisibility(View.GONE);

        for (int pos = 0; pos < habitacionesSeleccionadas.size(); pos++) {
            llSelectedRooms.addView(crearTagSeleccionado(pos));
        }
    }

    /**
     * Fila editable:
     *  [EditText alias] [tipo muted] [✕]
     * El EditText está conectado via TextWatcher a estancia.alias.
     */
    private View crearTagSeleccionado(final int pos) {
        Estancia estancia = habitacionesSeleccionadas.get(pos);

        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        filaLp.bottomMargin = dp(6);
        fila.setLayoutParams(filaLp);

        // EditText del alias
        EditText etAlias = new EditText(this);
        etAlias.setText(estancia.alias);
        etAlias.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etAlias.setTextColor(getResources().getColor(R.color.text, null));
        etAlias.setHint(getString(R.string.hint_alias_estancia));
        etAlias.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etAlias.setBackground(null); // sin borde por defecto, aspecto limpio
        etAlias.setPadding(0, 0, dp(6), 0);
        etAlias.setMaxLines(1);
        etAlias.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // TextWatcher: actualiza el alias de la Estancia en tiempo real
        etAlias.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                // Comprobar que la posición sigue siendo válida
                if (pos < habitacionesSeleccionadas.size()) {
                    habitacionesSeleccionadas.get(pos).alias = s.toString().trim();
                }
            }
        });
        fila.addView(etAlias);

        // Tipo (muted)
        TextView tvTipo = new TextView(this);
        tvTipo.setText(estancia.tipo);
        tvTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvTipo.setTextColor(getResources().getColor(R.color.muted, null));
        tvTipo.setPadding(0, 0, dp(6), 0);
        fila.addView(tvTipo);

        // Botón ✕
        TextView tvRemove = new TextView(this);
        tvRemove.setText("✕");
        tvRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvRemove.setTextColor(getResources().getColor(R.color.danger, null));
        tvRemove.setTypeface(null, Typeface.BOLD);
        tvRemove.setPadding(dp(8), 0, 0, 0);
        tvRemove.setOnClickListener(v -> eliminarSeleccionada(pos, estancia));
        fila.addView(tvRemove);

        return fila;
    }

    private void eliminarSeleccionada(int pos, Estancia estancia) {
        if (pos < 0 || pos >= habitacionesSeleccionadas.size()) return;
        habitacionesSeleccionadas.remove(pos);

        // Si era un chip sugerido, decrementar el contador del chip
        for (int i = 0; i < SUGERENCIAS_TIPO.length; i++) {
            if (SUGERENCIAS_TIPO[i].equals(estancia.tipo)) {
                // Comprobar si la raíz del alias coincide (chip o personalizado)
                if (chipCounts[i] > 0) {
                    chipCounts[i]--;
                    actualizarTextoChip(i, chipLabels[i]);
                    break;
                }
            }
        }
        actualizarListaSeleccionadas();
    }

    /* ════════════════════════════════════════════════════
       GUARDAR Y CONTINUAR → Activity2b (tareas)
    ════════════════════════════════════════════════════ */

    private void guardarYContinuar() {
        if (habitacionesSeleccionadas.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_min_una_estancia), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que ningún alias esté vacío
        for (Estancia e : habitacionesSeleccionadas) {
            if (e.alias == null || e.alias.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_alias_vacio),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (idHogar == -1) {
            Toast.makeText(this, getString(R.string.error_hogar_no_identificado), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Utilidades.hayConexionInternet(this)) {
            Toast.makeText(this, getString(R.string.sin_conexion_internet), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSiguiente.setEnabled(false);
        btnSiguiente.setText(getString(R.string.guardando));
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
            body.put("nombre",   estancia.alias);   // alias = nombre visible en la BD
            body.put("tipo",     estancia.tipo);     // valor real del enum
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
            btnSiguiente.setText(getString(R.string.siguiente));

            if (errores > 0) {
                Toast.makeText(this,
                        errores + " estancia(s) no pudieron guardarse. Puedes añadirlas después.",
                        Toast.LENGTH_LONG).show();
            }
            startActivity(new Intent(this, Activity2b_tareas_hogar.class));
        });
    }

    /* ════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════ */

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
