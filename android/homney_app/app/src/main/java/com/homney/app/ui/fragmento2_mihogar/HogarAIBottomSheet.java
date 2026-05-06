package com.homney.app.ui.fragmento2_mihogar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.homney.app.R;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * BottomSheet con Homney Mate, el asistente IA del hogar.
 *
 * Capacidades:
 * - Análisis de tareas, gastos y miembros del hogar.
 * - Responde solo preguntas relacionadas con el hogar.
 * - Puede publicar en el muro, crear tareas y registrar gastos (con confirmación del usuario).
 * - Historial persistente por hogar (SharedPreferences), auto-borrado a los 30 días.
 * - Modo bienvenida: presentación inicial para usuarios recién registrados.
 */
public class HogarAIBottomSheet extends BottomSheetDialogFragment {

    /** Callback para el modo bienvenida: notifica cuando el usuario pulsa Aceptar. */
    public interface OnAceptarBienvenidaListener {
        void onAceptar();
    }

    private OnAceptarBienvenidaListener onAceptarListener;

    public void setOnAceptarBienvenidaListener(OnAceptarBienvenidaListener listener) {
        this.onAceptarListener = listener;
    }

    private static final String ARG_BIENVENIDA = "es_bienvenida";

    /** Instancia en modo bienvenida: HomneyMate se presenta al nuevo usuario. */
    public static HogarAIBottomSheet newBienvenida() {
        HogarAIBottomSheet sheet = new HogarAIBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean(ARG_BIENVENIDA, true);
        sheet.setArguments(args);
        return sheet;
    }

    private static final String PREFS_IA       = "homney_ia";
    private static final long   MILLIS_30_DIAS = 30L * 24 * 60 * 60 * 1000;

    /* ── Vistas ── */
    private LinearLayout llMensajes;
    private ScrollView   scrollChat;
    private EditText     etPregunta;
    private ImageButton  btnEnviar;
    private ImageButton  btnLimpiar;
    private ProgressBar  loadingIa;

    /* ── Estado ── */
    private final JSONArray historial = new JSONArray();
    private boolean esperando        = false;
    private boolean analisisdoUnaVez = false;
    private int     idHogar          = -1;
    private int     idUsuario        = -1;

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_hogar_ai_chat, container, false);

        llMensajes = view.findViewById(R.id.ll_mensajes);
        scrollChat = view.findViewById(R.id.scroll_chat);
        etPregunta = view.findViewById(R.id.et_pregunta);
        btnEnviar  = view.findViewById(R.id.btn_enviar_ia);
        btnLimpiar = view.findViewById(R.id.btn_limpiar_historial);
        loadingIa  = view.findViewById(R.id.loading_ia);

        SharedPreferences sesion = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar   = sesion.getInt("id_hogar",   -1);
        idUsuario = sesion.getInt("id_usuario", -1);

        btnEnviar.setOnClickListener(v -> enviarPregunta());
        btnLimpiar.setOnClickListener(v -> confirmarLimpiarHistorial());

        // ── Modo bienvenida: ocultar entrada, mostrar botón Aceptar ──────
        boolean esBienvenida = getArguments() != null
                && getArguments().getBoolean(ARG_BIENVENIDA, false);
        if (esBienvenida) {
            View barraEntrada = view.findViewById(R.id.ll_barra_entrada);
            if (barraEntrada != null) barraEntrada.setVisibility(View.GONE);
            if (btnLimpiar   != null) btnLimpiar.setVisibility(View.GONE);

            Button btnAceptar = view.findViewById(R.id.btn_aceptar_bienvenida);
            if (btnAceptar != null) {
                btnAceptar.setVisibility(View.VISIBLE);
                btnAceptar.setOnClickListener(v -> {
                    dismiss();
                    if (onAceptarListener != null) onAceptarListener.onAceptar();
                });
            }
        }

        iniciarChat();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View parent = (View) view.getParent();
        if (parent != null) {
            BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    /* ════════════════════════════════════════════
       INICIO DEL CHAT
    ════════════════════════════════════════════ */

    private void iniciarChat() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_IA, Context.MODE_PRIVATE);

        String claveHistorial    = "historial_" + idHogar;
        String claveTimestamp    = "historial_ts_" + idHogar;
        long   timestamp         = prefs.getLong(claveTimestamp, 0);
        String historialGuardado = prefs.getString(claveHistorial, null);

        // Auto-borrar si han pasado más de 30 días
        if (historialGuardado != null
                && System.currentTimeMillis() - timestamp > MILLIS_30_DIAS) {
            prefs.edit().remove(claveHistorial).remove(claveTimestamp).apply();
            historialGuardado = null;
        }

        if (historialGuardado != null) {
            try {
                JSONArray guardado = new JSONArray(historialGuardado);
                for (int i = 0; i < guardado.length(); i++) {
                    historial.put(guardado.get(i));
                    JSONObject msg   = guardado.getJSONObject(i);
                    boolean esUsuario = "user".equals(msg.optString("role"));
                    agregarBurbuja(msg.optString("content", ""), esUsuario);
                }

                analisisdoUnaVez = true;
                setEntradaActiva(true);
            } catch (JSONException e) {
                prefs.edit().remove(claveHistorial).remove(claveTimestamp).apply();
                lanzarAnalisisInicial();
            }
        } else {
            agregarBurbuja("🐝 ¡Hola! Soy Homney Mate, la abeja del hogar.\nAnalizando la colmena...", false);
            lanzarAnalisisInicial();
        }
    }

    private void lanzarAnalisisInicial() {
        boolean esBienvenida = getArguments() != null
                && getArguments().getBoolean(ARG_BIENVENIDA, false);
        llamarAsistente("", true, esBienvenida);
    }

    /* ════════════════════════════════════════════
       COMUNICACIÓN CON EL BACKEND
    ════════════════════════════════════════════ */

    private void llamarAsistente(final String pregunta, boolean esInicial) {
        llamarAsistente(pregunta, esInicial, false);
    }

    private void llamarAsistente(final String pregunta, boolean esInicial, boolean esBienvenida) {
        if (esInicial && analisisdoUnaVez) return;
        if (esInicial) analisisdoUnaVez = true;

        if (idHogar == -1) {
            agregarBurbuja("❌ No se encontró el hogar en la sesión.", false);
            return;
        }

        esperando = true;
        setEntradaActiva(false);
        loadingIa.setVisibility(View.VISIBLE);

        final String historialStr = historial.toString();
        final String modoParam    = esBienvenida ? "bienvenida" : "";

        StringRequest req = new StringRequest(Request.Method.POST, WebService.URL_AsistenteIA,
                respuestaStr -> {
                    try {
                        JSONObject json = new JSONObject(respuestaStr);
                        if (WebService.JSON.SUCCESS.equals(json.getString(WebService.JSON.STATUS))) {

                            JSONObject data  = json.getJSONObject(WebService.JSON.DATA);
                            String texto     = data.getString("respuesta");
                            JSONObject accion = data.optJSONObject("accion"); // puede ser null

                            // Guardar turno en historial
                            try {
                                if (!pregunta.isEmpty()) {
                                    historial.put(new JSONObject()
                                            .put("role", "user").put("content", pregunta));
                                }
                                historial.put(new JSONObject()
                                        .put("role", "assistant").put("content", texto));
                            } catch (JSONException ignored) {}

                            guardarHistorial();

                            // Mostrar respuesta y, si hay acción, pedir confirmación
                            if (getActivity() == null) return;
                            requireActivity().runOnUiThread(() -> {
                                loadingIa.setVisibility(View.GONE);
                                agregarBurbuja(texto, false);
                                esperando = false;
                                setEntradaActiva(true);
                                if (accion != null) {
                                    mostrarConfirmacionAccion(accion);
                                }
                            });

                        } else {
                            String msg = json.optString(WebService.JSON.DATA, "Error desconocido");
                            mostrarErrorIa(msg);
                        }
                    } catch (JSONException e) {
                        mostrarErrorIa("⚠️ Respuesta inesperada del servidor");
                    }
                },
                error -> {
                    int code = (error.networkResponse != null)
                            ? error.networkResponse.statusCode : -1;
                    String msg = (code == -1)
                            ? "⚠️ Sin respuesta del servidor — ¿está encendido el backend?"
                            : "⚠️ Error HTTP " + code + " del servidor";
                    mostrarErrorIa(msg);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id_hogar",   String.valueOf(idHogar));
                params.put("id_usuario", String.valueOf(idUsuario));
                params.put("pregunta",   pregunta);
                params.put("historial",  historialStr);
                params.put("modo",       modoParam);
                return params;
            }
        };

        req.setRetryPolicy(new DefaultRetryPolicy(
                30_000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        PeticionesRed.anhadirPeticionACola(req);
    }

    /* ════════════════════════════════════════════
       SISTEMA DE ACCIONES
    ════════════════════════════════════════════ */

    private void mostrarConfirmacionAccion(JSONObject accion) {
        String tipo = accion.optString("tipo", "");
        JSONObject datos = accion.optJSONObject("datos");
        if (datos == null) return;

        String descripcion = construirDescripcionAccion(tipo, datos);
        if (descripcion == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("¿Confirmar acción?")
                .setMessage(descripcion)
                .setPositiveButton("✅ Sí, hacerlo", (d, w) -> ejecutarAccion(tipo, datos))
                .setNegativeButton("❌ No", null)
                .show();
    }

    private String construirDescripcionAccion(String tipo, JSONObject datos) {
        try {
            switch (tipo) {
                case "publicar_muro":
                    return "📢 Publicar en el muro:\n\n"
                            + "Título: " + datos.optString("titulo", "-") + "\n\n"
                            + datos.optString("cuerpo", "");

                case "crear_tarea":
                    String habitacion = datos.optString("nombre_habitacion", null);
                    String asignar    = datos.optString("nombre_usuario_asignar", null);
                    String desc = "📋 Crear tarea: " + datos.optString("nombre", "-") + "\n"
                            + "Frecuencia: " + datos.optString("frecuencia", "-")
                            + " × " + datos.optInt("num_veces", 1) + "\n";
                    if (habitacion != null && !habitacion.equals("null"))
                        desc += "Habitación: " + habitacion + "\n";
                    if (datos.optInt("duracion", 0) > 0)
                        desc += "Duración: " + datos.optInt("duracion", 0) + " min\n";
                    if (asignar != null && !asignar.equals("null"))
                        desc += "Asignar a: " + asignar;
                    return desc.trim();

                case "crear_gasto":
                    return "💰 Registrar gasto:\n\n"
                            + datos.optString("concepto", "-") + "\n"
                            + "Importe: " + datos.optDouble("importe", 0) + " €\n"
                            + "Categoría: " + datos.optString("categoria", "-") + "\n"
                            + "Fecha: " + datos.optString("fecha", "-") + "\n"
                            + "Modo: " + datos.optString("modo", "-")
                            + " | Tipo: " + datos.optString("tipo", "-");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void ejecutarAccion(String tipo, JSONObject datos) {
        switch (tipo) {
            case "publicar_muro":   ejecutarPublicarMuro(datos);  break;
            case "crear_tarea":     ejecutarCrearTarea(datos);    break;
            case "crear_gasto":     ejecutarCrearGasto(datos);    break;
        }
    }

    // ── Publicar en el muro ──────────────────────────────────────────────

    private void ejecutarPublicarMuro(JSONObject datos) {
        try {
            JSONObject body = new JSONObject();
            body.put("titulo",      datos.optString("titulo", "Sin título"));
            body.put("cuerpo",      datos.optString("cuerpo", ""));
            body.put("id_usuario",  idUsuario);

            enviarPostJson(WebService.URL_Muro, body,
                    "✅ ¡Publicado en el muro! 🐝",
                    "❌ No se pudo publicar en el muro");
        } catch (JSONException e) {
            agregarBurbuja("❌ Error al preparar la publicación", false);
        }
    }

    // ── Crear tarea (y opcionalmente asignarla) ──────────────────────────

    private void ejecutarCrearTarea(JSONObject datos) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre",     datos.optString("nombre", "Nueva tarea"));
            body.put("frecuencia", datos.optString("frecuencia", "semanal"));
            body.put("num_veces",  datos.optInt("num_veces", 1));

            int idHabitacion = datos.optInt("id_habitacion", 0);
            if (idHabitacion > 0) body.put("id_habitacion", idHabitacion);

            int duracion = datos.optInt("duracion", 0);
            if (duracion > 0) body.put("duracion", duracion);

            final int idAsignar = datos.optInt("id_usuario_asignar", 0);
            final String nombreAsignar = datos.optString("nombre_usuario_asignar", null);

            PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                    Request.Method.POST, WebService.URL_Tarea, body,
                    response -> {
                        try {
                            if (WebService.JSON.SUCCESS.equals(response.getString(WebService.JSON.STATUS))) {
                                int idTarea = response.getJSONObject(WebService.JSON.DATA)
                                                      .getInt("autoincrement");
                                if (idAsignar > 0) {
                                    asignarTarea(idTarea, idAsignar, nombreAsignar);
                                } else {
                                    mostrarExitoAccion("✅ Tarea creada 🐝");
                                }
                            } else {
                                mostrarExitoAccion("❌ No se pudo crear la tarea");
                            }
                        } catch (JSONException e) {
                            mostrarExitoAccion("❌ Error al leer respuesta del servidor");
                        }
                    },
                    error -> mostrarExitoAccion("❌ Error de red al crear la tarea")
            ));
        } catch (JSONException e) {
            agregarBurbuja("❌ Error al preparar la tarea", false);
        }
    }

    private void asignarTarea(int idTarea, int idUsuarioAsignar, String nombreUsuario) {
        try {
            JSONObject body = new JSONObject();
            body.put("id_tarea",   idTarea);
            body.put("id_usuario", idUsuarioAsignar);

            String okMsg = "✅ Tarea creada y asignada a "
                    + (nombreUsuario != null && !nombreUsuario.equals("null")
                       ? nombreUsuario : "usuario #" + idUsuarioAsignar) + " 🐝";

            enviarPostJson(WebService.URL_AsignacionTarea, body, okMsg,
                    "⚠️ Tarea creada, pero no se pudo asignar");
        } catch (JSONException e) {
            mostrarExitoAccion("⚠️ Tarea creada, pero error al asignar");
        }
    }

    // ── Crear gasto ──────────────────────────────────────────────────────

    private void ejecutarCrearGasto(JSONObject datos) {
        try {
            JSONObject body = new JSONObject();
            String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            body.put("fecha",              datos.optString("fecha",     hoy));
            body.put("categoria",          datos.optString("categoria", "Otros"));
            body.put("concepto",           datos.optString("concepto",  "Gasto"));
            body.put("modo",               datos.optString("modo",      "efectivo"));
            body.put("tipo",               datos.optString("tipo",      "compartido"));
            body.put("importe",            datos.optDouble("importe",   0));
            body.put("id_hogar",           idHogar);
            body.put("id_usuario_pagador", idUsuario);

            enviarPostJson(WebService.URL_Gasto, body,
                    "✅ Gasto registrado 🐝",
                    "❌ No se pudo registrar el gasto");
        } catch (JSONException e) {
            agregarBurbuja("❌ Error al preparar el gasto", false);
        }
    }

    // ── Helper genérico de POST JSON ─────────────────────────────────────

    private void enviarPostJson(String url, JSONObject body, String msgOk, String msgError) {
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        String estado = response.getString(WebService.JSON.STATUS);
                        mostrarExitoAccion(WebService.JSON.SUCCESS.equals(estado) ? msgOk : msgError);
                    } catch (JSONException e) {
                        mostrarExitoAccion(msgError);
                    }
                },
                error -> mostrarExitoAccion(msgError)
        ));
    }

    private void mostrarExitoAccion(String mensaje) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> agregarBurbuja(mensaje, false));
    }

    /* ════════════════════════════════════════════
       PERSISTENCIA DEL HISTORIAL
    ════════════════════════════════════════════ */

    private void guardarHistorial() {
        if (getContext() == null) return;
        getContext().getSharedPreferences(PREFS_IA, Context.MODE_PRIVATE)
                .edit()
                .putString("historial_" + idHogar, historial.toString())
                .putLong("historial_ts_" + idHogar, System.currentTimeMillis())
                .apply();
    }

    private void confirmarLimpiarHistorial() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Limpiar historial")
                .setMessage("¿Borrar toda la conversación con Homney Mate?")
                .setPositiveButton("Borrar", (d, w) -> limpiarHistorial())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void limpiarHistorial() {
        while (historial.length() > 0) historial.remove(0);
        if (getContext() != null) {
            getContext().getSharedPreferences(PREFS_IA, Context.MODE_PRIVATE)
                    .edit()
                    .remove("historial_" + idHogar)
                    .remove("historial_ts_" + idHogar)
                    .apply();
        }
        if (llMensajes != null) llMensajes.removeAllViews();
        analisisdoUnaVez = false;
        agregarBurbuja("🐝 ¡Historial borrado! Empecemos de nuevo...", false);
        lanzarAnalisisInicial();
    }

    /* ════════════════════════════════════════════
       INTERFAZ DE CHAT
    ════════════════════════════════════════════ */

    private void enviarPregunta() {
        if (esperando) return;
        String texto = etPregunta.getText().toString().trim();
        if (texto.isEmpty()) return;
        etPregunta.setText("");
        agregarBurbuja(texto, true);
        llamarAsistente(texto, false);
    }

    private void agregarBurbuja(String texto, boolean esUsuario) {
        if (getActivity() == null || llMensajes == null) return;
        requireActivity().runOnUiThread(() -> {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            filaLp.bottomMargin = dp(8);
            fila.setLayoutParams(filaLp);
            fila.setGravity(esUsuario ? Gravity.END : Gravity.START);

            TextView tv = new TextView(requireContext());
            tv.setText(texto);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setPadding(dp(12), dp(8), dp(12), dp(8));
            tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(14));
            if (esUsuario) {
                bg.setColor(getResources().getColor(R.color.accent_dark, null));
                tv.setTextColor(0xFFFFFFFF);
            } else {
                bg.setColor(getResources().getColor(R.color.surface_two, null));
                tv.setTextColor(getResources().getColor(R.color.text, null));
            }
            tv.setBackground(bg);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            fila.addView(tv);
            llMensajes.addView(fila);
            scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void mostrarErrorIa(String mensaje) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            loadingIa.setVisibility(View.GONE);
            agregarBurbuja(mensaje, false);
            esperando = false;
            setEntradaActiva(true);
        });
    }

    private void setEntradaActiva(boolean activa) {
        if (etPregunta != null) etPregunta.setEnabled(activa);
        if (btnEnviar  != null) btnEnviar.setEnabled(activa);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}
