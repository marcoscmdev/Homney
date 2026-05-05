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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.HashMap;
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
    private LinearLayout llHabitaciones, llGastos;
    private TextView   tvHabLoading, tvGastosLoading;
    private TextView   tvNumHabitaciones, tvTotalGastos;

    /* ── Sesión ────────────────────────────────────────── */
    private int    idUsuario = -1;
    private int    idHogar   = -1;
    private String nombreSesion = "?";
    private String avatarSesion = null;

    /* ── Estado foto ───────────────────────────────────── */
    private Uri avatarUri   = null;
    private Uri cameraUri   = null;
    private LoadingDialog loadingDialog;

    /* ── Control de carga para lanzar IA cuando todo esté listo ── */
    private final AtomicInteger peticionesPendientes = new AtomicInteger(2); // habitaciones + gastos
    private boolean bienvenidaLanzada = false;

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
                        }
                    });

    private final ActivityResultLauncher<Uri> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (Boolean.TRUE.equals(success) && cameraUri != null) {
                            avatarUri = cameraUri;
                            mostrarAvatarPreview(avatarUri);
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
        llHabitaciones    = findViewById(R.id.ll_habitaciones_resumen);
        llGastos          = findViewById(R.id.ll_gastos_resumen);
        tvHabLoading      = findViewById(R.id.tv_hab_loading);
        tvGastosLoading   = findViewById(R.id.tv_gastos_loading);
        tvNumHabitaciones = findViewById(R.id.tv_num_habitaciones);
        tvTotalGastos     = findViewById(R.id.tv_total_gastos);

        FloatingActionButton fab = findViewById(R.id.fab_homney_mate_resumen);

        // Rellenar datos de perfil
        tvNombre.setText(nombreSesion);
        tvEmail.setText(email);
        tvRol.setText("👑 " + capitalizar(rol));
        mostrarAvatarInicial(nombreSesion, avatarSesion);

        // Listeners foto
        ivAvatar.setOnClickListener(v -> mostrarDialogElegirFoto());
        tvCambiarFoto.setOnClickListener(v -> mostrarDialogElegirFoto());

        // FAB → HomneyMate
        fab.setOnClickListener(v -> abrirHomneyMate(false));

        // Botón entrar a MainActivity
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
        if (idHogar == -1) { registrarPeticionCompletada(); return; }

        String url = WebService.URL_Gasto + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (WebService.JSON.SUCCESS.equals(response.getString(WebService.JSON.STATUS))) {
                            JSONArray data = response.getJSONArray(WebService.JSON.DATA);
                            runOnUiThread(() -> mostrarGastos(data));
                        } else {
                            runOnUiThread(() -> {
                                tvGastosLoading.setText("Sin gastos recurrentes configurados");
                                tvTotalGastos.setText("0 €");
                            });
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> tvGastosLoading.setText("Error al leer gastos"));
                    }
                    registrarPeticionCompletada();
                },
                error -> {
                    runOnUiThread(() -> tvGastosLoading.setText("Sin conexión — revisa más tarde"));
                    registrarPeticionCompletada();
                }
        ));
    }

    /** Lanza la bienvenida IA cuando ambas peticiones hayan terminado. */
    private void registrarPeticionCompletada() {
        if (peticionesPendientes.decrementAndGet() == 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!bienvenidaLanzada && !isFinishing()) {
                    bienvenidaLanzada = true;
                    abrirHomneyMate(true);
                }
            }, 1200);
        }
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
            File f = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "homney_avatar_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", f);
            camaraLauncher.launch(cameraUri);
        } catch (Exception e) {
            Toast.makeText(this, "Error al acceder a la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    /** Sube la foto y actualiza SharedPreferences; si no hay foto nueva, no hace nada. */
    private void subirFotoSiHay(Runnable onDone) {
        if (avatarUri == null) { onDone.run(); return; }

        byte[] bytes = Utilidades.uriABytesJpeg(this, avatarUri, 400, 85);
        if (bytes == null) { onDone.run(); return; }

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
                            getSharedPreferences("sesion", Context.MODE_PRIVATE)
                                    .edit().putString("avatar", ruta).apply();
                        }
                    } catch (Exception ignored) {}
                    onDone.run();
                },
                error -> { loadingDialog.dismiss(); onDone.run(); }
        );
        PeticionesRed.anhadirPeticionACola(req);
    }

    /* ════════════════════════════════════════════════════
       NAVEGACIÓN
    ════════════════════════════════════════════════════ */

    private void irAMain() {
        subirFotoSiHay(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void abrirHomneyMate(boolean esBienvenida) {
        HogarAIBottomSheet sheet = esBienvenida
                ? HogarAIBottomSheet.newBienvenida()
                : new HogarAIBottomSheet();
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
