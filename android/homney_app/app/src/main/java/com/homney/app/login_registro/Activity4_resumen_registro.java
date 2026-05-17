package com.homney.app.login_registro;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.homney.app.MainActivity;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.VolleyMultipartRequest;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.ui.fragmento2_mihogar.HogarAIBottomSheet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PASO FINAL del wizard de registro.
 * Muestra:
 *  - Tarjeta 1: datos del usuario con opción de añadir foto de perfil
 *  - Tarjeta 2: habitaciones creadas (cargadas de la API)
 *  - Tarjeta 3: gastos recurrentes (cargados de la API)
 *  - FAB HomneyMate: abre el asistente IA
 * Al completarse la carga, lanza automáticamente el saludo de bienvenida de HomneyMate.
 */
public class Activity4_resumen_registro extends AppCompatActivity {

    /* ── Vistas ────────────────────────────────────────── */
    private ImageView  ivAvatar;
    private TextView   tvCambiarFoto;
    private TextView   tvNombre, tvEmail, tvRol;
    private TextView   tvNombreHogar;
    private LinearLayout llHabitaciones, llGastos;
    private TextView   tvHabLoading, tvGastosLoading;
    private TextView   tvNumHabitaciones, tvTotalGastos;

    /* ── Sesión ────────────────────────────────────────── */
    private int    idUsuario = -1;
    private int    idHogar   = -1;
    private String nombreSesion  = "?";
    private String avatarSesion  = null;
    private String nombreHogar   = "";   // se pone al nombrarlo

    /* ── Estado foto ───────────────────────────────────── */
    private Uri avatarUri   = null;
    private Uri cameraUri   = null;
    private LoadingDialog loadingDialog;

    /* ── Control de navegación ───────────────────────────── */
    private boolean bienvenidaLanzada  = false;

    /* ════════════════════════════════════════════════════
       LAUNCHERS  (registrados antes de onCreate)
    ════════════════════════════════════════════════════ */

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            avatarUri = result.getData().getData();
                            mostrarAvatarPreview(avatarUri);
                            subirFotoAhora(); // sube inmediatamente al seleccionar
                        }
                    });

    private final ActivityResultLauncher<Uri> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (Boolean.TRUE.equals(success) && cameraUri != null) {
                            avatarUri = cameraUri;
                            mostrarAvatarPreview(avatarUri);
                            subirFotoAhora(); // sube inmediatamente al seleccionar
                        }
                    });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) lanzarCamara();
                        else Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
                    });

    /* ════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity4_resumen_registro);

        loadingDialog = new LoadingDialog(this);

        // Sesión
        SharedPreferences prefs = getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario    = prefs.getInt("id_usuario", -1);
        idHogar      = prefs.getInt("id_hogar",   -1);
        nombreSesion = prefs.getString("nombre",  "Usuario");
        avatarSesion = prefs.getString("avatar",  null);
        String email = prefs.getString("email",   "—");
        String rol   = prefs.getString("rol",     "inquilino");

        // Vistas
        ivAvatar          = findViewById(R.id.iv_avatar_resumen);
        tvCambiarFoto     = findViewById(R.id.tv_cambiar_foto_resumen);
        tvNombre          = findViewById(R.id.tv_nombre_resumen);
        tvEmail           = findViewById(R.id.tv_email_resumen);
        tvRol             = findViewById(R.id.tv_rol_resumen);
        tvNombreHogar     = findViewById(R.id.tv_nombre_hogar_resumen);
        llHabitaciones    = findViewById(R.id.ll_habitaciones_resumen);
        llGastos          = findViewById(R.id.ll_gastos_resumen);
        tvHabLoading      = findViewById(R.id.tv_hab_loading);
        tvGastosLoading   = findViewById(R.id.tv_gastos_loading);
        tvNumHabitaciones = findViewById(R.id.tv_num_habitaciones);
        tvTotalGastos     = findViewById(R.id.tv_total_gastos);

        // Rellenar datos de perfil
        tvNombre.setText(nombreSesion);
        tvEmail.setText(email);
        tvRol.setText("👑 " + capitalizar(rol));
        mostrarAvatarInicial(nombreSesion, avatarSesion);

        // Listeners foto
        ivAvatar.setOnClickListener(v -> mostrarDialogElegirFoto());
        tvCambiarFoto.setOnClickListener(v -> mostrarDialogElegirFoto());

        // Botón "Nombrar hogar"
        findViewById(R.id.btn_nombrar_hogar).setOnClickListener(v -> mostrarDialogNombrarHogar());

        // Botón "Entrar a la colmena" — activo hasta que se nombre el hogar y abra el asistente
        findViewById(R.id.btn_entrar_colmena).setOnClickListener(v -> irAMain());

        // Cargar datos de la API
        cargarHabitaciones();
        cargarGastos();
    }

    /* ════════════════════════════════════════════════════
       CARGA DE DATOS DESDE LA API
    ════════════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        if (idHogar == -1) { registrarPeticionCompletada(); return; }

        String url = WebService.URL_Habitacion + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (WebService.JSON.SUCCESS.equals(response.getString(WebService.JSON.STATUS))) {
                            JSONArray data = response.getJSONArray(WebService.JSON.DATA);
                            runOnUiThread(() -> mostrarHabitaciones(data));
                        } else {
                            runOnUiThread(() -> tvHabLoading.setText("No se pudieron cargar las habitaciones"));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> tvHabLoading.setText("Error al leer habitaciones"));
                    }
                    registrarPeticionCompletada();
                },
                error -> {
                    runOnUiThread(() -> tvHabLoading.setText("Sin conexión — revisa más tarde"));
                    registrarPeticionCompletada();
                }
        ));
    }

    private void cargarGastos() {
        // Los gastos se guardaron localmente en Activity3 — los mostramos desde SharedPreferences.
        String gastosStr = getSharedPreferences("registro_wizard", MODE_PRIVATE)
                .getString("registro_gastos", null);
        if (gastosStr == null) {
            tvGastosLoading.setText("Sin gastos recurrentes configurados");
            tvTotalGastos.setText("0 €");
            return;
        }
        try {
            JSONArray data = new JSONArray(gastosStr);
            mostrarGastos(data);
        } catch (JSONException e) {
            tvGastosLoading.setText("Sin gastos recurrentes configurados");
            tvTotalGastos.setText("0 €");
        }
    }

    /** Marca que una de las dos peticiones (habitaciones/gastos) ha terminado. */
    private void registrarPeticionCompletada() {
        // Ya no lanza el asistente automáticamente:
        // el asistente se abre solo después de que el usuario nombre el hogar.
    }

    /* ════════════════════════════════════════════════════
       RENDERIZADO DE LISTAS
    ════════════════════════════════════════════════════ */

    private void mostrarHabitaciones(JSONArray data) {
        llHabitaciones.removeAllViews();
        int n = data.length();
        tvNumHabitaciones.setText(n + (n == 1 ? " habitación" : " habitaciones"));

        if (n == 0) {
            TextView tv = nuevaLinea("Sin habitaciones configuradas", true);
            llHabitaciones.addView(tv);
            return;
        }

        for (int i = 0; i < n; i++) {
            try {
                JSONObject hab = data.getJSONObject(i);
                String nombre = hab.optString("nombre", "—");
                String tipo   = hab.optString("tipo", "generica");
                String emoji  = emojiPorTipo(tipo);

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(4);
                fila.setLayoutParams(lp);

                TextView tvEmoji = new TextView(this);
                tvEmoji.setText(emoji);
                tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tvEmoji.setPadding(0, 0, dp(8), 0);
                fila.addView(tvEmoji);

                TextView tvNom = new TextView(this);
                tvNom.setText(nombre);
                tvNom.setTextColor(getResources().getColor(R.color.text, null));
                tvNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                fila.addView(tvNom);

                llHabitaciones.addView(fila);
            } catch (JSONException ignored) {}
        }
    }

    private void mostrarGastos(JSONArray data) {
        llGastos.removeAllViews();
        double total = 0;
        int n = data.length();

        if (n == 0) {
            llGastos.addView(nuevaLinea("Sin gastos recurrentes configurados", true));
            tvTotalGastos.setText("0 €");
            return;
        }

        for (int i = 0; i < n; i++) {
            try {
                JSONObject g = data.getJSONObject(i);
                String concepto = g.optString("concepto", "—");
                double importe  = g.optDouble("importe", 0);
                total += importe;

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(4);
                fila.setLayoutParams(lp);

                TextView tvConc = new TextView(this);
                tvConc.setText("• " + concepto);
                tvConc.setTextColor(getResources().getColor(R.color.text, null));
                tvConc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tvConc.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                fila.addView(tvConc);

                TextView tvImp = new TextView(this);
                tvImp.setText(formatImporte(importe) + " €");
                tvImp.setTextColor(getResources().getColor(R.color.accent_dark, null));
                tvImp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tvImp.setTypeface(null, Typeface.BOLD);
                fila.addView(tvImp);

                llGastos.addView(fila);
            } catch (JSONException ignored) {}
        }

        // Total
        double finalTotal = total;
        runOnUiThread(() -> tvTotalGastos.setText(formatImporte(finalTotal) + " €/mes"));
    }

    private TextView nuevaLinea(String texto, boolean muted) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(getResources().getColor(
                muted ? R.color.muted : R.color.text, null));
        return tv;
    }

    /* ════════════════════════════════════════════════════
       AVATAR — igual que fragment_mi_perfil
    ════════════════════════════════════════════════════ */

    private void mostrarAvatarInicial(String nombre, String avatar) {
        char inicial = (nombre != null && !nombre.isEmpty())
                ? Character.toUpperCase(nombre.charAt(0)) : '?';

        boolean tieneAvatarReal = avatar != null
                && !avatar.isEmpty()
                && !avatar.contains("default.png");

        if (tieneAvatarReal) {
            String url = WebService.PROTOCOLO + WebService.SERVIDOR
                    + WebService.CARPETA + "/" + avatar;
            Glide.with(this)
                    .load(url)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(crearBitmapInicial(inicial)))
                    .into(ivAvatar);
            tvCambiarFoto.setText("Cambiar foto");
        } else {
            ivAvatar.setImageDrawable(crearBitmapInicial(inicial));
            ivAvatar.setClipToOutline(true);
        }
    }

    private void mostrarAvatarPreview(Uri uri) {
        Glide.with(this)
                .load(uri)
                .apply(new RequestOptions().circleCrop())
                .into(ivAvatar);
        tvCambiarFoto.setText("Cambiar foto");
    }

    private Drawable crearBitmapInicial(char inicial) {
        int size = dp(80);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(0xFFF5C518);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bg);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextSize(size * 0.43f);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        float y = size / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(String.valueOf(inicial), size / 2f, y, text);
        return new BitmapDrawable(getResources(), bmp);
    }

    /* ════════════════════════════════════════════════════
       BACK PRESS — siempre bloqueado en el paso final
    ════════════════════════════════════════════════════ */

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Por favor completa el registro", Toast.LENGTH_SHORT).show();
    }

    /* ════════════════════════════════════════════════════
       NOMBRAR HOGAR
    ════════════════════════════════════════════════════ */

    private void mostrarDialogNombrarHogar() {
        android.widget.EditText etNombre = new android.widget.EditText(this);
        etNombre.setHint("Ej: Casa de la playa, Piso compartido…");
        etNombre.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        if (!nombreHogar.isEmpty()) etNombre.setText(nombreHogar);
        int pad = dp(20);
        etNombre.setPadding(pad, dp(12), pad, dp(12));

        new android.app.AlertDialog.Builder(this)
                .setTitle("¿Cómo se llama tu hogar?")
                .setView(etNombre)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(this, "Escribe un nombre para tu hogar", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    guardarNombreHogar(nombre);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarNombreHogar(String nombre) {
        loadingDialog.show();
        JSONObject body = new JSONObject();
        try {
            body.put("id_hogar",     idHogar);
            body.put("value_nombre", nombre);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            return;
        }
        String url = WebService.URL_Hogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.PUT, url, body,
                response -> {
                    loadingDialog.dismiss();
                    try {
                        if (WebService.JSON.SUCCESS.equals(
                                response.getString(WebService.JSON.STATUS))) {
                            nombreHogar = nombre;
                            // Guardar en SharedPreferences para que el asistente lo use
                            getSharedPreferences("sesion", MODE_PRIVATE).edit()
                                    .putString("nombre_hogar", nombre).apply();
                            runOnUiThread(() -> {
                                tvNombreHogar.setText(nombre);
                                tvNombreHogar.setTextColor(
                                        getResources().getColor(R.color.text, null));
                                // Bloquear atrás y abrir el asistente
                                if (!bienvenidaLanzada) {
                                    bienvenidaLanzada = true;
                                    abrirHomneyMate(true);
                                }
                            });
                        } else {
                            Toast.makeText(this,
                                    "No se pudo guardar el nombre", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    Utilidades.mostrar_error_peticion(this, "Activity4",
                            "Error al nombrar hogar", Request.Method.PUT, url, error);
                }
        ));
    }

    /* ════════════════════════════════════════════════════
       SELECCIÓN DE FOTO
    ════════════════════════════════════════════════════ */

    private void mostrarDialogElegirFoto() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setItems(new String[]{"📷  Cámara", "🖼️  Galería"}, (d, w) -> {
                    if (w == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            lanzarCamara();
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        galeriaLauncher.launch(intent);
                    }
                })
                .show();
    }

    private void lanzarCamara() {
        try {
            // Usamos getExternalFilesDir con fallback a getCacheDir
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (dir == null) dir = getCacheDir();
            File f = new File(dir, "homney_avatar_" + System.currentTimeMillis() + ".jpg");
            // Autoridad debe coincidir con AndroidManifest: ${applicationId}.fileprovider
            cameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", f);
            camaraLauncher.launch(cameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Error al acceder a la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sube la foto inmediatamente al seleccionarla (no espera a ir a MainActivity).
     * Actualiza SharedPreferences y la BD mediante subir_imagen.php.
     */
    private void subirFotoAhora() {
        if (avatarUri == null) return;

        byte[] bytes = Utilidades.uriABytesJpeg(this, avatarUri, 400, 85);
        if (bytes == null) return;

        loadingDialog.show();

        Map<String, String> params = new HashMap<>();
        params.put("tipo", "perfil");
        params.put("id",   String.valueOf(idUsuario));

        String nombreFich = Utilidades.nombreFicheroImagen("usuario", idUsuario);

        VolleyMultipartRequest req = new VolleyMultipartRequest(
                Request.Method.POST,
                WebService.URL_SubirImagen,
                params,
                nombreFich,
                bytes,
                15000,
                nr -> {
                    loadingDialog.dismiss();
                    try {
                        JSONObject json = new JSONObject(new String(nr.data, "UTF-8"));
                        if (WebService.JSON.SUCCESS.equals(json.getString(WebService.JSON.STATUS))) {
                            String ruta = json.getJSONObject(WebService.JSON.DATA).getString("ruta");
                            // Persiste en SharedPreferences y en DB (subir_imagen.php ya hace el UPDATE)
                            getSharedPreferences("sesion", Context.MODE_PRIVATE)
                                    .edit().putString("avatar", ruta).apply();
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Foto de perfil guardada ✓",
                                            Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception ignored) {}
                },
                error -> loadingDialog.dismiss()
        );
        PeticionesRed.anhadirPeticionACola(req);
    }

    /* ════════════════════════════════════════════════════
       NAVEGACIÓN — commit tareas+gastos y lanzar Main
    ════════════════════════════════════════════════════ */

    private void irAMain() {
        SharedPreferences wizardPrefs = getSharedPreferences("registro_wizard", MODE_PRIVATE);
        String tareasStr = wizardPrefs.getString("registro_tareas", null);
        String gastosStr = wizardPrefs.getString("registro_gastos", null);

        JSONArray tareas = null, gastos = null;
        try { if (tareasStr != null) tareas = new JSONArray(tareasStr); } catch (JSONException ignored) {}
        try { if (gastosStr != null) gastos = new JSONArray(gastosStr); } catch (JSONException ignored) {}

        int totalPeticiones = (tareas != null ? tareas.length() : 0)
                            + (gastos != null ? gastos.length() : 0);

        if (totalPeticiones == 0 || !Utilidades.hayConexionInternet(this)) {
            limpiarWizardPrefs();
            lanzarMain();
            return;
        }

        loadingDialog.show();
        AtomicInteger pendiente = new AtomicInteger(totalPeticiones);
        AtomicInteger errores   = new AtomicInteger(0);
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (tareas != null) {
            for (int i = 0; i < tareas.length(); i++) {
                try {
                    JSONObject t    = tareas.getJSONObject(i);
                    JSONObject body = new JSONObject();
                    body.put("nombre",        t.getString("nombre"));
                    body.put("frecuencia",    t.getString("frecuencia"));
                    body.put("num_veces",     t.getInt("num_veces"));
                    body.put("id_habitacion", t.getInt("id_habitacion"));
                    PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                            Request.Method.POST, WebService.URL_Tarea, body,
                            r -> { if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get()); },
                            e -> { errores.incrementAndGet(); if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get()); }
                    ));
                } catch (JSONException e) {
                    errores.incrementAndGet();
                    if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get());
                }
            }
        }

        if (gastos != null) {
            for (int i = 0; i < gastos.length(); i++) {
                try {
                    JSONObject g    = gastos.getJSONObject(i);
                    JSONObject body = new JSONObject();
                    body.put("fecha",              hoy);
                    body.put("categoria",          g.getString("categoria"));
                    body.put("concepto",           g.getString("concepto"));
                    body.put("modo",               g.getString("modo"));
                    body.put("tipo",               "fijo");
                    body.put("importe",            g.getDouble("importe"));
                    body.put("id_hogar",           idHogar);
                    body.put("id_usuario_pagador", idUsuario);
                    PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                            Request.Method.POST, WebService.URL_Gasto, body,
                            r -> { if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get()); },
                            e -> { errores.incrementAndGet(); if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get()); }
                    ));
                } catch (JSONException e) {
                    errores.incrementAndGet();
                    if (pendiente.decrementAndGet() == 0) onCommitDone(errores.get());
                }
            }
        }
    }

    private void onCommitDone(int errores) {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            if (errores > 0) {
                Toast.makeText(this,
                        errores + " elemento(s) no pudieron guardarse. Puedes añadirlos después.",
                        Toast.LENGTH_LONG).show();
            }
            limpiarWizardPrefs();
            lanzarMain();
        });
    }

    private void limpiarWizardPrefs() {
        getSharedPreferences("registro_wizard", MODE_PRIVATE)
                .edit()
                .remove("registro_tareas")
                .remove("registro_gastos")
                .apply();
    }

    private void lanzarMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void abrirHomneyMate(boolean esBienvenida) {
        HogarAIBottomSheet sheet = esBienvenida
                ? HogarAIBottomSheet.newBienvenida()
                : new HogarAIBottomSheet();
        if (esBienvenida) {
            // Al pulsar "Aceptar" en el BottomSheet → ir a MainActivity
            sheet.setOnAceptarBienvenidaListener(this::irAMain);
        }
        sheet.show(getSupportFragmentManager(), "hogar_ai_bienvenida");
    }

    /* ════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════ */

    /** Emoji representativo por tipo de habitación. */
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

    private String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatImporte(double d) {
        if (d == (long) d) return String.valueOf((long) d);
        return String.format(java.util.Locale.getDefault(), "%.2f", d);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}
