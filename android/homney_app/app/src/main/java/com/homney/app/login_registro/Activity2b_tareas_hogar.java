package com.homney.app.login_registro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PASO 2b del wizard de configuración del hogar.
 * Intermedio entre Activity2 (habitaciones) y Activity3 (gastos recurrentes).
 *
 * Carga las habitaciones ya creadas y muestra sugerencias de tareas comunes
 * según el tipo de cada habitación. El usuario puede:
 *  - Marcar/desmarcar las sugerencias predefinidas (vienen marcadas por defecto).
 *  - Añadir tareas personalizadas por habitación.
 *  - Omitir el paso si no quiere configurar tareas ahora.
 *
 * Al pulsar "Siguiente" crea las tareas seleccionadas vía API y pasa a Activity3.
 */
public class Activity2b_tareas_hogar extends AppCompatActivity {

    /* ══════════════════════════════════════════════════════════════
       MODELO DE SUGERENCIA
    ══════════════════════════════════════════════════════════════ */

    private static class TareaSugest {
        final String nombre;
        final String frecuencia; // "dia" | "semana" | "mes"
        final int    numVeces;

        TareaSugest(String nombre, String frecuencia, int numVeces) {
            this.nombre    = nombre;
            this.frecuencia = frecuencia;
            this.numVeces   = numVeces;
        }
    }

    /* ── Sugerencias por tipo de habitación (mismo enum de la BD) ── */
    private static final Map<String, TareaSugest[]> SUGEST_POR_TIPO = new HashMap<>();
    static {
        SUGEST_POR_TIPO.put("cocina", new TareaSugest[]{
            new TareaSugest("Limpiar cocina",             "semana", 1),
            new TareaSugest("Fregar suelo",               "semana", 1),
            new TareaSugest("Vaciar basura",              "semana", 2),
            new TareaSugest("Desinfectar superficies",    "dia",    1),
            new TareaSugest("Limpiar campana extractora", "mes",    1),
            new TareaSugest("Organizar despensa",         "mes",    1),
        });
        SUGEST_POR_TIPO.put("aseo", new TareaSugest[]{
            new TareaSugest("Limpiar baño",          "semana", 1),
            new TareaSugest("Fregar suelo",          "semana", 1),
            new TareaSugest("Desinfectar inodoro",   "semana", 1),
            new TareaSugest("Limpiar espejo",        "semana", 1),
            new TareaSugest("Cambiar toallas",       "semana", 1),
            new TareaSugest("Rellenar productos",    "mes",    1),
        });
        SUGEST_POR_TIPO.put("dormitorio", new TareaSugest[]{
            new TareaSugest("Hacer la cama",        "dia",    1),
            new TareaSugest("Cambiar sábanas",      "semana", 1),
            new TareaSugest("Limpiar polvo",        "semana", 1),
            new TareaSugest("Ventilar habitación",  "dia",    1),
            new TareaSugest("Pasar aspiradora",     "semana", 1),
            new TareaSugest("Ordenar armario",      "mes",    1),
        });
        SUGEST_POR_TIPO.put("salon", new TareaSugest[]{
            new TareaSugest("Limpiar salón",   "semana", 1),
            new TareaSugest("Aspirar sofá",    "semana", 1),
            new TareaSugest("Fregar suelo",    "semana", 1),
            new TareaSugest("Limpiar polvo",   "semana", 1),
            new TareaSugest("Limpiar TV",      "semana", 1),
        });
        SUGEST_POR_TIPO.put("comedor", new TareaSugest[]{
            new TareaSugest("Limpiar comedor", "semana", 1),
            new TareaSugest("Fregar suelo",    "semana", 1),
            new TareaSugest("Limpiar mesa",    "dia",    1),
            new TareaSugest("Limpiar sillas",  "semana", 1),
        });
        SUGEST_POR_TIPO.put("garaje", new TareaSugest[]{
            new TareaSugest("Barrer garaje",         "semana", 1),
            new TareaSugest("Ordenar garaje",        "mes",    1),
            new TareaSugest("Revisar aceite coche",  "mes",    1),
            new TareaSugest("Limpiar garaje",        "mes",    1),
        });
        SUGEST_POR_TIPO.put("exterior", new TareaSugest[]{
            new TareaSugest("Barrer patio",    "semana", 1),
            new TareaSugest("Regar plantas",   "dia",    1),
            new TareaSugest("Recoger hojas",   "semana", 1),
            new TareaSugest("Limpiar exterior","mes",    1),
        });
        SUGEST_POR_TIPO.put("terraza", new TareaSugest[]{
            new TareaSugest("Barrer terraza",  "semana", 1),
            new TareaSugest("Regar plantas",   "dia",    1),
            new TareaSugest("Limpiar terraza", "mes",    1),
            new TareaSugest("Lavar exterior",  "mes",    1),
        });
        SUGEST_POR_TIPO.put("infantil", new TareaSugest[]{
            new TareaSugest("Ordenar cuarto",      "dia",    1),
            new TareaSugest("Cambiar sábanas",     "semana", 1),
            new TareaSugest("Limpiar juguetes",    "semana", 1),
            new TareaSugest("Ventilar habitación", "dia",    1),
            new TareaSugest("Pasar aspiradora",    "semana", 1),
        });
        SUGEST_POR_TIPO.put("oficina", new TareaSugest[]{
            new TareaSugest("Ordenar escritorio",   "semana", 1),
            new TareaSugest("Limpiar pantalla",     "semana", 1),
            new TareaSugest("Organizar documentos", "mes",    1),
            new TareaSugest("Limpiar polvo",        "semana", 1),
        });
        SUGEST_POR_TIPO.put("trastero", new TareaSugest[]{
            new TareaSugest("Ordenar trastero", "mes", 1),
            new TareaSugest("Limpiar trastero", "mes", 1),
            new TareaSugest("Revisar cajas",    "mes", 1),
        });
        SUGEST_POR_TIPO.put("recibidor", new TareaSugest[]{
            new TareaSugest("Barrer entrada",  "semana", 1),
            new TareaSugest("Limpiar felpudo", "semana", 1),
            new TareaSugest("Ordenar zapatos", "semana", 1),
        });
        SUGEST_POR_TIPO.put("deportiva", new TareaSugest[]{
            new TareaSugest("Limpiar zona deportiva", "semana", 1),
            new TareaSugest("Ordenar equipamiento",   "semana", 1),
            new TareaSugest("Limpiar colchonetas",    "semana", 1),
        });
        SUGEST_POR_TIPO.put("generica", new TareaSugest[]{
            new TareaSugest("Limpiar habitación", "semana", 1),
            new TareaSugest("Ventilar",           "dia",    1),
            new TareaSugest("Barrer suelo",       "semana", 1),
            new TareaSugest("Ordenar",            "semana", 1),
        });
    }

    /* ══════════════════════════════════════════════════════════════
       MODELOS INTERNOS
    ══════════════════════════════════════════════════════════════ */

    /** Representa un ítem de tarea en la lista de una sección (sugerida o custom). */
    private static class TareaItem {
        String  nombre;
        String  frecuencia;
        int     numVeces;
        boolean esCustom;
        CheckBox checkBox; // referencia al CB de la fila (null para custom)

        TareaItem(String nombre, String frecuencia, int numVeces, boolean esCustom) {
            this.nombre    = nombre;
            this.frecuencia = frecuencia;
            this.numVeces   = numVeces;
            this.esCustom   = esCustom;
        }
    }

    /** Agrupa los datos de una habitación y sus ítems de tarea. */
    private static class SeccionHabitacion {
        final int    idHabitacion;
        final String nombre;
        final String tipo;
        final List<TareaItem> tareas = new ArrayList<>();

        SeccionHabitacion(int idHabitacion, String nombre, String tipo) {
            this.idHabitacion = idHabitacion;
            this.nombre       = nombre;
            this.tipo         = tipo;
        }
    }

    /* ══════════════════════════════════════════════════════════════
       CAMPOS
    ══════════════════════════════════════════════════════════════ */

    private int           idHogar   = -1;
    private int           idUsuario = -1;
    private LinearLayout  llSecciones;
    private LoadingDialog loadingDialog;

    private final List<SeccionHabitacion> secciones = new ArrayList<>();

    /* ══════════════════════════════════════════════════════════════
       CICLO DE VIDA
    ══════════════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity2b_tareas_hogar);

        // Edge-to-edge forzado en Android 15/16: la barra superior del wizard y los
        // botones inferiores deben apartarse de la status bar / barra de navegación.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View barraSuperior = findViewById(R.id.barra_superior_tareas);
        View barraInferior = findViewById(R.id.barra_inferior_tareas);
        int barraSuperiorPaddingBase = barraSuperior.getPaddingTop();
        int barraInferiorPaddingBase = barraInferior.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            barraSuperior.setPadding(barraSuperior.getPaddingLeft(), barraSuperiorPaddingBase + bars.top,
                    barraSuperior.getPaddingRight(), barraSuperior.getPaddingBottom());
            barraInferior.setPadding(barraInferior.getPaddingLeft(), barraInferior.getPaddingTop(),
                    barraInferior.getPaddingRight(), barraInferiorPaddingBase + bars.bottom);
            return windowInsets;
        });

        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar   = prefs.getInt("id_hogar",   -1);
        idUsuario = prefs.getInt("id_usuario", -1);

        loadingDialog = new LoadingDialog(this);
        llSecciones   = findViewById(R.id.ll_secciones_habitaciones);

        findViewById(R.id.btn_siguiente_tareas).setOnClickListener(v -> guardarYContinuar());
        findViewById(R.id.btn_omitir_tareas).setOnClickListener(v -> {
            getSharedPreferences("registro_wizard", MODE_PRIVATE)
                    .edit().remove("registro_tareas").apply();
            irAGastos();
        });
        cargarHabitaciones();
    }

    /* ══════════════════════════════════════════════════════════════
       CARGA DE HABITACIONES DESDE LA API
    ══════════════════════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        if (idHogar == -1) { mostrarSinHabitaciones(); return; }

        loadingDialog.show();
        String url = WebService.URL_Habitacion + "?id_hogar=" + idHogar;

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    loadingDialog.dismiss();
                    try {
                        if (WebService.JSON.SUCCESS.equals(
                                response.getString(WebService.JSON.STATUS))) {
                            JSONArray data = response.getJSONArray(WebService.JSON.DATA);
                            runOnUiThread(() -> construirSecciones(data));
                        } else {
                            runOnUiThread(this::mostrarSinHabitaciones);
                        }
                    } catch (JSONException e) {
                        runOnUiThread(this::mostrarSinHabitaciones);
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    runOnUiThread(this::mostrarSinHabitaciones);
                }
        ));
    }

    private void mostrarSinHabitaciones() {
        llSecciones.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(getString(R.string.no_habitaciones_wizard));
        tv.setTextColor(getResources().getColor(R.color.muted, null));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(20), dp(32), dp(20), dp(16));
        llSecciones.addView(tv);
    }

    /* ══════════════════════════════════════════════════════════════
       CONSTRUCCIÓN DINÁMICA DE SECCIONES
    ══════════════════════════════════════════════════════════════ */

    private void construirSecciones(JSONArray data) {
        llSecciones.removeAllViews();
        secciones.clear();

        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject hab      = data.getJSONObject(i);
                int    id           = hab.getInt("id_habitacion");
                String nombre       = hab.optString("nombre", "Habitación");
                String tipo         = hab.optString("tipo",   "generica");

                SeccionHabitacion sec = new SeccionHabitacion(id, nombre, tipo);

                // Sugerencias según tipo (fallback a generica)
                TareaSugest[] sugest = SUGEST_POR_TIPO.containsKey(tipo)
                        ? SUGEST_POR_TIPO.get(tipo)
                        : SUGEST_POR_TIPO.get("generica");

                if (sugest != null) {
                    for (TareaSugest ts : sugest) {
                        sec.tareas.add(new TareaItem(ts.nombre, ts.frecuencia, ts.numVeces, false));
                    }
                }

                secciones.add(sec);
                llSecciones.addView(crearVistaSeccion(sec));

            } catch (JSONException ignored) {}
        }

        if (secciones.isEmpty()) mostrarSinHabitaciones();
    }

    /* ══════════════════════════════════════════════════════════════
       VISTAS: SECCIÓN POR HABITACIÓN
    ══════════════════════════════════════════════════════════════ */

    private View crearVistaSeccion(SeccionHabitacion sec) {
        // ── Tarjeta contenedor ───────────────────────────────
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(getResources().getDrawable(R.drawable.bg_auth_box, null));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(14);
        card.setLayoutParams(cardLp);

        // ── Cabecera de la habitación ────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hLp.bottomMargin = dp(10);
        header.setLayoutParams(hLp);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emojiPorTipo(sec.tipo));
        tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tvEmoji.setPadding(0, 0, dp(10), 0);
        header.addView(tvEmoji);

        TextView tvNom = new TextView(this);
        tvNom.setText(sec.nombre);
        tvNom.setTextColor(getResources().getColor(R.color.text, null));
        tvNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvNom.setTypeface(null, Typeface.BOLD);
        tvNom.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(tvNom);

        // Tipo como badge gris
        TextView tvTipo = new TextView(this);
        tvTipo.setText(sec.tipo);
        tvTipo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tvTipo.setTextColor(getResources().getColor(R.color.muted, null));
        GradientDrawable tipoBg = new GradientDrawable();
        tipoBg.setCornerRadius(dp(10));
        tipoBg.setColor(getResources().getColor(R.color.surface_two, null));
        tvTipo.setBackground(tipoBg);
        tvTipo.setPadding(dp(7), dp(3), dp(7), dp(3));
        header.addView(tvTipo);

        card.addView(header);

        // Separador
        View sep = new View(this);
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        sepLp.bottomMargin = dp(10);
        sep.setLayoutParams(sepLp);
        sep.setBackgroundColor(getResources().getColor(R.color.border, null));
        card.addView(sep);

        // ── Lista de tareas ──────────────────────────────────
        LinearLayout llTareas = new LinearLayout(this);
        llTareas.setOrientation(LinearLayout.VERTICAL);
        llTareas.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        for (TareaItem item : sec.tareas) {
            llTareas.addView(crearFilaTarea(item));
        }
        card.addView(llTareas);

        // ── Zona de tarea personalizada ──────────────────────
        card.addView(crearZonaCustom(sec, llTareas));

        return card;
    }

    /* ══════════════════════════════════════════════════════════════
       FILA DE TAREA SUGERIDA
    ══════════════════════════════════════════════════════════════ */

    private View crearFilaTarea(TareaItem item) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);
        fila.setLayoutParams(lp);

        CheckBox cb = new CheckBox(this);
        cb.setText(item.nombre);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        cb.setChecked(true);
        cb.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        item.checkBox = cb;
        fila.addView(cb);

        // Badge de frecuencia — pulsable para cambiarla
        TextView tvFreq = new TextView(this);
        tvFreq.setText(frecuenciaLabel(item.frecuencia));
        tvFreq.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFreq.setTextColor(getResources().getColor(R.color.accent_dark, null));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(10));
        badgeBg.setColor(getResources().getColor(R.color.accent_background, null));
        tvFreq.setBackground(badgeBg);
        tvFreq.setPadding(dp(8), dp(3), dp(8), dp(3));
        tvFreq.setClickable(true);
        tvFreq.setFocusable(true);
        tvFreq.setOnClickListener(v -> mostrarSelectorFrecuencia(item, tvFreq));
        fila.addView(tvFreq);

        return fila;
    }

    /* ══════════════════════════════════════════════════════════════
       DIÁLOGO CAMBIO DE FRECUENCIA (tareas sugeridas)
    ══════════════════════════════════════════════════════════════ */

    private void mostrarSelectorFrecuencia(TareaItem item, TextView tvBadge) {
        String[] labels = {"Diario", "Semanal", "Mensual"};
        String[] values = {"dia", "semana", "mes"};
        int cur = 1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(item.frecuencia)) { cur = i; break; }
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_cambiar_frecuencia_titulo))
                .setSingleChoiceItems(labels, cur, (d, which) -> {
                    item.frecuencia = values[which];
                    tvBadge.setText(labels[which]);
                    d.dismiss();
                })
                .show();
    }

    /* ══════════════════════════════════════════════════════════════
       ZONA DE TAREA PERSONALIZADA
    ══════════════════════════════════════════════════════════════ */

    private View crearZonaCustom(SeccionHabitacion sec, LinearLayout llTareas) {
        final String[] freqSel = {"semana"}; // frecuencia seleccionada por el usuario

        LinearLayout zona = new LinearLayout(this);
        zona.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams zonaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        zonaLp.topMargin = dp(10);
        zona.setLayoutParams(zonaLp);

        // ── Fila 1: input + botón ＋ ──────────────────────────
        LinearLayout filaInput = new LinearLayout(this);
        filaInput.setOrientation(LinearLayout.HORIZONTAL);
        filaInput.setGravity(Gravity.CENTER_VERTICAL);
        filaInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        EditText etNombre = new EditText(this);
        etNombre.setHint(getString(R.string.hint_tarea_personalizada));
        etNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etNombre.setBackground(crearBordeInput());
        etNombre.setPadding(dp(10), dp(8), dp(10), dp(8));
        etNombre.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etLp.rightMargin = dp(8);
        etNombre.setLayoutParams(etLp);
        filaInput.addView(etNombre);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(8));
        btnBg.setColor(getResources().getColor(R.color.accent, null));

        Button btnAdd = new Button(this);
        btnAdd.setText("＋");
        btnAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        btnAdd.setBackground(btnBg);
        btnAdd.setTextColor(getResources().getColor(R.color.text, null));
        btnAdd.setPadding(dp(14), 0, dp(14), 0);
        btnAdd.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)));
        filaInput.addView(btnAdd);
        zona.addView(filaInput);

        // ── Fila 2: chips de periodicidad ─────────────────────
        LinearLayout llChips = new LinearLayout(this);
        llChips.setOrientation(LinearLayout.HORIZONTAL);
        llChips.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams chipsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        chipsLp.topMargin = dp(6);
        llChips.setLayoutParams(chipsLp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(getString(R.string.periodicidad_label));
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabel.setTextColor(getResources().getColor(R.color.muted, null));
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lblLp.rightMargin = dp(4);
        tvLabel.setLayoutParams(lblLp);
        llChips.addView(tvLabel);

        String[] freqLabels = {"Diario", "Semanal", "Mensual"};
        String[] freqValues = {"dia",    "semana",  "mes"};
        TextView[] chips    = new TextView[3];

        for (int i = 0; i < 3; i++) {
            final int fi = i;
            chips[i] = new TextView(this);
            chips[i].setText(freqLabels[i]);
            chips[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chips[i].setPadding(dp(10), dp(5), dp(10), dp(5));
            chips[i].setClickable(true);
            chips[i].setFocusable(true);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            chipLp.rightMargin = dp(4);
            chips[i].setLayoutParams(chipLp);
            actualizarEstiloChip(chips[i], i == 1); // Semanal por defecto
            chips[i].setOnClickListener(v -> {
                freqSel[0] = freqValues[fi];
                for (int j = 0; j < 3; j++) actualizarEstiloChip(chips[j], j == fi);
            });
            llChips.addView(chips[i]);
        }
        zona.addView(llChips);

        // ── Listener del botón ────────────────────────────────
        btnAdd.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_escribe_nombre_tarea), Toast.LENGTH_SHORT).show();
                return;
            }
            TareaItem custom = new TareaItem(nombre, freqSel[0], 1, true);
            sec.tareas.add(custom);
            llTareas.addView(crearTagCustom(custom, sec, llTareas));
            etNombre.setText("");
        });

        return zona;
    }

    /** Aplica el estilo seleccionado/no-seleccionado al chip de frecuencia. */
    private void actualizarEstiloChip(TextView chip, boolean seleccionado) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        if (seleccionado) {
            bg.setColor(getResources().getColor(R.color.accent, null));
            chip.setTextColor(getResources().getColor(R.color.text, null));
            chip.setTypeface(null, Typeface.BOLD);
        } else {
            bg.setColor(getResources().getColor(R.color.surface_two, null));
            chip.setTextColor(getResources().getColor(R.color.muted, null));
            chip.setTypeface(null, Typeface.NORMAL);
        }
        chip.setBackground(bg);
    }

    /** Tag visual amarillo para tareas custom, con botón de eliminar. */
    private View crearTagCustom(TareaItem item, SeccionHabitacion sec, LinearLayout llTareas) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setBackgroundColor(getResources().getColor(R.color.accent_background, null));
        fila.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(4);
        lp.topMargin    = dp(2);
        fila.setLayoutParams(lp);

        TextView tvNom = new TextView(this);
        tvNom.setText("✓  " + item.nombre);
        tvNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNom.setTextColor(getResources().getColor(R.color.text, null));
        tvNom.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        fila.addView(tvNom);

        TextView tvSemanal = new TextView(this);
        tvSemanal.setText(frecuenciaLabel(item.frecuencia));
        tvSemanal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSemanal.setTextColor(getResources().getColor(R.color.accent_dark, null));
        tvSemanal.setPadding(0, 0, dp(8), 0);
        fila.addView(tvSemanal);

        TextView tvRm = new TextView(this);
        tvRm.setText("✕");
        tvRm.setTextColor(getResources().getColor(R.color.danger, null));
        tvRm.setTypeface(null, Typeface.BOLD);
        tvRm.setPadding(dp(4), 0, 0, 0);
        tvRm.setOnClickListener(v -> {
            sec.tareas.remove(item);
            llTareas.removeView(fila);
        });
        fila.addView(tvRm);

        return fila;
    }

    /* ══════════════════════════════════════════════════════════════
       NAVEGACIÓN — sin retroceso durante el registro
    ══════════════════════════════════════════════════════════════ */

    @Override
    public void onBackPressed() {
        Toast.makeText(this, getString(R.string.error_completa_registro), Toast.LENGTH_SHORT).show();
    }

    /* ══════════════════════════════════════════════════════════════
       GUARDAR Y CONTINUAR
       Las tareas se guardan localmente; se enviarán a la BD al
       confirmar el registro en Activity4.
    ══════════════════════════════════════════════════════════════ */

    private void guardarYContinuar() {
        JSONArray tareasJson = new JSONArray();
        for (SeccionHabitacion sec : secciones) {
            for (TareaItem item : sec.tareas) {
                boolean incluir = item.esCustom
                        || (item.checkBox != null && item.checkBox.isChecked());
                if (incluir) {
                    try {
                        JSONObject t = new JSONObject();
                        t.put("nombre",        item.nombre);
                        t.put("frecuencia",    item.frecuencia);
                        t.put("num_veces",     item.numVeces);
                        t.put("id_habitacion", sec.idHabitacion);
                        tareasJson.put(t);
                    } catch (JSONException ignored) {}
                }
            }
        }

        getSharedPreferences("registro_wizard", MODE_PRIVATE)
                .edit()
                .putString("registro_tareas", tareasJson.toString())
                .apply();

        irAGastos();
    }

    private void irAGastos() {
        startActivity(new Intent(this, Activity3_gastos_recurrentes.class));
    }

    /* ══════════════════════════════════════════════════════════════
       HELPERS
    ══════════════════════════════════════════════════════════════ */

    private String emojiPorTipo(String tipo) {
        switch (tipo) {
            case "cocina":    return "🍳";
            case "aseo":      return "🚿";
            case "garaje":    return "🚗";
            case "exterior":  return "🌿";
            case "dormitorio":return "🛏️";
            case "infantil":  return "🧸";
            case "comedor":   return "🍽️";
            case "salon":     return "🛋️";
            case "oficina":   return "💼";
            case "trastero":  return "📦";
            case "recibidor": return "🚪";
            case "terraza":   return "☀️";
            case "deportiva": return "🏋️";
            default:          return "🏠";
        }
    }

    private String frecuenciaLabel(String f) {
        switch (f) {
            case "dia":    return "Diario";
            case "semana": return "Semanal";
            case "mes":    return "Mensual";
            default:       return "Variable";
        }
    }

    private GradientDrawable crearBordeInput() {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(4));
        gd.setStroke(dp(1), getResources().getColor(R.color.border, null));
        gd.setColor(getResources().getColor(R.color.surface, null));
        return gd;
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}
