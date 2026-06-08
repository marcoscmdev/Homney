package com.homney.app.ui.nav_crear_publi;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import java.io.File;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.VolleyMultipartRequest;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class fragment_crear_nueva_publicacion extends Fragment {

    /* ── Vistas ──────────────────────────────────────────── */
    private EditText   et_titulo_publi, et_cuerpo_publi;
    private ImageView  img_sube_foto, img_lanza_cam, imgPreviewFoto;
    private TextView   tvQuitarFoto;
    private Button     btn_cancelar_publi, btn_crear_publi;

    /* ── Sesión ──────────────────────────────────────────── */
    private int idUsuario = -1;

    /* ── Estado imagen ───────────────────────────────────── */
    /** URI de la imagen seleccionada (galería o cámara). null = sin imagen. */
    private Uri imagenUri    = null;
    /** URI temporal que se le pasa a la cámara para guardar la foto. */
    private Uri cameraUri    = null;

    private LoadingDialog loadingDialog;
    private static final String TAG = "WS_CREAR_PUBLI";

    /* ════════════════════════════════════════════════════════
       LAUNCHERS  (deben declararse antes de onCreate/onAttach)
    ════════════════════════════════════════════════════════ */

    /**
     * Galería: abre el selector de imágenes del dispositivo.
     * Al volver con RESULT_OK guarda la URI y muestra la previsualización.
     */
    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            imagenUri = result.getData().getData();
                            mostrarPreview(imagenUri);
                        }
                    });

    /**
     * Cámara: usa TakePicture (guarda directamente en la URI FileProvider que creamos).
     */
    private final ActivityResultLauncher<Uri> camaraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (Boolean.TRUE.equals(success) && cameraUri != null) {
                            imagenUri = cameraUri;
                            mostrarPreview(imagenUri);
                        }
                    });

    /**
     * Solicita el permiso CAMERA en runtime (obligatorio en Android 6+).
     * Si el usuario lo concede, lanza la cámara directamente.
     */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
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

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_crear_nueva_publicacion, container, false);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);

        /* Enlazar vistas */
        et_titulo_publi    = v.findViewById(R.id.et_titulo_publi);
        et_cuerpo_publi    = v.findViewById(R.id.et_cuerpo_publi);
        img_sube_foto      = v.findViewById(R.id.img_sube_foto);
        img_lanza_cam      = v.findViewById(R.id.img_lanza_cam);
        imgPreviewFoto     = v.findViewById(R.id.img_preview_foto);
        tvQuitarFoto       = v.findViewById(R.id.tv_quitar_foto);
        btn_cancelar_publi = v.findViewById(R.id.btn_cancelar_publi);
        btn_crear_publi    = v.findViewById(R.id.btn_crear_publi);

        loadingDialog = new LoadingDialog(requireContext());

        /* ── Galería ──────────────────────────────────────── */
        img_sube_foto.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            galeriaLauncher.launch(intent);
        });

        /* ── Cámara ───────────────────────────────────────── */
        img_lanza_cam.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                lanzarCamara();
            } else {
                // Pedimos el permiso; si se concede, lanzarCamara() se invoca desde el launcher
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        /* ── Quitar foto ──────────────────────────────────── */
        tvQuitarFoto.setOnClickListener(view -> {
            imagenUri = null;
            cameraUri = null;
            imgPreviewFoto.setImageDrawable(null);
            imgPreviewFoto.setVisibility(View.GONE);
            tvQuitarFoto.setVisibility(View.GONE);
        });

        /* ── Cancelar ─────────────────────────────────────── */
        btn_cancelar_publi.setOnClickListener(cancel ->
                navegarAtras());

        /* ── Publicar ─────────────────────────────────────── */
        btn_crear_publi.setOnClickListener(click ->
                validarYPublicar());

        return v;
    }

    /* ════════════════════════════════════════════════════════
       HELPERS DE IMAGEN
    ════════════════════════════════════════════════════════ */

    /** Prepara la URI FileProvider y lanza la app de cámara. */
    private void lanzarCamara() {
        cameraUri = crearUriCamara();
        if (cameraUri != null) camaraLauncher.launch(cameraUri);
    }

    /** Muestra la imagen seleccionada en el preview y activa el botón "Quitar". */
    private void mostrarPreview(Uri uri) {
        imgPreviewFoto.setImageURI(uri);
        imgPreviewFoto.setVisibility(View.VISIBLE);
        tvQuitarFoto.setVisibility(View.VISIBLE);
    }

    /**
     * Crea un fichero temporal en el directorio privado de la app y devuelve su URI
     * usando FileProvider. Es el método compatible con TODOS los dispositivos físicos
     * (incluidas cámaras OEM de Samsung, Xiaomi, Huawei, etc.) ya que evita el
     * FileUriExposedException de Android 7+ y los problemas con MediaStore URIs.
     *
     * El fichero se guarda en: [almacenamiento externo de la app]/Pictures/
     * (visible solo para la app, no requiere permisos de escritura en Android 10+)
     */
    private Uri crearUriCamara() {
        try {
            File picturesDir = new File(
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "");
            if (!picturesDir.exists()) picturesDir.mkdirs();

            File imageFile = new File(picturesDir,
                    "homney_pub_" + System.currentTimeMillis() + ".jpg");

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

    /* ════════════════════════════════════════════════════════
       FLUJO DE PUBLICACIÓN
       Paso 1 → POST muro → obtiene id_pub
       Paso 2 → (si hay imagen) sube imagen vía multipart → obtiene ruta
       Paso 3 → (si hay imagen) PUT muro con imagen=ruta
       El usuario solo ve un loadingDialog durante todo el proceso.
    ════════════════════════════════════════════════════════ */

    private void validarYPublicar() {
        String titulo = et_titulo_publi.getText().toString().trim();
        String cuerpo = et_cuerpo_publi.getText().toString().trim();

        if (titulo.isEmpty() || cuerpo.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Por favor, introduce título y contenido", Toast.LENGTH_SHORT).show();
            return;
        }

        btn_crear_publi.setEnabled(false);
        loadingDialog.show();

        // ── Paso 1: POST muro ─────────────────────────────────
        JSONObject body = new JSONObject();
        try {
            body.put("titulo",     titulo);
            body.put("cuerpo",     cuerpo);
            body.put("id_usuario", idUsuario);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            btn_crear_publi.setEnabled(true);
            return;
        }

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, WebService.URL_Muro, body,
                response -> {
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            int idPub = response.getJSONObject(WebService.JSON.DATA)
                                               .optInt("autoincrement", -1);

                            if (idPub != -1 && imagenUri != null) {
                                // Con imagen → pasos 2 y 3
                                subirImagenYActualizar(idPub, titulo, cuerpo);
                            } else {
                                // Sin imagen → fin directo
                                finalizarPublicacion();
                            }

                        } else {
                            loadingDialog.dismiss();
                            btn_crear_publi.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al publicar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        loadingDialog.dismiss();
                        btn_crear_publi.setEnabled(true);
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    btn_crear_publi.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al publicar", Request.Method.POST, WebService.URL_Muro, error);
                }
        ));
    }

    /**
     * Paso 2: convierte la URI a JPEG y la sube al servidor.
     * Máx 1080px, calidad 80 — rápido para una foto de muro.
     */
    private void subirImagenYActualizar(int idPub, String titulo, String cuerpo) {
        // La codificación del bitmap (I/O + CPU) se hace en un hilo de background
        // para no bloquear el hilo principal y evitar que la animación de carga se congele.
        new Thread(() -> {
            byte[] bytes = Utilidades.uriABytesJpeg(requireContext(), imagenUri, 1080, 80);

            // Volvemos al hilo principal para encolcar la petición Volley
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    loadingDialog.dismiss();
                    return;
                }

                if (bytes == null) {
                    // Si la conversión falla, la publicación ya existe → navegamos sin imagen
                    Toast.makeText(requireContext(),
                            getString(R.string.publicado_sin_imagen),
                            Toast.LENGTH_SHORT).show();
                    finalizarPublicacion();
                    return;
                }

                Map<String, String> params = new HashMap<>();
                params.put("tipo", "muro");
                params.put("id",   String.valueOf(idPub));

                String nombreFich = Utilidades.nombreFicheroImagen("pub", idPub); // "pub_12.jpg"

                VolleyMultipartRequest upload = new VolleyMultipartRequest(
                        Request.Method.POST,
                        WebService.URL_SubirImagen,
                        params,
                        nombreFich,
                        bytes,
                        15000,   // 15 s de timeout — suficiente para fotos comprimidas
                        (NetworkResponse networkResponse) -> {
                            if (!isAdded()) { loadingDialog.dismiss(); return; }
                            try {
                                JSONObject json = new JSONObject(
                                        new String(networkResponse.data, "UTF-8"));
                                if (json.getString(WebService.JSON.STATUS)
                                        .equals(WebService.JSON.SUCCESS)) {
                                    String ruta = json.getJSONObject(WebService.JSON.DATA)
                                                     .getString("ruta");
                                    // Paso 3: enlaza la imagen al registro del muro
                                    actualizarMuroConImagen(idPub, titulo, cuerpo, ruta);
                                } else {
                                    // Imagen fallida, pero la publi ya existe
                                    finalizarPublicacion();
                                }
                            } catch (Exception e) {
                                finalizarPublicacion();
                            }
                        },
                        error -> {
                            if (!isAdded()) { loadingDialog.dismiss(); return; }
                            // Upload fallido, la publi ya existe sin imagen
                            finalizarPublicacion();
                        }
                );
                PeticionesRed.anhadirPeticionACola(upload);
            });
        }).start();
    }

    /** Paso 3: PUT muro añadiendo la ruta de imagen al registro ya creado. */
    private void actualizarMuroConImagen(int idPub, String titulo, String cuerpo, String ruta) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_pub",  idPub);
            body.put("titulo",  titulo);
            body.put("cuerpo",  cuerpo);
            body.put("imagen",  ruta);
        } catch (JSONException e) {
            finalizarPublicacion();
            return;
        }

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.PUT, WebService.URL_Muro, body,
                response -> {
                    if (!isAdded()) { loadingDialog.dismiss(); return; }
                    finalizarPublicacion();
                },
                error -> {
                    // La imagen ya está subida en el servidor aunque el PUT falle
                    if (isAdded()) finalizarPublicacion();
                    else loadingDialog.dismiss();
                }
        ));
    }

    /** Cierra el loading, muestra toast y navega atrás. */
    private void finalizarPublicacion() {
        loadingDialog.dismiss();
        Toast.makeText(requireContext(), getString(R.string.publicado), Toast.LENGTH_SHORT).show();
        navegarAtras();
    }

    private void navegarAtras() {
        if (!isAdded()) return;
        // Navegar a fragmento5 dentro del NavController existente, sin recrear la Activity.
        // setPopUpTo limpia la pila hasta nav_home (inclusive=false lo conserva) para que
        // no quede nav_crear_publicacion ni fragmento5 apilados debajo del nuevo fragmento5.
        Navigation.findNavController(requireView()).navigate(
                R.id.fragmento5, null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_home, false)
                        .build());
    }
}
