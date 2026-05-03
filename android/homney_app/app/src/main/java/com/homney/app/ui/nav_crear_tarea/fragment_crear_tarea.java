package com.homney.app.ui.nav_crear_tarea;

import android.annotation.SuppressLint;
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
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TimePicker;
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
import com.homney.app.utils.LoadingDialog;
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

    /* ── Vistas ────────────────────────────────── */
    AutoCompleteTextView etNombreTarea;
    Spinner spinnerHabitacion;
    Spinner spinnerFrecuencia;
    Spinner spinnerAsigna;
    NumberPicker npHoras, npMinutos;
    EditText etNumVeces;
    Button btnCancelar;
    Button btnCrear;

    /* ── Sesión ─────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

    /* ── Datos de los spinners dinámicos ────────── */
    private List<Habitacion> listaHabitaciones = new ArrayList<>();
    private List<Usuario>    listaUsuarios     = new ArrayList<>();
    private LoadingDialog loadingDialog;

    private static final String TAG = "WS_CREAR_TAREA";
    private int stepMinutos = 5;

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_crear_nueva_tarea, container, false);

        etNombreTarea     = v.findViewById(R.id.et_nombre_tarea);
        spinnerHabitacion = v.findViewById(R.id.spinner_habitacion);
        spinnerFrecuencia = v.findViewById(R.id.spinner_frecuencia);
        spinnerAsigna     = v.findViewById(R.id.spinner_asigna);
        etNumVeces        = v.findViewById(R.id.et_num_veces);
        btnCancelar       = v.findViewById(R.id.btn_cancelar_tarea);
        btnCrear          = v.findViewById(R.id.btn_crear_tarea);
        npHoras           = v.findViewById(R.id.np_horas);
        npMinutos         = v.findViewById(R.id.np_minutos);
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        loadingDialog = new LoadingDialog(requireContext());

        // configurar formato de pickers en tiempo de ejecucion
        configurarPickers();

        // IMPORTANTE: evitar que el ScrollView robe el gesto si no, no se mueven los pickers
        npHoras.setOnTouchListener((view, event) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        npMinutos.setOnTouchListener((view, event) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });



        // AutoComplete con sugerencias de tareas comunes
        String[] sugerencias = requireContext().getResources()
                .getStringArray(R.array.tareas_options);
        ArrayAdapter<String> acAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias);
        etNombreTarea.setAdapter(acAdapter);
        etNombreTarea.setThreshold(1);

        btnCancelar.setOnClickListener(btn ->
                Navigation.findNavController(v).popBackStack());
        btnCrear.setOnClickListener(btn -> validarYCrearTarea());

        if (Utilidades.hayConexionInternet(requireContext())) {
            cargarHabitaciones();
            cargarUsuarios();
        } else {
            Toast.makeText(requireContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
        }

        return v;
    }

    // configurar visualizacion de numberpicker
    private void configurarPickers() {
        // -------- HORAS --------
        int maxHoras = 12;
        String[] horasValores = new String[maxHoras + 1];
        for (int i = 0; i <= maxHoras; i++) {
            horasValores[i] = i + " h";
        }
        npHoras.setMinValue(0);
        npHoras.setMaxValue(maxHoras);
        npHoras.setDisplayedValues(horasValores);
        // -------- MINUTOS --------
        int cantidadValores = 60 / stepMinutos;
        String[] minutosValores = new String[cantidadValores];
        for (int i = 0; i < cantidadValores; i++) {
            int valor = i * stepMinutos;
            minutosValores[i] = String.format("%02d m", valor);
        }
        npMinutos.setMinValue(0);
        npMinutos.setMaxValue(cantidadValores - 1);
        npMinutos.setDisplayedValues(minutosValores);
    }

    /* ════════════════════════════════════════════
       CARGA DINÁMICA DE SPINNERS DESDE BD
    ════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        String url = WebService.URL_Habitacion + "?id_hogar=" + idHogar;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
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
                error -> {
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error habitaciones", Request.Method.GET, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    private void cargarUsuarios() {
        String url = WebService.URL_Usuario + "?id_hogar=" + idHogar;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
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
                error -> {
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error usuarios", Request.Method.GET, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    private void poblarSpinnerHabitaciones() {
        if (!isAdded()) return;
        List<String> nombres = new ArrayList<>();
        nombres.add("— Selecciona habitación —");
        for (Habitacion h : listaHabitaciones) nombres.add(h.getNombre());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHabitacion.setAdapter(adapter);
    }

    private void poblarSpinnerUsuarios() {
        if (!isAdded()) return;
        List<String> nombres = new ArrayList<>();
        int indexPropio = 0;
        for (int i = 0; i < listaUsuarios.size(); i++) {
            Usuario u = listaUsuarios.get(i);
            String label = u.getNombre() != null ? u.getNombre() : "Usuario " + u.getId_usuario();
            if ("admin".equalsIgnoreCase(u.getRol())) label += " [Admin]";
            nombres.add(label);
            if (u.getId_usuario() == idUsuario) indexPropio = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAsigna.setAdapter(adapter);
        spinnerAsigna.setSelection(indexPropio);
    }

    /* ════════════════════════════════════════════
       VALIDACIÓN Y ENVÍO
    ════════════════════════════════════════════ */

    private void validarYCrearTarea() {
        String nombre = etNombreTarea.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Habitación: OBLIGATORIA para que la tarea sea visible en el hogar ──
        int habPos = spinnerHabitacion.getSelectedItemPosition();
        if (habPos <= 0 || listaHabitaciones.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Selecciona una habitación para la tarea", Toast.LENGTH_SHORT).show();
            return;
        }
        if (habPos - 1 >= listaHabitaciones.size()) {
            Toast.makeText(requireContext(), "Error al leer la habitación", Toast.LENGTH_SHORT).show();
            return;
        }
        int idHabitacion = listaHabitaciones.get(habPos - 1).getId_habitacion();

        // Duración (opcional)
        Integer duracion = null;
        int horas = npHoras.getValue();
        int minutos = npMinutos.getValue() * stepMinutos;
        duracion = horas * 60 + minutos;

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

        // Frecuencia (array en el mismo orden que frecuencia_options del XML)
        String[] freqValues = {"dia", "semana", "mes", "variable"};
        String frecuencia = freqValues[spinnerFrecuencia.getSelectedItemPosition()];

        // Usuario a asignar
        int asignarUID = idUsuario;
        int userPos = spinnerAsigna.getSelectedItemPosition();
        if (!listaUsuarios.isEmpty() && userPos >= 0 && userPos < listaUsuarios.size()) {
            asignarUID = listaUsuarios.get(userPos).getId_usuario();
        }

        // Construir JSON
        JSONObject body = new JSONObject();
        try {
            body.put("nombre",     nombre);
            body.put("frecuencia", frecuencia);
            body.put("num_veces",  numVeces);
            body.put("id_habitacion", idHabitacion);
            if (duracion != null) body.put("duracion", duracion);
            body.put("explicacion_frecuencia_variable",
                    frecuencia.equals("variable") ? "" : JSONObject.NULL);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error al preparar los datos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCrear.setEnabled(false);
        loadingDialog.show();
        final int uidAsignar = asignarUID;

        String url = WebService.URL_Tarea;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            int idTarea = -1;
                            if (response.has(WebService.JSON.DATA)
                                    && !response.isNull(WebService.JSON.DATA)) {
                                idTarea = response.getJSONObject(WebService.JSON.DATA)
                                        .optInt("autoincrement", -1);
                            }

                            if (idTarea != -1) {
                                crearAsignacion(idTarea, uidAsignar);
                            } else {
                                Toast.makeText(requireContext(),
                                        "Tarea creada", Toast.LENGTH_SHORT).show();
                                navegarAtras();
                            }
                        } else {
                            btnCrear.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al crear la tarea"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        btnCrear.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
                    btnCrear.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al crear tarea", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /** POST asignacion_tarea: asigna la tarea recién creada al usuario seleccionado. */
    private void crearAsignacion(int idTarea, int idUsuarioAsig) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_tarea",   idTarea);
            body.put("id_usuario", idUsuarioAsig);
        } catch (JSONException e) {
            if (isAdded())
                Toast.makeText(requireContext(), "Tarea creada (sin asignar)", Toast.LENGTH_SHORT).show();
            navegarAtras();
            return;
        }

        String url = WebService.URL_Asignacion_Tarea;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss();
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
                    navegarAtras();
                },
                error -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Tarea creada pero error al asignar", Toast.LENGTH_SHORT).show();
                    navegarAtras();
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /** Navega atrás de forma segura usando requireView() en lugar de la vista capturada. */
    private void navegarAtras() {
        if (isAdded() && getView() != null) {
            Navigation.findNavController(requireView()).popBackStack();
        }
    }
}
