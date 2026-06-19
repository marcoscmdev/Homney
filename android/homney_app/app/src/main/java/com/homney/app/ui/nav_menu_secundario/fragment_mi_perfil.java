package com.homney.app.ui.nav_menu_secundario;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.loginActivity;
import com.homney.app.VolleyMultipartRequest;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class fragment_mi_perfil extends Fragment {

    /**
     * Valores canónicos que se almacenan en BD, independientes del idioma del dispositivo.
     * El índice coincide 1:1 con el de sexo_options en values/arrays.xml y values-en/arrays.xml.
     */
    private static final String[] SEXO_CANONICAL = {
            "Mujer", "Hombre", "No binario", "Género fluido",
            "Transgénero", "Agénero", "Prefiero no decirlo", "Otro"
    };

    /* ── Vistas ──────────────────────────────────────── */
    private ImageView ivAvatar;
    private TextView  tvCambiarFoto, tvEmail;
    private EditText  etNombre, etTelefono, etFechaNac;
    private Spinner   spinnerSexo;
    private Button    btnCancelar, btnGuardar, btnCambiarPass, btnEliminarCuenta;

    /* ── Sesión ──────────────────────────────────────── */
    private int    idUsuario = -1;

    /* ── Estado avatar ───────────────────────────────── */
    /** URI local nueva foto (null = no cambió) */
    private Uri    avatarUri     = null;
    /** URI temporal que la cámara escribe */
    private Uri    cameraUri     = null;

    /* ── Snapshot para detectar cambios ─────────────── */
    private String origNombre    = "";
    private String origTelefono  = "";
    private String origSexo      = "";
    private String origFechaNac  = "";

    private LoadingDialog loadingDialog;
    private static final String TAG = "WS_PERFIL";

    /* ════════════════════════════════════════════════════
       LAUNCHERS  (declarados antes de onCreate)
    ════════════════════════════════════════════════════ */

    private final ActivityResultLauncher<PickVisualMediaRequest> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            avatarUri = uri;
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
                        if (granted) {
                            lanzarCamara();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Se necesita permiso de cámara para tomar fotos",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    /* ════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_mi_perfil, container, false);

        /* Enlazar vistas */
        ivAvatar       = v.findViewById(R.id.iv_avatar_perfil);
        tvCambiarFoto  = v.findViewById(R.id.tv_cambiar_foto_perfil);
        tvEmail        = v.findViewById(R.id.tv_email_perfil);
        etNombre       = v.findViewById(R.id.et_nombre_perfil);
        etTelefono     = v.findViewById(R.id.et_telefono_perfil);
        etFechaNac     = v.findViewById(R.id.et_fecha_nac_perfil);
        spinnerSexo    = v.findViewById(R.id.spinner_sexo_perfil);
        btnCancelar      = v.findViewById(R.id.btn_cancelar_perfil);
        btnGuardar       = v.findViewById(R.id.btn_guardar_perfil);
        btnCambiarPass   = v.findViewById(R.id.btn_cambiar_pass_perfil);
        btnEliminarCuenta = v.findViewById(R.id.btn_eliminar_cuenta);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);

        loadingDialog = new LoadingDialog(requireContext());

        /* Spinner de sexo */
        String[] sexoOpts = requireContext().getResources()
                .getStringArray(R.array.sexo_options);
        ArrayAdapter<String> sexoAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, sexoOpts);
        sexoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSexo.setAdapter(sexoAdapter);

        /* Pre-rellenar desde sesión mientras carga del servidor */
        tvEmail.setText(prefs.getString("email", ""));
        etNombre.setText(prefs.getString("nombre", ""));
        mostrarAvatarInicial(prefs.getString("nombre", "?"), prefs.getString("avatar", null));

        /* Fecha con DatePicker */
        etFechaNac.setFocusable(false);
        etFechaNac.setClickable(true);
        etFechaNac.setOnClickListener(btn -> mostrarDatePicker());

        /* Cambiar foto */
        tvCambiarFoto.setOnClickListener(btn -> mostrarDialogElegirFoto());

        /* Cancelar */
        btnCancelar.setOnClickListener(btn -> {
            if (hayCambios()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.dialog_cambios_sin_guardar_titulo))
                        .setMessage(getString(R.string.dialog_cambios_sin_guardar_msg))
                        .setPositiveButton(getString(R.string.salir_sin_guardar), (d, w) -> navegarAtras())
                        .setNegativeButton(getString(R.string.seguir_editando), null)
                        .show();
            } else {
                navegarAtras();
            }
        });

        /* Guardar */
        btnGuardar.setOnClickListener(btn -> {
            if (!hayCambios()) {
                Toast.makeText(requireContext(), getString(R.string.sin_cambios_que_guardar),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.dialog_confirmar_cambios_titulo))
                    .setMessage(getString(R.string.dialog_confirmar_cambios_msg))
                    .setPositiveButton(getString(R.string.guardar), (d, w) -> procesarGuardado())
                    .setNegativeButton(getString(R.string.btn_cancelar), null)
                    .show();
        });

        /* Contraseña */
        btnCambiarPass.setOnClickListener(btn -> mostrarDialogCambiarPassword());

        /* Eliminar cuenta */
        btnEliminarCuenta.setOnClickListener(btn -> mostrarDialogEliminarCuenta());

        /* Cargar datos completos del servidor */
        cargarDatosPerfil();

        return v;
    }

    /* ════════════════════════════════════════════════════
       CARGA DE DATOS
    ════════════════════════════════════════════════════ */

    private void cargarDatosPerfil() {
        loadingDialog.show();
        String url = WebService.URL_Usuario + "?id_usuario=" + idUsuario;

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> resp = gson.fromJson(response.toString(), tipo);
                            if (resp.data != null && !resp.data.isEmpty()) {
                                poblarCampos(resp.data.get(0));
                            }
                        }
                    } catch (Exception e) {
                        // Fallback: se usa lo ya precargado desde SharedPreferences
                        tomarSnapshot();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    tomarSnapshot(); // snapshot de los datos de sesión ya mostrados
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error cargando perfil", Request.Method.GET, url, error);
                }
        ));
    }

    private void poblarCampos(Usuario u) {
        etNombre.setText(u.getNombre() != null ? u.getNombre() : "");
        etTelefono.setText(u.getTelefono_movil() != null ? u.getTelefono_movil() : "");
        etFechaNac.setText(Utilidades.fechaEntradaASalida(u.getFecha_nacimiento()));
        tvEmail.setText(u.getEmail() != null ? u.getEmail() : "");

        /* Spinner sexo: buscar índice usando valores canónicos (independiente del idioma).
         * El array sexo_options puede estar en español o inglés según el locale del dispositivo,
         * pero los valores en BD siempre se guardan en español (SEXO_CANONICAL). */
        if (u.getSexo() != null) {
            for (int i = 0; i < SEXO_CANONICAL.length; i++) {
                if (SEXO_CANONICAL[i].equalsIgnoreCase(u.getSexo())) {
                    spinnerSexo.setSelection(i);
                    break;
                }
            }
        }

        /* Avatar */
        mostrarAvatarInicial(u.getNombre(), u.getAvatar());

        /* Guardar snapshot DESPUÉS de poblar (referencia de "sin cambios") */
        tomarSnapshot();
    }

    /* ════════════════════════════════════════════════════
       AVATAR
    ════════════════════════════════════════════════════ */

    /** Muestra la foto real con Glide, o el círculo con inicial si no hay foto. */
    private void mostrarAvatarInicial(String nombre, String avatar) {
        boolean tieneAvatarReal = avatar != null
                && !avatar.isEmpty()
                && !avatar.contains("default.png");

        if (tieneAvatarReal) {
            // La URL pasa por el proxy PHP (get_imagen.php) porque AwardSpace bloquea
            // el acceso directo a ficheros estáticos en uploads/.
            // El cache-buster ?t= fuerza a Glide a descargar la imagen cuando cambia
            // (el servidor reutiliza el mismo nombre de fichero al sobreescribir).
            long ts = requireContext()
                    .getSharedPreferences("sesion", Context.MODE_PRIVATE)
                    .getLong("avatar_ts", 0L);
            String url = WebService.urlImagen(avatar, ts);
            Glide.with(this)
                    .load(url)
                    .apply(new RequestOptions()
                            .circleCrop()
                            .placeholder(crearBitmapInicial(nombre))
                            .error(crearBitmapInicial(nombre)))
                    .into(ivAvatar);
        } else {
            ivAvatar.setImageDrawable(crearBitmapInicial(nombre));
            ivAvatar.setClipToOutline(true);
        }
    }

    /** Preview inmediato de la nueva imagen seleccionada. */
    private void mostrarAvatarPreview(Uri uri) {
        Glide.with(this)
                .load(uri)
                .apply(new RequestOptions().circleCrop())
                .into(ivAvatar);
    }

    /**
     * Genera un Drawable circular con la inicial del nombre sobre fondo accent.
     * Mismo estilo que el avatar del toolbar en MainActivity.
     */
    private Drawable crearBitmapInicial(String nombre) {
        char inicial = (nombre != null && !nombre.isEmpty())
                ? Character.toUpperCase(nombre.charAt(0)) : '?';

        int size = Math.round(88 * requireContext().getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFF5C518); // accent
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.42f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        float yPos = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(inicial), size / 2f, yPos, textPaint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    /** Diálogo para elegir entre galería y cámara. */
    private void mostrarDialogElegirFoto() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_cambiar_foto_titulo))
                .setItems(new String[]{"📷  Cámara", "🖼️  Galería"}, (dialog, which) -> {
                    if (which == 0) {
                        // Cámara
                        if (ContextCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            lanzarCamara();
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // Galería
                        galeriaLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void lanzarCamara() {
        cameraUri = crearUriCamara();
        if (cameraUri != null) camaraLauncher.launch(cameraUri);
    }

    private Uri crearUriCamara() {
        try {
            File picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (picturesDir != null && !picturesDir.exists()) picturesDir.mkdirs();
            File imageFile = new File(picturesDir,
                    "homney_avatar_" + System.currentTimeMillis() + ".jpg");
            return FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    imageFile);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    getString(R.string.no_se_pudo_preparar_camara), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /* ════════════════════════════════════════════════════
       DETECCIÓN DE CAMBIOS
    ════════════════════════════════════════════════════ */

    private void tomarSnapshot() {
        origNombre   = etNombre.getText().toString().trim();
        origTelefono = etTelefono.getText().toString().trim();
        origSexo     = spinnerSexo.getSelectedItem() != null
                ? spinnerSexo.getSelectedItem().toString() : "";
        origFechaNac = etFechaNac.getText().toString().trim();
        avatarUri    = null; // nueva foto descartada del snapshot
    }

    private boolean hayCambios() {
        String currentSexo = spinnerSexo.getSelectedItem() != null
                ? spinnerSexo.getSelectedItem().toString() : "";
        return !etNombre.getText().toString().trim().equals(origNombre)
                || !etTelefono.getText().toString().trim().equals(origTelefono)
                || !currentSexo.equals(origSexo)
                || !etFechaNac.getText().toString().trim().equals(origFechaNac)
                || avatarUri != null;
    }

    /* ════════════════════════════════════════════════════
       FECHA
    ════════════════════════════════════════════════════ */

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        String fechaActual = etFechaNac.getText().toString().trim();
        if (!fechaActual.isEmpty()) {
            try {
                Date d = Utilidades.FMT_SALIDA.parse(fechaActual);
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {}
        }
        new DatePickerDialog(requireContext(),
                (picker, year, month, day) -> {
                    String fechaSeleccionada = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", day, month + 1, year);
                    etFechaNac.setText(fechaSeleccionada);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /* ════════════════════════════════════════════════════
       GUARDADO  (foto → campos)
    ════════════════════════════════════════════════════ */

    private void procesarGuardado() {
        btnGuardar.setEnabled(false);
        loadingDialog.show();
        if (avatarUri != null) {
            subirAvatarYActualizar();
        } else {
            actualizarPerfil(null);
        }
    }

    /**
     * Paso 1 (si hay nueva foto): convierte la URI a JPEG 400px/calidad 85 y sube al servidor.
     * Carpeta destino: uploads/perfiles/
     */
    private void subirAvatarYActualizar() {
        byte[] bytes = Utilidades.uriABytesJpeg(requireContext(), avatarUri, 400, 85);
        if (bytes == null) {
            // Foto fallida → actualiza igualmente sin cambiar avatar
            actualizarPerfil(null);
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("tipo", "perfil");   // backend acepta: "perfil" | "muro"
        params.put("id",   String.valueOf(idUsuario));

        String nombreFich = Utilidades.nombreFicheroImagen("usuario", idUsuario);

        VolleyMultipartRequest upload = new VolleyMultipartRequest(
                Request.Method.POST,
                WebService.URL_SubirImagen,
                params,
                nombreFich,
                bytes,
                15000,
                (NetworkResponse nr) -> {
                    if (!isAdded()) return;
                    try {
                        JSONObject json = new JSONObject(new String(nr.data, "UTF-8"));
                        if (json.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            String ruta = json.getJSONObject(WebService.JSON.DATA)
                                    .getString("ruta");
                            actualizarPerfil(ruta);
                        } else {
                            actualizarPerfil(null); // upload falló, pero guardamos el resto
                        }
                    } catch (Exception e) {
                        actualizarPerfil(null);
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    actualizarPerfil(null);
                }
        );
        PeticionesRed.anhadirPeticionACola(upload);
    }

    /**
     * Paso 2 (siempre): PUT /usuario.php con los campos editables.
     * @param nuevaRutaAvatar ruta del servidor si se cambió foto, null si no
     */
    private void actualizarPerfil(@Nullable String nuevaRutaAvatar) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_usuario",     idUsuario);
            body.put("nombre",         etNombre.getText().toString().trim());
            body.put("telefono_movil", etTelefono.getText().toString().trim());
            // Usar el valor canónico (siempre en español) según la posición seleccionada,
            // para que el dato en BD no dependa del idioma del dispositivo.
            int sexoPos = spinnerSexo.getSelectedItemPosition();
            String sexoCanonical = (sexoPos >= 0 && sexoPos < SEXO_CANONICAL.length)
                    ? SEXO_CANONICAL[sexoPos] : "";
            body.put("sexo", sexoCanonical);
            String fnac = Utilidades.fechaSalidaAEntrada(etFechaNac.getText().toString().trim());
            if (!fnac.isEmpty()) body.put("fecha_nacimiento", fnac);
            if (nuevaRutaAvatar != null) body.put("avatar", nuevaRutaAvatar);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            btnGuardar.setEnabled(true);
            return;
        }

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.PUT, WebService.URL_Usuario, body,
                response -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // Actualizar SharedPreferences para que el toolbar refleje los cambios
                            SharedPreferences.Editor editor = requireContext()
                                    .getSharedPreferences("sesion", Context.MODE_PRIVATE).edit();
                            editor.putString("nombre", etNombre.getText().toString().trim());
                            if (nuevaRutaAvatar != null) {
                                editor.putString("avatar", nuevaRutaAvatar);
                                // Cache-buster: fuerza a Glide a descargar la imagen nueva
                                // (el servidor reutiliza el mismo nombre de fichero)
                                editor.putLong("avatar_ts", System.currentTimeMillis());
                            }
                            editor.apply();

                            // Forzar recarga del avatar en el toolbar
                            requireActivity().invalidateOptionsMenu();

                            Toast.makeText(requireContext(),
                                    getString(R.string.perfil_actualizado), Toast.LENGTH_SHORT).show();
                            navegarAtras();

                        } else {
                            btnGuardar.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al actualizar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        btnGuardar.setEnabled(true);
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    btnGuardar.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al actualizar perfil",
                            Request.Method.PUT, WebService.URL_Usuario, error);
                }
        ));
    }

    /* ════════════════════════════════════════════════════
       CONTRASEÑA
    ════════════════════════════════════════════════════ */

    private void mostrarDialogCambiarPassword() {
        // Layout programático con dos campos de contraseña
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int px = Math.round(20 * requireContext().getResources().getDisplayMetrics().density);
        layout.setPadding(px, px / 2, px, 0);

        EditText etNuevaPass = new EditText(requireContext());
        etNuevaPass.setHint(getString(R.string.hint_nueva_pass));
        etNuevaPass.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNuevaPass);

        View separador = new View(requireContext());
        separador.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, px / 2));
        layout.addView(separador);

        EditText etConfirmaPass = new EditText(requireContext());
        etConfirmaPass.setHint(getString(R.string.hint_confirma_pass));
        etConfirmaPass.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmaPass);

        // setPositiveButton(null) para controlar el dismiss manualmente
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_cambiar_pass_titulo))
                .setView(layout)
                .setPositiveButton(getString(R.string.cambiar_btn), null) // null → no auto-dismiss
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .create();

        dialog.show();

        // Override del botón positivo DESPUÉS de show() para poder validar sin cerrar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nueva    = etNuevaPass.getText().toString().trim();
            String confirma = etConfirmaPass.getText().toString().trim();

            if (nueva.isEmpty() || confirma.isEmpty()) {
                Toast.makeText(requireContext(),
                        getString(R.string.rellena_ambos_campos), Toast.LENGTH_SHORT).show();
                return;
            }
            if (nueva.length() < 6) {
                Toast.makeText(requireContext(),
                        getString(R.string.minimo_6_caracteres), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nueva.equals(confirma)) {
                Toast.makeText(requireContext(),
                        getString(R.string.passwords_no_coinciden), Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            enviarNuevaContrasena(nueva);
        });
    }

    private void enviarNuevaContrasena(String nuevaClaveTexto) {
        loadingDialog.show();
        JSONObject body = new JSONObject();
        try {
            body.put("id_usuario", idUsuario);
            body.put("clave", Utilidades.encriptaMD5(nuevaClaveTexto));
        } catch (JSONException e) {
            loadingDialog.dismiss();
            return;
        }

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.PUT, WebService.URL_Usuario, body,
                response -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.contrasena_actualizada), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al cambiar contraseña"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al cambiar contraseña",
                            Request.Method.PUT, WebService.URL_Usuario, error);
                }
        ));
    }

    /* ════════════════════════════════════════════════════
       ELIMINAR CUENTA
    ════════════════════════════════════════════════════ */

    /**
     * Muestra un diálogo de confirmación con doble aviso antes de eliminar la cuenta.
     * El usuario debe pulsar getString(R.string.aceptar) para proceder; getString(R.string.btn_cancelar) cierra sin hacer nada.
     */
    private void mostrarDialogEliminarCuenta() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_eliminar_cuenta_titulo))
                .setMessage(getString(R.string.dialog_eliminar_cuenta_msg)
                        + "y no podrás recuperarlos.\n\n¿Estás seguro de que quieres eliminar tu cuenta?")
                .setPositiveButton(getString(R.string.aceptar), (dialog, which) -> eliminarCuenta())
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    /**
     * Ejecuta el DELETE en usuario.php y, si tiene éxito, limpia la sesión
     * y navega a la pantalla de login.
     */
    private void eliminarCuenta() {
        loadingDialog.show();
        String url = WebService.URL_Usuario + "?id_usuario=" + idUsuario;

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.DELETE, url, null,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // Limpiar sesión local
                            requireContext()
                                    .getSharedPreferences("sesion", Context.MODE_PRIVATE)
                                    .edit().clear().apply();

                            Toast.makeText(requireContext(),
                                    "Cuenta eliminada. ¡Hasta pronto!",
                                    Toast.LENGTH_LONG).show();

                            // Volver a la pantalla de login cerrando toda la pila
                            Intent intent = new Intent(requireActivity(), loginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString("message",
                                            "No se pudo eliminar la cuenta. Inténtalo de nuevo."),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta del servidor",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al eliminar cuenta",
                            Request.Method.DELETE, url, error);
                }
        ));
    }

    /* ════════════════════════════════════════════════════
       NAVEGACIÓN
    ════════════════════════════════════════════════════ */

    private void navegarAtras() {
        if (isAdded() && getView() != null)
            Navigation.findNavController(requireView()).popBackStack();
    }
}
