package com.homney.app.ui.nav_crear_publi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class fragment_crear_nueva_publicacion extends Fragment {
    EditText et_titulo_publi, et_cuerpo_publi;
    ImageView img_sube_foto, img_lanza_cam;
    Button btn_cancelar_publi, btn_crear_publi;
    /* ── Sesión ─────────────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar = -1;
    private static final String TAG = "WS_CREAR_PUBLI";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crear_nueva_publicacion, container, false);
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        et_titulo_publi = v.findViewById(R.id.et_titulo_publi);
        et_cuerpo_publi = v.findViewById(R.id.et_cuerpo_publi);
        btn_cancelar_publi = v.findViewById(R.id.btn_cancelar_publi);
        btn_crear_publi = v.findViewById(R.id.btn_crear_publi);

        // TODO listener y funcionamiento de subir imagen

        btn_cancelar_publi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).popBackStack();
            }
        });
        btn_crear_publi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validarYCrearPublicacion(view);
            }
        });
        return v;
    }

    private void validarYCrearPublicacion(View v) {
        String titulo = et_titulo_publi.getText().toString().trim();
        String cuerpo = et_cuerpo_publi.getText().toString().trim();
        if (titulo.isEmpty() || cuerpo.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, introduce todos los datos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
            JSONObject body = new JSONObject();
            try {
                body.put("titulo",           titulo);
                body.put("cuerpo",            cuerpo);
                body.put("id_usuario", idUsuario);
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Error al preparar los datos de la publicacion", Toast.LENGTH_SHORT).show();
                return;
            }

            btn_crear_publi.setEnabled(false);

        String url = WebService.URL_Muro;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            int idPubli = -1;
                            if (response.has(WebService.JSON.DATA)
                                    && !response.isNull(WebService.JSON.DATA)) {
                                idPubli = response.getJSONObject(WebService.JSON.DATA)
                                        .optInt("autoincrement", -1);
                            }

                            if (idPubli != -1 ) {
                                Toast.makeText(requireContext(),
                                        "Publicacion creada correctamente", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(v).popBackStack();
                            }
                        } else {
                            btn_crear_publi.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al registrar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        btn_crear_publi.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btn_crear_publi.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al crear gasto", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }


}
