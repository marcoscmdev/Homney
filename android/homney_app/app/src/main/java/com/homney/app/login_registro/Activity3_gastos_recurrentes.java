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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PASO 2 del wizard de configuración del hogar.
 * Equivale al Paso 3 del wizard web: gastos recurrentes sugeridos (alquiler, luz, agua…)
 * con importes editables + campo personalizado.
 *
 * Al pulsar "Comenzar" se crean los gastos vía API gasto.php y se lanza MainActivity.
 */
public class Activity3_gastos_recurrentes extends AppCompatActivity {

    /* ═══════════════════════════════════════════════════════
       Gasto sugerido (nombre + importe por defecto + modo de pago)
       Igual que EXP_SUGG de la web
    ═══════════════════════════════════════════════════════ */
    private static class GastoSugest {
        final String nombre;
        final double importeDefault;
        final String modo;
        final String categoria;
        GastoSugest(String nombre, double importeDefault, String modo, String categoria) {
            this.nombre          = nombre;
            this.importeDefault  = importeDefault;
            this.modo            = modo;
            this.categoria       = categoria;
        }
    }

    private static final GastoSugest[] SUGERENCIAS = {
        new GastoSugest("Alquiler",  700, "transferencia", "Alquiler / Hipoteca"),
        new GastoSugest("Luz",        80, "transferencia", "Suministros"),
        new GastoSugest("Agua",       35, "transferencia", "Suministros"),
        new GastoSugest("Gas",        55, "transferencia", "Suministros"),
        new GastoSugest("Internet",   45, "transferencia", "Suministros"),
        new GastoSugest("Netflix",    13, "tarjeta",       "Ocio / Entretenimiento"),
        new GastoSugest("Spotify",    11, "tarjeta",       "Ocio / Entretenimiento"),
        new GastoSugest("Comunidad",  60, "transferencia", "Alquiler / Hipoteca"),
    };

    /* ── Vistas ─────────────────────────────────────────── */
    private LinearLayout llGastosTabla;
    private TextView     tvResumen;
    private EditText     etCustomNombre, etCustomImporte;
    private Button       btnAddGasto, btnComenzar, btnAtras;

    /* ── Estado ─────────────────────────────────────────── */
    /** CheckBoxes de las sugerencias (índice alineado con SUGERENCIAS). */
    private final CheckBox[] checkBoxes     = new CheckBox[SUGERENCIAS.length];
    /** EditTexts de importes de las sugerencias. */
    private final EditText[] etImportes     = new EditText[SUGERENCIAS.length];
    /** Gastos personalizados añadidos por el usuario. */
    private final List<double[]> gastosCustom = new ArrayList<>(); // [0]=importe
    private final List<String>   nombresCustom = new ArrayList<>();

    private int    idHogar    = -1;
    private int    idUsuario  = -1;
    private LoadingDialog loadingDialog;
    private static final String TAG = "WZ_GASTOS";

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity3_gastos_recurrentes);

        // Sesión
        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar   = prefs.getInt("id_hogar",   -1);
        idUsuario = prefs.getInt("id_usuario", -1);

        loadingDialog = new LoadingDialog(this);

        // Vistas
        llGastosTabla   = findViewById(R.id.ll_gastos_tabla);
        tvResumen       = findViewById(R.id.tv_resumen_gastos);
        etCustomNombre  = findViewById(R.id.et_custom_nombre);
        etCustomImporte = findViewById(R.id.et_custom_importe);
        btnAddGasto     = findViewById(R.id.btn_add_gasto);
        btnComenzar     = findViewById(R.id.btn_comenzar);
        btnAtras        = findViewById(R.id.btn_atras);

        construirTabla();

        btnAddGasto.setOnClickListener(v -> añadirGastoPersonalizado());
        btnComenzar.setOnClickListener(v -> guardarYContinuar());
        btnAtras.setOnClickListener(v -> finish()); // vuelve a Activity2
    }

    /* ════════════════════════════════════════════════════════
       TABLA DE GASTOS SUGERIDOS
       Cada fila: [CheckBox nombre] [EditText importe]
    ════════════════════════════════════════════════════════ */

    private void construirTabla() {
        int gap = dp(6);

        for (int i = 0; i < SUGERENCIAS.length; i++) {
            final int idx = i;
            GastoSugest g = SUGERENCIAS[i];

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(dp(10), dp(8), dp(10), dp(8));
            // Fondo alternado
            fila.setBackgroundColor(i % 2 == 0
                    ? getResources().getColor(R.color.surface, null)
                    : getResources().getColor(R.color.surface_two, null));

            LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            filaLp.bottomMargin = dp(1);
            fila.setLayoutParams(filaLp);

            // CheckBox + nombre
            CheckBox cb = new CheckBox(this);
            cb.setText(g.nombre);
            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            cb.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f));
            cb.setOnCheckedChangeListener((v, checked) -> actualizarResumen());
            checkBoxes[idx] = cb;
            fila.addView(cb);

            // EditText importe
            EditText etImporte = new EditText(this);
            etImporte.setText(formatImporte(g.importeDefault));
            etImporte.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            etImporte.setGravity(Gravity.END);
            etImporte.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                    | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etImporte.setBackground(crearBordeInput());
            etImporte.setPadding(dp(6), dp(4), dp(6), dp(4));
            LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
            etLp.leftMargin = dp(8);
            etImporte.setLayoutParams(etLp);
            etImportes[idx] = etImporte;
            fila.addView(etImporte);

            llGastosTabla.addView(fila);
        }

        actualizarResumen();
    }

    /* ════════════════════════════════════════════════════════
       GASTO PERSONALIZADO
    ════════════════════════════════════════════════════════ */

    private void añadirGastoPersonalizado() {
        String nombre  = etCustomNombre.getText().toString().trim();
        String impStr  = etCustomImporte.getText().toString().trim();

        if (nombre.isEmpty() || impStr.isEmpty()) {
            Toast.makeText(this, "Completa nombre e importe", Toast.LENGTH_SHORT).show();
            return;
        }

        double importe;
        try { importe = Double.parseDouble(impStr); }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Importe no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        nombresCustom.add(nombre);
        gastosCustom.add(new double[]{importe});

        // Añadir tag visual
        llGastosTabla.addView(crearTagCustom(nombre, importe, nombresCustom.size() - 1));
        etCustomNombre.setText("");
        etCustomImporte.setText("");
        actualizarResumen();
    }

    private View crearTagCustom(String nombre, double importe, int indexCustom) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(dp(10), dp(8), dp(10), dp(8));
        fila.setBackgroundColor(getResources().getColor(R.color.accent_background, null));

        LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        filaLp.bottomMargin = dp(1);
        fila.setLayoutParams(filaLp);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre + "  —  " + formatImporte(importe) + " €");
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNombre.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        fila.addView(tvNombre);

        TextView tvRm = new TextView(this);
        tvRm.setText("✕");
        tvRm.setTextColor(getResources().getColor(R.color.danger, null));
        tvRm.setTypeface(null, Typeface.BOLD);
        tvRm.setPadding(dp(8), 0, 0, 0);
        tvRm.setOnClickListener(v -> {
            nombresCustom.set(indexCustom, null); // marcar como eliminado
            gastosCustom.set(indexCustom, null);
            llGastosTabla.removeView(fila);
            actualizarResumen();
        });
        fila.addView(tvRm);

        return fila;
    }

    /* ════════════════════════════════════════════════════════
       RESUMEN
    ════════════════════════════════════════════════════════ */

    private void actualizarResumen() {
        int total = 0;
        for (CheckBox cb : checkBoxes) { if (cb != null && cb.isChecked()) total++; }
        for (String n : nombresCustom) { if (n != null) total++; }
        tvResumen.setText(total + " gasto" + (total != 1 ? "s" : "") + " seleccionado" + (total != 1 ? "s" : ""));
    }

    /* ════════════════════════════════════════════════════════
       GUARDAR Y LANZAR MAIN ACTIVITY
    ════════════════════════════════════════════════════════ */

    private void guardarYContinuar() {
        if (!Utilidades.hayConexionInternet(this)) {
            Toast.makeText(this, "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir lista de gastos a crear
        List<String>  nombres    = new ArrayList<>();
        List<Double>  importes   = new ArrayList<>();
        List<String>  modos      = new ArrayList<>();
        List<String>  categorias = new ArrayList<>();

        for (int i = 0; i < SUGERENCIAS.length; i++) {
            if (checkBoxes[i] != null && checkBoxes[i].isChecked()) {
                nombres.add(SUGERENCIAS[i].nombre);
                double imp = SUGERENCIAS[i].importeDefault;
                try {
                    String txt = etImportes[i].getText().toString().trim();
                    if (!txt.isEmpty()) imp = Double.parseDouble(txt);
                } catch (NumberFormatException ignored) {}
                importes.add(imp);
                modos.add(SUGERENCIAS[i].modo);
                categorias.add(SUGERENCIAS[i].categoria);
            }
        }
        for (int i = 0; i < nombresCustom.size(); i++) {
            if (nombresCustom.get(i) != null) {
                nombres.add(nombresCustom.get(i));
                importes.add(gastosCustom.get(i)[0]);
                modos.add("efectivo");
                categorias.add("Otros");
            }
        }

        if (nombres.isEmpty()) {
            // Sin gastos: ir directamente al main
            irAResumen();
            return;
        }

        if (idHogar == -1 || idUsuario == -1) {
            Toast.makeText(this, "Error de sesión — vuelve a iniciar sesión", Toast.LENGTH_LONG).show();
            return;
        }

        btnComenzar.setEnabled(false);
        btnComenzar.setText("Guardando…");
        loadingDialog.show();

        AtomicInteger pendiente = new AtomicInteger(nombres.size());
        AtomicInteger errores   = new AtomicInteger(0);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (int i = 0; i < nombres.size(); i++) {
            crearGasto(nombres.get(i), importes.get(i), modos.get(i), categorias.get(i), hoy, pendiente, errores);
        }
    }

    private void crearGasto(String concepto, double importe, String modo, String categoria,
                             String fecha, AtomicInteger pendiente, AtomicInteger errores) {
        try {
            JSONObject body = new JSONObject();
            body.put("fecha",              fecha);
            body.put("categoria",          categoria);
            body.put("concepto",           concepto);
            body.put("modo",               modo);
            body.put("tipo",               "fijo");       // gasto recurrente = fijo
            body.put("importe",            importe);
            body.put("id_hogar",           idHogar);
            body.put("id_usuario_pagador", idUsuario);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                    Request.Method.POST, WebService.URL_Gasto, body,
                    response -> {
                        try {
                            if (!response.getString(WebService.JSON.STATUS)
                                    .equals(WebService.JSON.SUCCESS)) {
                                errores.incrementAndGet();
                            }
                        } catch (JSONException e) {
                            errores.incrementAndGet();
                        }
                        if (pendiente.decrementAndGet() == 0) onTodosCreados(errores.get());
                    },
                    error -> {
                        errores.incrementAndGet();
                        if (pendiente.decrementAndGet() == 0) onTodosCreados(errores.get());
                    }
            ));
        } catch (JSONException e) {
            errores.incrementAndGet();
            if (pendiente.decrementAndGet() == 0) onTodosCreados(errores.get());
        }
    }

    private void onTodosCreados(int errores) {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            btnComenzar.setEnabled(true);
            btnComenzar.setText("Comenzar ");
            if (errores > 0) {
                Toast.makeText(this,
                        errores + " gasto(s) no pudieron guardarse. Puedes añadirlos después.",
                        Toast.LENGTH_LONG).show();
            }
            irAResumen();
        });
    }

    private void irAResumen() {
        // → Resumen final del wizard antes de entrar a MainActivity
        startActivity(new Intent(this, Activity4_resumen_registro.class));
    }

    /* ════════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════════ */

    private String formatImporte(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format(Locale.getDefault(), "%.2f", d);
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
