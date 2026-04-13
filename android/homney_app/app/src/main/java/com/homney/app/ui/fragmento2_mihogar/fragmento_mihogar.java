package com.homney.app.ui.fragmento2_mihogar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Hogar;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

public class fragmento_mihogar extends Fragment {

    TextView tv_hogar;
    String tagLogCat = "WS";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment2_hogar, container, false);

        tv_hogar = root.findViewById(R.id.text_gallery);

        Bundle argumentos = getArguments();
        if (argumentos != null) {
            String dato = argumentos.getString("dato");
            Toast.makeText(getContext(), "Dato recibido: " + dato, Toast.LENGTH_LONG).show();
        }

        cargarHogar();

        return root;
    }

    void cargarHogar() {
        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), "No existe conexión a INTERNET", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = WebService.URL_Hogar;
        int metodo = Request.Method.GET;

        JsonObjectRequest peticionHogar = new JsonObjectRequest(metodo, endPoint, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString(WebService.JSON.STATUS).equals(WebService.JSON.SUCCESS)) {
                                Gson gson = new GsonBuilder().create();

                                Type tipoRespuesta = new TypeToken<RespuestaLista<Hogar>>() {}.getType();
                                RespuestaLista<Hogar> respuestaHogar = gson.fromJson(response.toString(), tipoRespuesta);

                                if (respuestaHogar.data != null && !respuestaHogar.data.isEmpty()) {
                                    StringBuilder sb = new StringBuilder();
                                    for (Hogar h : respuestaHogar.data) {
                                        sb.append("ID: ").append(h.getId_hogar())
                                          .append(" | Clave: ").append(h.getClave_inv())
                                          .append("\n");
                                    }
                                    tv_hogar.setText(sb.toString());
                                } else {
                                    tv_hogar.setText("No hay hogares registrados");
                                }
                            } else {
                                String mensajeError = response.has("message")
                                        ? response.getString("message")
                                        : "Error al obtener hogares";
                                Toast.makeText(requireContext(), mensajeError, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(requireContext(), "Error al procesar la respuesta del servidor", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utilidades.mostrar_error_peticion(requireContext(), tagLogCat, "Error en la red", metodo, endPoint, error);
                    }
                }
        );

        PeticionesRed.anhadirPeticionACola(peticionHogar);
    }
}