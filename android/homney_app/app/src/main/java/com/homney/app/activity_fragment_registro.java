package com.homney.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

public class activity_fragment_registro extends Fragment {

    EditText et_mail, et_pass;
    CheckBox cb_recordar_sesion;
    Button btn_login;
    String tagLogCat = "WS";
    String mail;
    boolean remember;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    LoadingDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_login_registro, container, false);
        et_mail = vista.findViewById(R.id.et_mail);
        et_pass = vista.findViewById(R.id.et_pass);
        cb_recordar_sesion = vista.findViewById(R.id.cb_recordar_sesion);
        btn_login = vista.findViewById(R.id.btn_login);

        preferences = requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE);
        editor = preferences.edit();
        loadingDialog = new LoadingDialog(requireContext());

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = et_mail.getText().toString().trim();
                String pass = et_pass.getText().toString().trim();

                if (mail.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(getContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                } else {
                    iniciarSesion(mail, pass);
                }
            }
        });
        return vista;
    }

    void iniciarSesion(String email, String password) {
        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), "No existe conexión a INTERNET", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = WebService.URL_Login;
        int metodo = Request.Method.POST;

        JSONObject credenciales = new JSONObject();
        try {
            credenciales.put("email", email);
            credenciales.put("password", password); // login.php espera "password"
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        loadingDialog.show();

        JsonObjectRequest peticionLogin = new JsonObjectRequest(metodo, endPoint, credenciales,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString(WebService.JSON.STATUS).equals(WebService.JSON.SUCCESS)) {
                                Gson gson = new GsonBuilder().create();

                                Type tipoRespuesta = new TypeToken<RespuestaLista<Usuario>>() {
                                }.getType();
                                RespuestaLista<Usuario> respuestaLogin = gson.fromJson(response.toString(), tipoRespuesta);

                                if (respuestaLogin.data != null && !respuestaLogin.data.isEmpty()) {
                                    Usuario usuario = respuestaLogin.data.get(0);

                                    editor.putInt("id_usuario", usuario.getId_usuario());
                                    editor.putString("nombre", usuario.getNombre());
                                    editor.putString("email", usuario.getEmail());
                                    editor.putString("rol", usuario.getRol());
                                    editor.putInt("id_hogar", usuario.getId_hogar());
                                    editor.putString("avatar", usuario.getAvatar());

                                    if (cb_recordar_sesion.isChecked()) {
                                        editor.putBoolean("remember", true);
                                        editor.putString("mail", usuario.getEmail());
                                    } else {
                                        editor.putBoolean("remember", false);
                                    }
                                    editor.apply();

                                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                                    intent.putExtra("usuario_id", usuario.getId_usuario());
                                    intent.putExtra("usuario_nombre", usuario.getNombre());
                                    intent.putExtra("usuario_email", usuario.getEmail());

                                    // ── NO hacemos dismiss() aquí ──────────────────────────
                                    // El loading sigue girando durante la transición.
                                    // Cuando la loginActivity hace finish(), el dialog desaparece
                                    // junto con ella en cuanto MainActivity es visible.
                                    startActivity(intent);
                                    requireActivity().finish();

                                } else {
                                    // Login incorrecto → sí descartamos el loading
                                    loadingDialog.dismiss();
                                    Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                loadingDialog.dismiss();
                                String mensajeError = response.has("message")
                                        ? response.getString("message") : "Error de login";
                                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            loadingDialog.dismiss();
                            Toast.makeText(requireContext(), "Error al procesar la respuesta del servidor", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loadingDialog.dismiss();
                        Utilidades.mostrar_error_peticion(requireContext(), tagLogCat, "Error en la red", metodo, endPoint, error);
                    }
                }
        );

        PeticionesRed.anhadirPeticionACola(peticionLogin);
    }
}
