package com.homney.app.login_registro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

import java.util.Random;

/**
 * Fragment de creación de cuenta nueva.
 * <p>
 * Flujo:
 * - Modo 0 "Crear hogar nuevo"  → POST hogar.php (clave_inv generada) → POST usuario.php (rol=admin)
 * - Modo 1 "Unirme a un hogar" → GET  hogar.php?clave_inv=X           → POST usuario.php (rol=miembro)
 * <p>
 * Tras el registro arranca Activity2_registro_config_hogar (wizard de estancias).
 */
public class activity_fragment_nueva_cuenta extends Fragment {

    private static final String TAG = "REG";

    /* ── Vistas ───────────────────────────────────────────── */
    EditText reg_nombre, reg_email, reg_telefono, reg_password, reg_password2, reg_clave;
    Spinner reg_sexo, reg_modo;
    LinearLayout reg_clave_wrap;
    Button btn_registro;
    LoadingDialog loadingDialog;

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View vista = inflater.inflate(R.layout.fragment_login_nueva_cuenta, container, false);

        reg_nombre = vista.findViewById(R.id.reg_nombre);
        reg_email = vista.findViewById(R.id.reg_email);
        reg_telefono = vista.findViewById(R.id.reg_telefono);
        reg_password = vista.findViewById(R.id.reg_password);
        reg_password2 = vista.findViewById(R.id.reg_password2);
        reg_clave = vista.findViewById(R.id.reg_clave);
        reg_clave_wrap = vista.findViewById(R.id.reg_clave_wrap);
        reg_sexo = vista.findViewById(R.id.reg_sexo);
        reg_modo = vista.findViewById(R.id.reg_modo);
        btn_registro = vista.findViewById(R.id.btn_registro);
        loadingDialog = new LoadingDialog(requireContext());

        reg_modo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modoRegistro(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btn_registro.setOnClickListener(v -> intentarRegistro());
        return vista;
    }


    /**
     * Muestra u oculta el campo de clave de invitación según el spinner de modo.
     */
    void modoRegistro(int modo) {reg_clave_wrap.setVisibility(modo == 1 ? View.VISIBLE : View.GONE);
    }

    /* ════════════════════════════════════════════════════════
       PASO 1 — VALIDAR CAMPOS
    ════════════════════════════════════════════════════════ */

    private void intentarRegistro() {
        String nombre = reg_nombre.getText().toString().trim();
        String email = reg_email.getText().toString().trim();
        String telefono = reg_telefono.getText().toString().trim();
        String password = reg_password.getText().toString();
        String password2 = reg_password2.getText().toString();
        String sexo = reg_sexo.getSelectedItem().toString();
        int modo = reg_modo.getSelectedItemPosition();
        String claveInv = reg_clave.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || telefono.isEmpty() || password.isEmpty() || password2.isEmpty()) {
            Toast.makeText(getContext(), "Rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 5) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 5 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(password2)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modo == 1 && claveInv.isEmpty()) {
            Toast.makeText(getContext(), "Introduce la clave de invitación del hogar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(getContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            return;
        }

        btn_registro.setEnabled(false);
        btn_registro.setText("Creando cuenta…");
        loadingDialog.show();

        if (modo == 0) {
            crearHogarYUsuario(generarClaveInv(), nombre, email, telefono, password, sexo, "admin");
        } else {
            buscarHogarPorClave(claveInv, nombre, email, telefono, password, sexo);
        }
    }

    /* ════════════════════════════════════════════════════════
       PASO 2a — MODO "CREAR HOGAR NUEVO"
       POST hogar.php { clave_inv } → recibe id_hogar → crea usuario
    ════════════════════════════════════════════════════════ */

    private void crearHogarYUsuario(String claveInv, String nombre, String email, String telefono, String password, String sexo, String rol) {
        try {
            JSONObject body = new JSONObject();
            body.put("clave_inv", claveInv);

            Log.d(TAG, "POST hogar → " + body);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(Request.Method.POST, WebService.URL_Hogar, body, response -> {
                Log.d(TAG, "hogar.php respuesta → " + response);
                try {
                    if (response.getString(WebService.JSON.STATUS).equals(WebService.JSON.SUCCESS)) {
                        int idHogar = response.getJSONObject(WebService.JSON.DATA).getInt("autoincrement");
                        crearUsuario(nombre, email, telefono, password, sexo, idHogar, rol);
                    } else {
                        loadingDialog.dismiss();
                        mostrarError("hogar fail: " + response);
                    }
                } catch (JSONException e) {
                    loadingDialog.dismiss();
                    mostrarError("Error JSON hogar: " + e.getMessage());
                }
            }, error -> {
                loadingDialog.dismiss();
                mostrarError("Error de red al crear el hogar");
            }));
        } catch (JSONException e) {
            loadingDialog.dismiss();
            mostrarError("Error al preparar petición de hogar");
        }
    }

    /* ════════════════════════════════════════════════════════
       PASO 2b — MODO "UNIRME A HOGAR EXISTENTE"
       GET hogar.php?clave_inv=X → recibe id_hogar → crea usuario
    ════════════════════════════════════════════════════════ */

    private void buscarHogarPorClave(String claveInv, String nombre, String email, String telefono, String password, String sexo) {
        String url = WebService.URL_Hogar + "?clave_inv=" + claveInv;
        Log.d(TAG, "GET hogar → " + url);

        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            Log.d(TAG, "hogar.php GET respuesta → " + response);
            try {
                String status = response.getString(WebService.JSON.STATUS);
                boolean hayDatos = !response.isNull(WebService.JSON.DATA);

                if (status.equals(WebService.JSON.SUCCESS) && hayDatos) {
                    JSONArray data = response.getJSONArray(WebService.JSON.DATA);
                    if (data.length() > 0) {
                        int idHogar = data.getJSONObject(0).getInt("id_hogar");
                        String passEncriptada = Utilidades.encriptaMD5(password);
                        crearUsuario(nombre, email, telefono, passEncriptada, sexo, idHogar, "miembro");
                    } else {
                        loadingDialog.dismiss();
                        mostrarError("Clave de invitación no encontrada");
                    }
                } else {
                    loadingDialog.dismiss();
                    mostrarError("Clave de invitación no válida");
                }
            } catch (JSONException e) {
                loadingDialog.dismiss();
                mostrarError("Error JSON hogar GET: " + e.getMessage());
            }
        }, error -> {
            loadingDialog.dismiss();
            mostrarError("Error de red al verificar la clave de invitación");
        }));
    }

    /* ════════════════════════════════════════════════════════
       PASO 3 — CREAR USUARIO
       POST usuario.php → recibe id_usuario → guarda sesión y continúa
    ════════════════════════════════════════════════════════ */

    private void crearUsuario(String nombre, String email, String telefono, String password, String sexo, int idHogar, String rol) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre);
            body.put("email", email);
            body.put("telefono_movil", telefono);
            body.put("clave", password);
            body.put("sexo", sexo);
            body.put("id_hogar", idHogar);
            body.put("rol", rol);

            Log.d(TAG, "POST usuario → " + body);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(Request.Method.POST, WebService.URL_Usuario, body, response -> {
                Log.d(TAG, "usuario.php respuesta → " + response);
                try {
                    if (response.getString(WebService.JSON.STATUS).equals(WebService.JSON.SUCCESS)) {
                        int idUsuario = response.getJSONObject(WebService.JSON.DATA).getInt("autoincrement");
                        guardarSesionYContinuar(idUsuario, nombre, email, rol, idHogar);
                    } else {
                        loadingDialog.dismiss();
                        // Muestra la respuesta completa del servidor para diagnosticar
                        mostrarError("Servidor: " + response);
                    }
                } catch (JSONException e) {
                    loadingDialog.dismiss();
                    mostrarError("Error JSON usuario: " + e.getMessage());
                }
            }, error -> {
                loadingDialog.dismiss();
                Log.e(TAG, "Volley error usuario: " + error);
                mostrarError("Error de red al crear el usuario");
            }));
        } catch (JSONException e) {
            loadingDialog.dismiss();
            mostrarError("Error al preparar la petición de usuario");
        }
    }

    /* ════════════════════════════════════════════════════════
       PASO 4 — GUARDAR SESIÓN Y LANZAR WIZARD
    ════════════════════════════════════════════════════════ */

    private void guardarSesionYContinuar(int idUsuario, String nombre, String email, String rol, int idHogar) {
        loadingDialog.dismiss();
        SharedPreferences.Editor editor = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE).edit();
        editor.putInt("id_usuario", idUsuario);
        editor.putString("nombre", nombre);
        editor.putString("email", email);
        editor.putString("rol", rol);
        editor.putInt("id_hogar", idHogar);
        editor.putString("avatar", "uploads/perfiles/default.png");
        editor.apply();

        startActivity(new Intent(requireActivity(), Activity2_registro_config_hogar.class));
        requireActivity().finish();
    }

    /* ════════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════════ */

    private void mostrarError(String mensaje) {
        loadingDialog.dismiss();
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            btn_registro.setEnabled(true);
            btn_registro.setText(getString(R.string.btn_crear_cuenta));
            Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Genera una clave de invitación aleatoria de 6 caracteres (ej: "H3K9MX").
     */
    private String generarClaveInv() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
