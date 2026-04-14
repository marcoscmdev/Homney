package com.homney.app.ui.nav_crear_tarea;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.homney.app.webservice.modelo.Habitacion;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class fragment_crear_tarea extends Fragment {

    /* ── Vistas (IDs de fragment_crear_nueva_tarea.xml) ── */
    AutoCompleteTextView etNombreTarea;   // R.id.et_nombre_tarea
    Spinner spinnerHabitacion;            // R.id.spinner_habitacion
    Spinner spinnerFrecuencia;            // R.id.spinner_frecuencia  (entries ya en XML)
    Spinner spinnerAsigna;                // R.id.spinner_asigna
    EditText etDuracion;                  // R.id.et_duracion
    EditText etNumVeces;                  // R.id.et_num_veces
    Button btnCancelar;                   // R.id.btn_cancelar_tarea
    Button btnCrear;                      // R.id.btn_crear_tarea

    /* ── Sesión ─────────────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

    /* ── Datos de los spinners dinámicos ────────────────── */
    private List<Habitacion> listaHabitaciones = new ArrayList<>();
    private List<Usuario>    listaUsuarios     = new ArrayList<>();

    private static final String TAG = "WS_CREAR_TAREA";

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_crear_nueva_tarea, container, false);

        etNombreTarea     = v.findViewById(R.id.et_nombre_tarea);
        spinnerHabitacion = v.findViewById(R.id.spinner_habitacion);
        spinnerFrecuencia = v.findViewById(R.id.spinner_frecuencia);
        spinnerAsigna     = v.findViewById(R.id.spinner_asigna);
        etDuracion        = v.findViewById(R.id.et_duracion);
        etNumVeces        = v.findViewById(R.id.et_num_veces);
        btnCancelar       = v.findViewById(R.id.btn_cancelar_tarea);
        btnCrear          = v.findViewById(R.id.btn_crear_tarea);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        // ── AutoComplete: sugerencias de tareas comunes ──────
        String[] sugerencias = requireContext().getResources()
                .getStringArray(R.array.tareas_options);
        ArrayAdapter<String> acAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias);
        etNombreTarea.setAdapter(acAdapter);
        etNombreTarea.setThreshold(1); // mostrar sugerencias desde el 1er carácter

        // ── Botones ──────────────────────────────────────────
        btnCancelar.setOnClickListener(btn ->
                Navigation.findNavController(v).popBackStack());

        btnCrear.setOnClickListener(btn -> validarYCrearTarea(v));

        // ── Cargar spinners dinámicos ─────────────────────────
        if (Utilidades.hayConexionInternet(requireContext())) {
            cargarHabitaciones();
            cargarUsuarios();
        } else {
            Toast.makeText(requireContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
        }

        return v;
    }

    /* ════════════════════════════════════════════
       CARGA DINÁMICA DE SPINNERS
    ════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        String url = WebService.URL_Habitacion + "?id_hogar=" + idHogar;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Habitacion>>() {}.getType();
                            RespuestaLista<Habitacion> resp =
                                    gson.fromJson(response.toString(), tipo);
                            listaHabitaciones = resp.data != null ? resp.data : new ArrayList<>();
                            poblarSpinnerHabitaciones();
                        }
                    } catch (JSONException ignored) {}
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error habitaciones", Request.Method.GET, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    private void cargarUsuarios() {
        String url = WebService.URL_Usuario + "?id_hogar=" + idHogar;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> resp =
                                    gson.fromJson(response.toString(), tipo);
                            listaUsuarios = resp.data != null ? resp.data : new ArrayList<>();
                            poblarSpinnerUsuarios();
                        }
                    } catch (JSONException ignored) {}
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error usuarios", Request.Method.GET, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    private void poblarSpinnerHabitaciones() {
        List<String> nombres = new ArrayList<>();
        nombres.add("Sin habitación");
        for (Habitacion h : listaHabitaciones) nombres.add(h.getNombre());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHabitacion.setAdapter(adapter);
    }

    private void poblarSpinnerUsuarios() {
        List<String> nombres = new ArrayList<>();
        int indexPropio = 0;
        for (int i = 0; i < listaUsuarios.size(); i++) {
            Usuario u = listaUsuarios.get(i);
            String label = u.getNombre()
                    + ("admin".equalsIgnoreCase(u.getRol()) ? " [Admin]" : "");
            nombres.add(label);
            if (u.getId_usuario() == idUsuario) indexPropio = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAsigna.setAdapter(adapter);
        spinnerAsigna.setSelection(indexPropio); // preseleccionar el usuario actual
    }

    /* ════════════════════════════════════════════
       VALIDACIÓN Y ENVÍO  (POST tarea → POST asignacion)
       Equivale a submitTarea() del web
    ════════════════════════════════════════════ */

    private void validarYCrearTarea(View v) {
        String nombre = etNombreTarea.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Duración (opcional)
        Integer duracion = null;
        String durStr = etDuracion.getText().toString().trim();
        if (!durStr.isEmpty()) {
            try {
                duracion = Integer.parseInt(durStr);
                if (duracion < 1 || duracion > 255) {
                    Toast.makeText(requireContext(),
                            "Duración debe estar entre 1 y 255 min", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Duración inválida", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Nº veces
        int numVeces = 1;
        String nvStr = etNumVeces.getText().toString().trim();
        if (!nvStr.isEmpty()) {
            try {
                numVeces = Integer.parseInt(nvStr);
                if (numVeces < 1 || numVeces > 127) {
                    Toast.makeText(requireContext(),
                            "Nº veces debe estar entre 1 y 127", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Nº veces inválido", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Frecuencia (el Spinner usa el array frecuencia_options: Dia, Semana, Mes, Variable)
        String[] freqLabels = {"dia", "semana", "mes", "variable"};
        String frecuencia = freqLabels[spinnerFrecuencia.getSelectedItemPosition()];

        // Habitación (índice 0 = "Sin habitación")
        Integer idHabitacion = null;
        int habPos = spinnerHabitacion.getSelectedItemPosition();
        if (habPos > 0 && !listaHabitaciones.isEmpty() && habPos - 1 < listaHabitaciones.size()) {
            idHabitacion = listaHabitaciones.get(habPos - 1).getId_habitacion();
        }

        // Usuario a asignar
        int asignarUID = idUsuario;
        int userPos = spinnerAsigna.getSelectedItemPosition();
        if (!listaUsuarios.isEmpty() && userPos < listaUsuarios.size()) {
            asignarUID = listaUsuarios.get(userPos).getId_usuario();
        }

        // Construir JSON
        JSONObject body = new JSONObject();
        try {
            body.put("nombre",    nombre);
            body.put("frecuencia", frecuencia);
            body.put("num_veces", numVeces);
            body.put("id_hogar",  idHogar);
            if (duracion != null)     body.put("duracion",       duracion);
            if (idHabitacion != null) body.put("id_habitacion",  idHabitacion);
            body.put("explicacion_frecuencia_variable",
                    frecuencia.equals("variable") ? "" : JSONObject.NULL);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error al preparar los datos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCrear.setEnabled(false);
        final int uidAsignar = asignarUID;

        String url = WebService.URL_Tarea;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // Leer el id_tarea generado (autoincrement)
                            int idTarea = -1;
                            if (response.has(WebService.JSON.DATA)
                                    && !response.isNull(WebService.JSON.DATA)) {
                                idTarea = response.getJSONObject(WebService.JSON.DATA)
                                        .optInt("autoincrement", -1);
                            }

                            if (idTarea != -1) {
                                crearAsignacion(idTarea, uidAsignar, v);
                            } else {
                                Toast.makeText(requireContext(),
                                        "Tarea creada", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(v).popBackStack();
                            }
                        } else {
                            btnCrear.setEnabled(true);
                            String msg = response.optString("message", "Error al crear la tarea");
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        btnCrear.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    btnCrear.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al crear tarea", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /** POST asignacion_tarea: asigna la tarea recién creada al usuario seleccionado. */
    private void crearAsignacion(int idTarea, int idUsuarioAsig, View v) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_tarea",   idTarea);
            body.put("id_usuario", idUsuarioAsig);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Tarea creada (sin asignar)", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).popBackStack();
            return;
        }

        String url = WebService.URL_Asignacion_Tarea;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    "Tarea creada y asignada ✓", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Tarea creada pero no asignada", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException ignored) {}
                    Navigation.findNavController(v).popBackStack();
                },
                error -> {
                    Toast.makeText(requireContext(),
                            "Tarea creada pero error al asignar", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(v).popBackStack();
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }
}
