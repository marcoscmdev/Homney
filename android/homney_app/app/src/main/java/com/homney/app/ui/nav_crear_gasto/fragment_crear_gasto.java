package com.homney.app.ui.nav_crear_gasto;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.homney.app.webservice.modelo.Categoria;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class fragment_crear_gasto extends Fragment {

    /* ── Vistas ─────────────────────────────────────────── */
    EditText  etConcepto;          // R.id.et_nombre_tarea
    EditText  etImporte;           // R.id.et_importe
    EditText  etFecha;             // R.id.et_fecha_gasto
    Spinner   spinnerCategoria;    // R.id.spinner_categoria      (categorías padre)
    TextView  tvLabelSubcategoria; // R.id.tv_label_subcategoria  (hidden by default)
    Spinner   spinnerSubcategoria; // R.id.spinner_subcategoria   (hidden by default)
    Spinner   spinnerModoPago;     // R.id.spinner_modo_pago
    Spinner   spinnerTipoPago;     // R.id.spinner_tipo_pago
    Button    btnCancelar;         // R.id.btn_cancelar_tarea
    Button    btnCrear;            // R.id.btn_crear_gasto

    /* ── Sesión ─────────────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

    /* ── Datos categorías ───────────────────────────────── */
    private List<Categoria>              listaCategorias    = new ArrayList<>(); // solo raíz
    private List<Categoria>              listaHijosActuales = new ArrayList<>(); // hijos de la seleccionada
    private Map<String, List<Categoria>> hijosPorPadre      = new HashMap<>();  // padre → hijos
    private List<Usuario>                listaUsuarios       = new ArrayList<>();

    private static final String TAG = "WS_CREAR_GASTO";

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_crear_nuevo_gasto, container, false);

        etConcepto           = v.findViewById(R.id.et_nombre_tarea);
        etImporte            = v.findViewById(R.id.et_importe);
        etFecha              = v.findViewById(R.id.et_fecha_gasto);
        spinnerCategoria     = v.findViewById(R.id.spinner_categoria);
        tvLabelSubcategoria  = v.findViewById(R.id.tv_label_subcategoria);
        spinnerSubcategoria  = v.findViewById(R.id.spinner_subcategoria);
        spinnerModoPago      = v.findViewById(R.id.spinner_modo_pago);
        spinnerTipoPago      = v.findViewById(R.id.spinner_tipo_pago);
        btnCancelar          = v.findViewById(R.id.btn_cancelar_tarea);
        btnCrear             = v.findViewById(R.id.btn_crear_gasto);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        // ── Fecha ────────────────────────────────────────────
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etFecha.setText(hoy);
        etFecha.setFocusable(false);
        etFecha.setClickable(true);
        etFecha.setOnClickListener(btn -> mostrarDatePicker());

        // ── Spinners estáticos ────────────────────────────────
        poblarSpinnerEstatico(spinnerModoPago, R.array.modo_pago_options);
        poblarSpinnerEstatico(spinnerTipoPago, R.array.tipo_gasto_options);

        // ── Listener en spinner padre → mostrar/ocultar hijos ─
        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarSpinnerSubcategoria(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Botones ──────────────────────────────────────────
        btnCancelar.setOnClickListener(btn ->
                Navigation.findNavController(v).popBackStack());
        btnCrear.setOnClickListener(btn -> validarYCrearGasto(v));

        // ── Cargar spinners dinámicos ─────────────────────────
        if (Utilidades.hayConexionInternet(requireContext())) {
            cargarCategorias();
            cargarUsuarios();
        } else {
            Toast.makeText(requireContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            poblarCategoriasFallback();
        }

        return v;
    }

    /* ════════════════════════════════════════════
       DatePickerDialog
    ════════════════════════════════════════════ */

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(etFecha.getText().toString());
            if (d != null) cal.setTime(d);
        } catch (Exception ignored) {}

        new DatePickerDialog(requireContext(),
                (picker, year, month, day) ->
                        etFecha.setText(String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /* ════════════════════════════════════════════
       CARGA DE CATEGORÍAS
    ════════════════════════════════════════════ */

    private void cargarCategorias() {
        String url = WebService.URL_Categoria;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Categoria>>() {}.getType();
                            RespuestaLista<Categoria> resp =
                                    gson.fromJson(response.toString(), tipo);

                            listaCategorias = new ArrayList<>();
                            hijosPorPadre   = new HashMap<>();

                            if (resp.data != null) {
                                // Separar raíces de hijos
                                for (Categoria c : resp.data) {
                                    if (c.getCateg_padre() == null) {
                                        listaCategorias.add(c);
                                    } else {
                                        hijosPorPadre
                                                .computeIfAbsent(c.getCateg_padre(),
                                                        k -> new ArrayList<>())
                                                .add(c);
                                    }
                                }
                            }
                            poblarSpinnerCategorias();
                        }
                    } catch (JSONException ignored) {
                        poblarCategoriasFallback();
                    }
                },
                error -> {
                    poblarCategoriasFallback();
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error categorías", Request.Method.GET, url, error);
                }
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
                        }
                    } catch (JSONException ignored) {}
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error usuarios", Request.Method.GET, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       POBLAR SPINNERS
    ════════════════════════════════════════════ */

    private void poblarSpinnerCategorias() {
        List<String> nombres = new ArrayList<>();
        nombres.add("Seleccionar categoría");
        for (Categoria c : listaCategorias) nombres.add(c.getNombre());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapter);
        // Forzar que el listener procese la selección inicial (posición 0 = placeholder)
        actualizarSpinnerSubcategoria(0);
    }

    /**
     * Cuando cambia la categoría padre, carga sus hijos en el spinner de subcategoría.
     * Si no tiene hijos, oculta el spinner de subcategoría.
     * Equivale a onCategPadreChange() del web.
     */
    private void actualizarSpinnerSubcategoria(int posicionPadre) {
        // posición 0 = "Seleccionar categoría" (placeholder)
        if (posicionPadre == 0 || listaCategorias.isEmpty()
                || posicionPadre - 1 >= listaCategorias.size()) {
            ocultarSubcategoria();
            return;
        }

        String nombrePadre = listaCategorias.get(posicionPadre - 1).getNombre();
        List<Categoria> hijos = hijosPorPadre.get(nombrePadre);

        if (hijos == null || hijos.isEmpty()) {
            ocultarSubcategoria();
            return;
        }

        // Hay hijos → poblar y mostrar spinner de subcategoría
        listaHijosActuales = hijos;

        List<String> nombresHijos = new ArrayList<>();
        nombresHijos.add("Sin subcategoría");
        for (Categoria h : hijos) nombresHijos.add(h.getNombre());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, nombresHijos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubcategoria.setAdapter(adapter);

        tvLabelSubcategoria.setVisibility(View.VISIBLE);
        spinnerSubcategoria.setVisibility(View.VISIBLE);
    }

    private void ocultarSubcategoria() {
        listaHijosActuales = new ArrayList<>();
        tvLabelSubcategoria.setVisibility(View.GONE);
        spinnerSubcategoria.setVisibility(View.GONE);
    }

    /**
     * Devuelve la categoría final a enviar al backend:
     * - Si hay subcategoría seleccionada → nombre de la subcategoría
     * - Si no → nombre de la categoría padre
     * - Fallback → "Otros"
     * Equivale a getCategoria() del web.
     */
    private String getCategoriaSeleccionada() {
        // Subcategoría visible y seleccionada (> 0 para excluir "Sin subcategoría")
        if (spinnerSubcategoria.getVisibility() == View.VISIBLE) {
            int subPos = spinnerSubcategoria.getSelectedItemPosition();
            if (subPos > 0 && subPos - 1 < listaHijosActuales.size()) {
                return listaHijosActuales.get(subPos - 1).getNombre();
            }
        }
        // Categoría padre
        int catPos = spinnerCategoria.getSelectedItemPosition();
        if (catPos > 0 && !listaCategorias.isEmpty() && catPos - 1 < listaCategorias.size()) {
            return listaCategorias.get(catPos - 1).getNombre();
        }
        return "Otros";
    }

    private void poblarCategoriasFallback() {
        String[] defaults = {"Alimentación", "Suministros", "Limpieza",
                "Transporte", "Ocio", "Salud", "Hogar", "Otros"};
        listaCategorias = new ArrayList<>();
        hijosPorPadre   = new HashMap<>();
        for (String s : defaults) listaCategorias.add(new Categoria(s, null, null));
        poblarSpinnerCategorias();
    }

    private void poblarSpinnerEstatico(Spinner spinner, int arrayResId) {
        String[] items = requireContext().getResources().getStringArray(arrayResId);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /* ════════════════════════════════════════════
       VALIDACIÓN Y ENVÍO  (POST gasto + reparto)
    ════════════════════════════════════════════ */

    private void validarYCrearGasto(View v) {
        String concepto   = etConcepto.getText().toString().trim();
        String importeStr = etImporte.getText().toString().trim();
        String fecha      = etFecha.getText().toString().trim();

        if (concepto.isEmpty() || importeStr.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Rellena todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double importe;
        try {
            importe = Double.parseDouble(importeStr.replace(",", "."));
            if (importe <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Importe inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Categoría final (padre o hijo según selección)
        String categoria = getCategoriaSeleccionada();

        // Modo y tipo
        String[] modos = requireContext().getResources().getStringArray(R.array.modo_pago_options);
        String modo = modos[spinnerModoPago.getSelectedItemPosition()];
        String[] tipos = requireContext().getResources().getStringArray(R.array.tipo_gasto_options);
        String tipo = tipos[spinnerTipoPago.getSelectedItemPosition()];

        JSONObject body = new JSONObject();
        try {
            body.put("concepto",           concepto);
            body.put("importe",            importe);
            body.put("fecha",              fecha);
            body.put("categoria",          categoria);
            body.put("modo",               modo);
            body.put("tipo",               tipo);
            body.put("id_hogar",           idHogar);
            body.put("id_usuario_pagador", idUsuario);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error al preparar los datos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCrear.setEnabled(false);
        final double importeFinal = importe;

        String url = WebService.URL_Gasto;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            int idGasto = -1;
                            if (response.has(WebService.JSON.DATA)
                                    && !response.isNull(WebService.JSON.DATA)) {
                                idGasto = response.getJSONObject(WebService.JSON.DATA)
                                        .optInt("autoincrement", -1);
                            }

                            if (idGasto != -1 && !listaUsuarios.isEmpty()) {
                                crearRepartoAutomatico(idGasto, importeFinal, v);
                            } else {
                                Toast.makeText(requireContext(),
                                        "Gasto registrado ✓", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(v).popBackStack();
                            }
                        } else {
                            btnCrear.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al registrar"),
                                    Toast.LENGTH_SHORT).show();
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
                            "Error al crear gasto", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /**
     * Crea el reparto automático dividiendo el importe entre todos los miembros.
     * Equivale a crearReparto() del web.
     */
    private void crearRepartoAutomatico(int idGasto, double totalImporte, View v) {
        int n = listaUsuarios.size();
        double share = Math.round((totalImporte / n) * 1000.0) / 1000.0;

        AtomicInteger pendiente = new AtomicInteger(n);
        boolean[] errorFlag = {false};

        for (Usuario u : listaUsuarios) {
            JSONObject body = new JSONObject();
            try {
                body.put("id_gasto",   idGasto);
                body.put("id_usuario", u.getId_usuario());
                body.put("pagador",    u.getId_usuario() == idUsuario);
                body.put("importe",    share);
                body.put("abonado",    false);
            } catch (JSONException e) {
                if (pendiente.decrementAndGet() == 0)
                    finalizarCreacion(v, errorFlag[0]);
                continue;
            }

            String url = WebService.URL_RepartoGasto;
            JsonObjectRequest peticion = new JsonObjectRequest(
                    Request.Method.POST, url, body,
                    response -> {
                        try {
                            if (!response.getString(WebService.JSON.STATUS)
                                    .equals(WebService.JSON.SUCCESS)) {
                                errorFlag[0] = true;
                            }
                        } catch (JSONException ignored) { errorFlag[0] = true; }
                        if (pendiente.decrementAndGet() == 0)
                            finalizarCreacion(v, errorFlag[0]);
                    },
                    error -> {
                        errorFlag[0] = true;
                        if (pendiente.decrementAndGet() == 0)
                            finalizarCreacion(v, errorFlag[0]);
                    }
            );
            PeticionesRed.anhadirPeticionACola(peticion);
        }
    }

    private void finalizarCreacion(View v, boolean huboError) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(),
                huboError ? "Gasto registrado (reparto parcial)"
                          : "Gasto registrado y repartido ✓",
                Toast.LENGTH_SHORT).show();
        Navigation.findNavController(v).popBackStack();
    }
}
