package com.homney.app.ui.nav_crear_tarea;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
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
    private static final int STEP_MIN = 5;

    /*
     * Spinner habitación:
     *   pos 0 = "— Selecciona —"  (placeholder, inválido)
     *   pos 1 = "General"         (toda la casa)
     *   pos 2+ = habitaciones reales
     */
    private static final int POS_GENERAL = 1;

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
        configurarPickers();

        npHoras.setOnTouchListener((view, e) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true); return false; });
        npMinutos.setOnTouchListener((view, e) -> {
            view.getParent().requestDisallowInterceptTouchEvent(true); return false; });

        // AutoComplete sugerencias
        String[] sugerencias = requireContext().getResources()
                .getStringArray(R.array.tareas_options);
        etNombreTarea.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias));
        etNombreTarea.setThreshold(1);

        btnCancelar.setOnClickListener(btn ->
                Navigation.findNavController(v).popBackStack());
        btnCrear.setOnClickListener(btn -> validarYCrearTarea());

        if (Utilidades.hayConexionInternet(requireContext())) {
            cargarHabitaciones();
            cargarUsuarios();
        } else {
            Toast.makeText(requireContext(),
                    getString(R.string.sin_conexion_internet), Toast.LENGTH_SHORT).show();
        }

        return v;
    }

    private void configurarPickers() {
        String[] horasVal = new String[13];
        for (int i = 0; i <= 12; i++) horasVal[i] = i + " h";
        npHoras.setMinValue(0); npHoras.setMaxValue(12);
        npHoras.setDisplayedValues(horasVal);

        int n = 60 / STEP_MIN;
        String[] minVal = new String[n];
        for (int i = 0; i < n; i++) minVal[i] = String.format("%02d m", i * STEP_MIN);
        npMinutos.setMinValue(0); npMinutos.setMaxValue(n - 1);
        npMinutos.setDisplayedValues(minVal);
    }

    /* ════════════════════════════════════════════
       CARGA DINÁMICA DE SPINNERS
    ════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        String url = WebService.URL_Habitacion + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Habitacion>>() {}.getType();
                            RespuestaLista<Habitacion> resp = gson.fromJson(response.toString(), tipo);
                            listaHabitaciones = resp.data != null ? resp.data : new ArrayList<>();
                            poblarSpinnerHabitaciones();
                        }
                    } catch (JSONException ignored) {}
                },
                error -> { if (isAdded())
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error habitaciones", Request.Method.GET, url, error); }
        ));
    }

    private void cargarUsuarios() {
        String url = WebService.URL_Usuario + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> resp = gson.fromJson(response.toString(), tipo);
                            listaUsuarios = resp.data != null ? resp.data : new ArrayList<>();
                            poblarSpinnerUsuarios();
                        }
                    } catch (JSONException ignored) {}
                },
                error -> { if (isAdded())
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error usuarios", Request.Method.GET, url, error); }
        ));
    }

    private void poblarSpinnerHabitaciones() {
        if (!isAdded()) return;
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.spinner_selecciona_habitacion)); // 0 — placeholder
        nombres.add(getString(R.string.habitacion_general));             // 1 — General
        for (Habitacion h : listaHabitaciones) nombres.add(h.getNombre()); // 2+

        ArrayAdapter<String> a = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHabitacion.setAdapter(a);
        spinnerHabitacion.setSelection(0);
    }

    private void poblarSpinnerUsuarios() {
        if (!isAdded()) return;
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.spinner_sin_asignar)); // 0 — sin asignar
        int seleccion = 0;

        for (int i = 0; i < listaUsuarios.size(); i++) {
            Usuario u = listaUsuarios.get(i);
            nombres.add(u.getNombre() != null ? u.getNombre() : "Usuario " + u.getId_usuario());
            if (u.getId_usuario() == idUsuario) seleccion = i + 1;
        }

        ArrayAdapter<String> a = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAsigna.setAdapter(a);
        spinnerAsigna.setSelection(seleccion);
    }

    /* ════════════════════════════════════════════
       VALIDACIÓN Y ENVÍO
    ════════════════════════════════════════════ */

    private void validarYCrearTarea() {

        // ── Nombre ──────────────────────────────────
        String nombre = etNombreTarea.getText().toString().trim();
        if (nombre.isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.error_nombre_obligatorio), Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Habitación ──────────────────────────────
        int habPos = spinnerHabitacion.getSelectedItemPosition();
        if (habPos <= 0) {
            Toast.makeText(requireContext(),
                    getString(R.string.error_selecciona_habitacion), Toast.LENGTH_SHORT).show();
            return;
        }

        // ── Duración ────────────────────────────────
        int duracion = npHoras.getValue() * 60 + npMinutos.getValue() * STEP_MIN;

        // ── Nº veces ────────────────────────────────
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
                Toast.makeText(requireContext(),
                        getString(R.string.error_num_veces_invalido), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ── Frecuencia ──────────────────────────────
        String[] freqValues = {"dia", "semana", "mes", "variable"};
        String frecuencia = freqValues[spinnerFrecuencia.getSelectedItemPosition()];

        // ── Usuario a asignar ───────────────────────
        int asignarUID = -1; // -1 = sin asignar
        int userPos = spinnerAsigna.getSelectedItemPosition();
        if (userPos > 0 && userPos <= listaUsuarios.size()) {
            asignarUID = listaUsuarios.get(userPos - 1).getId_usuario();
        }

        btnCrear.setEnabled(false);
        loadingDialog.show();

        final String fNombre    = nombre;
        final String fFrecuencia = frecuencia;
        final int    fDuracion  = duracion;
        final int    fNumVeces  = numVeces;
        final int    fAsignar   = asignarUID;

        if (habPos == POS_GENERAL) {
            // ── Tarea general: necesitamos un id_habitacion válido ──────────
            // El servidor filtra tareas por habitación (JOIN).
            // Buscamos una habitación de tipo "generica" existente; si no hay, la creamos.
            resolverHabitacionGeneral(idHab ->
                    crearTareaConId(fNombre, idHab, fFrecuencia, fDuracion, fNumVeces, fAsignar));
        } else {
            // Habitación real seleccionada
            int idx = habPos - 2;
            if (idx < 0 || idx >= listaHabitaciones.size()) {
                loadingDialog.dismiss();
                btnCrear.setEnabled(true);
                Toast.makeText(requireContext(),
                        getString(R.string.error_leer_habitacion), Toast.LENGTH_SHORT).show();
                return;
            }
            int idHab = listaHabitaciones.get(idx).getId_habitacion();
            crearTareaConId(fNombre, idHab, fFrecuencia, fDuracion, fNumVeces, fAsignar);
        }
    }

    /* ════════════════════════════════════════════
       RESOLVER HABITACIÓN GENERAL
       Reutiliza la primera habitación de tipo "generica"
       del hogar; si no existe la crea automáticamente.
    ════════════════════════════════════════════ */

    interface HabitacionCallback { void onResuelta(int idHabitacion); }

    private void resolverHabitacionGeneral(HabitacionCallback cb) {
        // Buscar habitación "generica" ya existente
        for (Habitacion h : listaHabitaciones) {
            if ("generica".equalsIgnoreCase(h.getTipo())) {
                cb.onResuelta(h.getId_habitacion());
                return;
            }
        }

        // No existe → crearla en el servidor
        JSONObject body = new JSONObject();
        try {
            body.put("nombre",   "General");
            body.put("tipo",     "generica");
            body.put("id_hogar", idHogar);
        } catch (JSONException e) {
            loadingDialog.dismiss(); btnCrear.setEnabled(true);
            Toast.makeText(requireContext(),
                    getString(R.string.error_preparar_datos), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WebService.URL_Habitacion;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    if (!isAdded()) { loadingDialog.dismiss(); return; }
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            int idHab = -1;
                            if (response.has(WebService.JSON.DATA)
                                    && !response.isNull(WebService.JSON.DATA)) {
                                idHab = response.getJSONObject(WebService.JSON.DATA)
                                        .optInt("autoincrement", -1);
                            }
                            if (idHab != -1) {
                                // Añadir a la lista local para no volver a crearla
                                Habitacion nueva = new Habitacion("General", "generica", idHogar);
                                nueva.setId_habitacion(idHab);
                                listaHabitaciones.add(nueva);
                                cb.onResuelta(idHab);
                            } else {
                                loadingDialog.dismiss(); btnCrear.setEnabled(true);
                                Toast.makeText(requireContext(),
                                        getString(R.string.error_preparar_datos),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            loadingDialog.dismiss(); btnCrear.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    getString(R.string.error_preparar_datos),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        loadingDialog.dismiss(); btnCrear.setEnabled(true);
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss(); btnCrear.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error crear habitación general", Request.Method.POST, url, error);
                }
        ));
    }

    /* ════════════════════════════════════════════
       POST tarea
    ════════════════════════════════════════════ */

    private void crearTareaConId(String nombre, int idHabitacion, String frecuencia,
                                  int duracion, int numVeces, int uidAsignar) {
        JSONObject body = new JSONObject();
        try {
            body.put("nombre",        nombre);
            body.put("frecuencia",    frecuencia);
            body.put("num_veces",     numVeces);
            body.put("id_habitacion", idHabitacion);
            if (duracion > 0) body.put("duracion", duracion);
            body.put("explicacion_frecuencia_variable",
                    frecuencia.equals("variable") ? "" : JSONObject.NULL);
        } catch (JSONException e) {
            loadingDialog.dismiss(); btnCrear.setEnabled(true);
            Toast.makeText(requireContext(),
                    getString(R.string.error_preparar_datos), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WebService.URL_Tarea;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
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

                            if (idTarea != -1 && uidAsignar != -1) {
                                crearAsignacion(idTarea, uidAsignar);
                            } else {
                                // Sin asignación (usuario no seleccionado o autoincrement no devuelto)
                                String msg = (uidAsignar != -1)
                                        ? "Tarea creada, pero no se pudo asignar automáticamente"
                                        : getString(R.string.tarea_creada_sin_asignar);
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
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
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (!isAdded()) return;
                    loadingDialog.dismiss(); btnCrear.setEnabled(true);
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al crear tarea", Request.Method.POST, url, error);
                }
        ));
    }

    /* ════════════════════════════════════════════
       POST asignacion_tarea
    ════════════════════════════════════════════ */

    private void crearAsignacion(int idTarea, int idUsuarioAsig) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_tarea",   idTarea);
            body.put("id_usuario", idUsuarioAsig);
        } catch (JSONException e) {
            Toast.makeText(requireContext(),
                    getString(R.string.tarea_creada_sin_asignar), Toast.LENGTH_SHORT).show();
            navegarAtras(); return;
        }

        String url = WebService.URL_Asignacion_Tarea;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    if (!isAdded()) return;
                    try {
                        String msg = WebService.JSON.SUCCESS.equals(
                                response.getString(WebService.JSON.STATUS))
                                ? getString(R.string.tarea_creada_asignada)
                                : getString(R.string.tarea_creada_sin_asignar);
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    } catch (JSONException ignored) {}
                    navegarAtras();
                },
                error -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            getString(R.string.tarea_creada_sin_asignar), Toast.LENGTH_SHORT).show();
                    navegarAtras();
                }
        ));
    }

    private void navegarAtras() {
        if (!isAdded()) return;
        Navigation.findNavController(requireView()).navigate(
                R.id.fragmento3, null,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_home, false)
                        .build());
    }
}
