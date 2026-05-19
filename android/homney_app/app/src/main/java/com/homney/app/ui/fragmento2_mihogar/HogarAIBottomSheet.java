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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /* ── Groq (llamada directa desde Android; AwardSpace bloquea puerto 443 saliente) ── */
    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_KEY   = "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";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    private static final String PREFS_IA       = "homney_ia";
    private static final long   MILLIS_30_DIAS = 30L * 24 * 60 * 60 * 1000;

    /** System prompt cacheado tras la primera carga de contexto. */
    private String systemPrompt    = null;
    private int    numHabs         = 0;
    private String listaHabs       = "";
    private String nombreDelHogar  = "";  // cargado desde contexto_ia o SharedPreferences

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
        idHogar        = sesion.getInt("id_hogar",       -1);
        idUsuario      = sesion.getInt("id_usuario",     -1);
        nombreDelHogar = sesion.getString("nombre_hogar", "");

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
       COMUNICACIÓN CON EL BACKEND Y GROQ
       Flujo:
         1) GET contexto_ia.php → datos del hogar desde la BD (AwardSpace OK)
         2) Construir system prompt en Android
         3) POST api.groq.com directamente (Android no tiene restricción de salida)
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

        if (systemPrompt == null) {
            // Primera vez: obtener contexto de la BD y luego llamar a Groq
            fetchContextoYLlamar(pregunta, esBienvenida);
        } else {
            // Contexto ya cacheado: llamar a Groq directamente
            llamarGroq(pregunta, esBienvenida);
        }
    }

    /** Paso 1: descarga el contexto del hogar desde el backend PHP. */
    private void fetchContextoYLlamar(final String pregunta, final boolean esBienvenida) {
        String url = WebService.URL_ContextoIA + "?id_hogar=" + idHogar;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                respStr -> {
                    try {
                        JSONObject json = new JSONObject(respStr);
                        if (!WebService.JSON.SUCCESS.equals(json.getString(WebService.JSON.STATUS))) {
                            mostrarErrorIa("❌ No se pudo cargar el contexto del hogar");
                            return;
                        }
                        JSONObject data = json.getJSONObject(WebService.JSON.DATA);

                        // Datos para el mensaje de bienvenida
                        String nhFromServer = data.optString("nombre_hogar", "");
                        if (!nhFromServer.isEmpty()) nombreDelHogar = nhFromServer;

                        JSONArray habs = data.optJSONArray("habitaciones");
                        numHabs = habs != null ? habs.length() : 0;
                        List<String> nombresHabs = new ArrayList<>();
                        if (habs != null) {
                            for (int i = 0; i < habs.length(); i++)
                                nombresHabs.add(habs.getJSONObject(i).optString("nombre", ""));
                        }
                        listaHabs = android.text.TextUtils.join(", ", nombresHabs);

                        systemPrompt = construirSystemPrompt(data);
                        llamarGroq(pregunta, esBienvenida);

                    } catch (JSONException e) {
                        mostrarErrorIa("❌ Error al procesar el contexto del hogar");
                    }
                },
                error -> mostrarErrorIa("❌ No se pudo conectar al servidor — ¿backend activo?")
        );
        req.setRetryPolicy(new DefaultRetryPolicy(15_000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        PeticionesRed.anhadirPeticionACola(req);
    }

    /** Construye el system prompt a partir del contexto recibido. */
    private String construirSystemPrompt(JSONObject data) throws JSONException {
        JSONArray miembros     = data.optJSONArray("miembros");
        JSONArray habitaciones = data.optJSONArray("habitaciones");
        JSONArray tareas       = data.optJSONArray("tareas");
        JSONArray gastosArr    = data.optJSONArray("gastos");
        JSONArray categoriasArr = data.optJSONArray("categorias");

        String hoy = Utilidades.fechaHoyEntrada();

        // Nombre del hogar: usar el campo del contexto o el valor ya cargado
        String nomHogar = data.optString("nombre_hogar", "");
        if (nomHogar.isEmpty()) nomHogar = nombreDelHogar;
        String hogarLabel = nomHogar.isEmpty() ? "la colmena" : "«" + nomHogar + "»";

        StringBuilder sb = new StringBuilder();
        sb.append("Eres «Homney Mate» 🐝, la abeja obrera del hogar compartido ").append(hogarLabel).append(". ");
        sb.append("Eres muy simpática, cariñosa y trabajadora. ");
        sb.append("Adoras a los miembros del hogar como si fueran tu abeja reina y los llamas por su nombre cuando puedes. ");
        sb.append("Siempre que menciones el hogar, llámalo por su nombre: ").append(hogarLabel).append(". ");
        sb.append("Eres pacificadora: cuando hay desequilibrios en tareas o gastos los presentas con diplomacia y propones soluciones amables y pragmáticas. ");
        sb.append("Usas emojis 🐝🍯 con moderación. Responde siempre en español. Sé concisa y práctica.\n\n");
        sb.append("RESTRICCIÓN: Solo respondes preguntas sobre el hogar (tareas, gastos, miembros, organización, convivencia). ");
        sb.append("Si te preguntan otra cosa di: «🐝 ¡Ese tema se escapa de mi colmena! Solo puedo ayudarte con asuntos de la colmena 🍯».\n\n");
        sb.append("Nombre del hogar: ").append(hogarLabel).append("\n");
        sb.append("Fecha actual: ").append(hoy).append("\n\n");

        sb.append("👥 MIEMBROS DEL HOGAR:\n");
        if (miembros != null) {
            for (int i = 0; i < miembros.length(); i++) {
                JSONObject m = miembros.getJSONObject(i);
                sb.append("• id=").append(m.optInt("id_usuario"))
                  .append(" | ").append(m.optString("nombre"))
                  .append(" — ").append(m.optString("rol")).append("\n");
            }
        }

        sb.append("\n🏠 HABITACIONES:\n");
        if (habitaciones == null || habitaciones.length() == 0) {
            sb.append("• (no hay habitaciones registradas)\n");
        } else {
            for (int i = 0; i < habitaciones.length(); i++) {
                JSONObject h = habitaciones.getJSONObject(i);
                sb.append("• id=").append(h.optInt("id_habitacion"))
                  .append(" | ").append(h.optString("nombre")).append("\n");
            }
        }

        int numTareas = tareas != null ? tareas.length() : 0;
        sb.append("\n📋 TAREAS (").append(numTareas).append("):\n");
        if (tareas != null) {
            for (int i = 0; i < tareas.length(); i++) {
                JSONObject t = tareas.getJSONObject(i);
                sb.append("• ").append(t.optString("nombre"))
                  .append(" | ").append(t.optString("habitacion"))
                  .append(" | ").append(t.optString("frecuencia"))
                  .append(" ×").append(t.optInt("num_veces")).append("\n");
            }
        }

        double total = 0;
        int numGastos = gastosArr != null ? gastosArr.length() : 0;
        if (gastosArr != null) {
            for (int i = 0; i < gastosArr.length(); i++)
                total += gastosArr.getJSONObject(i).optDouble("importe", 0);
        }
        sb.append("\n💰 ÚLTIMOS GASTOS (").append(numGastos)
          .append(" — total ").append(String.format(Locale.getDefault(), "%.2f", total))
          .append(" €):\n");
        if (gastosArr != null) {
            for (int i = 0; i < gastosArr.length(); i++) {
                JSONObject g = gastosArr.getJSONObject(i);
                sb.append("• ").append(g.optString("fecha"))
                  .append(" | ").append(g.optString("concepto"))
                  .append(" | ").append(g.optDouble("importe", 0))
                  .append(" € | ").append(g.optString("categoria")).append("\n");
            }
        }

        List<String> cats = new ArrayList<>();
        if (categoriasArr != null) {
            for (int i = 0; i < categoriasArr.length(); i++)
                cats.add(categoriasArr.optString(i, ""));
        }
        sb.append("\n🏷️ CATEGORÍAS DISPONIBLES: ")
          .append(android.text.TextUtils.join(", ", cats)).append("\n");

        sb.append("\n═══════════════════════════════════\n");
        sb.append("ACCIONES QUE PUEDES REALIZAR:\n");
        sb.append("Cuando el usuario te pida EXPLÍCITAMENTE publicar, crear una tarea o registrar un gasto,\n");
        sb.append("añade EXACTAMENTE esto al FINAL de tu respuesta (sin saltos de línea dentro del bloque):\n\n");
        sb.append("[[ACCION]]{\"tipo\":\"TIPO\",\"datos\":{...}}[[/ACCION]]\n\n");
        sb.append("TIPOS DISPONIBLES:\n\n");
        sb.append("1) publicar_muro\n   {\"tipo\":\"publicar_muro\",\"datos\":{\"titulo\":\"...\",\"cuerpo\":\"...\"}}\n\n");
        sb.append("2) crear_tarea\n   {\"tipo\":\"crear_tarea\",\"datos\":{\"nombre\":\"...\",\"frecuencia\":\"dia|semana|mes|variable\",\"num_veces\":N,\"id_habitacion\":ID_O_NULL,\"nombre_habitacion\":\"NOMBRE_O_null\",\"duracion\":MINUTOS_O_NULL,\"id_usuario_asignar\":ID_O_NULL,\"nombre_usuario_asignar\":\"NOMBRE_O_null\"}}\n\n");
        sb.append("3) crear_gasto\n   {\"tipo\":\"crear_gasto\",\"datos\":{\"concepto\":\"...\",\"importe\":DECIMAL,\"categoria\":\"CATEGORIA_EXACTA\",\"fecha\":\"").append(hoy).append("\",\"modo\":\"efectivo|transferencia|tarjeta|bizum\",\"tipo\":\"ocasional|fijo\"}}\n\n");
        sb.append("REGLAS ESTRICTAS:\n");
        sb.append("- Usa ÚNICAMENTE los IDs de las listas de arriba.\n");
        sb.append("- Usa ÚNICAMENTE las categorías de la lista. Si no encaja, usa la más parecida.\n");
        sb.append("- Si el usuario no especifica un campo opcional, usa null.\n");
        sb.append("- NUNCA incluyas [[ACCION]] si el usuario solo pregunta o conversa.\n");
        sb.append("- El JSON debe estar en UNA sola línea, sin comentarios.\n");
        sb.append("═══════════════════════════════════\n");

        return sb.toString();
    }

    /** Paso 2: llama directamente a la API de Groq desde Android. */
    private void llamarGroq(final String pregunta, final boolean esBienvenida) {
        try {
            JSONArray mensajes = new JSONArray();
            mensajes.put(new JSONObject().put("role", "system").put("content", systemPrompt));

            // Historial reciente (máx. 20 turnos)
            for (int i = Math.max(0, historial.length() - 20); i < historial.length(); i++) {
                mensajes.put(historial.get(i));
            }

            // Mensaje del usuario (o el mensaje inicial si pregunta vacía)
            String userMsg;
            if (!pregunta.isEmpty()) {
                userMsg = pregunta;
            } else if (esBienvenida) {
                String hogarBienvenida = nombreDelHogar.isEmpty()
                        ? "la colmena" : "«" + nombreDelHogar + "»";
                userMsg = "Acabo de configurar mi hogar en Homney. Soy completamente nuevo aquí y quiero saber qué puedes hacer por mí.\n\n"
                        + "Por favor:\n"
                        + "1. 🐝 Preséntate con tu personalidad de abeja trabajadora y cariñosa, y da la bienvenida al hogar " + hogarBienvenida + ".\n"
                        + "2. 📋 Explícame QUÉ PUEDES HACER por mí (tareas, gastos, muro, consejos, análisis de reparto…).\n"
                        + "3. 🏠 Menciona que el hogar " + hogarBienvenida + " tiene " + numHabs + " habitacion(es)"
                        + (numHabs > 0 ? " (" + listaHabs + ")" : "") + " y que ya las conoces.\n"
                        + "4. Termina con una frase motivadora y cálida de bienvenida usando el nombre del hogar.\n\n"
                        + "Sé cercana, entusiasta y breve (máx. 5-6 párrafos cortos).";
            } else {
                userMsg = "Saluda al hogar con tu personalidad de abeja y haz un resumen inicial:\n"
                        + "1. ¿El reparto de tareas es equilibrado? ¿Alguna habitación sin tareas?\n"
                        + "2. ¿Cuáles son las mayores partidas de gasto? ¿Algo llamativo?\n"
                        + "3. Dame 2-3 consejos concretos para mejorar el día a día.\n"
                        + "Sé breve y cariñosa.";
            }
            mensajes.put(new JSONObject().put("role", "user").put("content", userMsg));

            JSONObject body = new JSONObject();
            body.put("model",       GROQ_MODEL);
            body.put("messages",    mensajes);
            body.put("max_tokens",  1400);
            body.put("temperature", 0.7);

            final String preguntaFinal = pregunta;
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST, GROQ_URL, body,
                    response -> procesarRespuestaGroq(response, preguntaFinal),
                    error -> {
                        String msg;
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            if (code == 429) msg = "⚠️ Límite de peticiones alcanzado. Espera un momento.";
                            else if (code == 401) msg = "⚠️ Clave de API no válida.";
                            else msg = "⚠️ Error HTTP " + code + " del proveedor IA";
                        } else {
                            msg = "⚠️ Sin respuesta del proveedor IA — comprueba la conexión";
                        }
                        mostrarErrorIa(msg);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    h.put("Authorization", "Bearer " + GROQ_KEY);
                    return h;
                }
            };

            req.setRetryPolicy(new DefaultRetryPolicy(
                    30_000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            PeticionesRed.anhadirPeticionACola(req);

        } catch (JSONException e) {
            mostrarErrorIa("❌ Error al preparar la solicitud al asistente");
        }
    }

    /** Parsea la respuesta OpenAI-format de Groq y extrae texto + acción opcional. */
    private void procesarRespuestaGroq(JSONObject response, String pregunta) {
        try {
            String textoCompleto = response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // Extraer bloque de acción [[ACCION]]...[[/ACCION]]
            JSONObject accion = extraerAccion(textoCompleto);
            String texto = accion != null
                    ? textoCompleto.replaceAll("(?s)\\[\\[ACCION\\]\\].*?\\[\\[/ACCION\\]\\]", "").trim()
                    : textoCompleto;

            // Guardar turno en historial
            try {
                if (!pregunta.isEmpty()) {
                    historial.put(new JSONObject().put("role", "user").put("content", pregunta));
                }
                historial.put(new JSONObject().put("role", "assistant").put("content", texto));
            } catch (JSONException ignored) {}
            guardarHistorial();

            if (getActivity() == null) return;
            final JSONObject accionFinal = accion;
            final String textoFinal = texto;
            requireActivity().runOnUiThread(() -> {
                loadingIa.setVisibility(View.GONE);
                agregarBurbuja(textoFinal, false);
                esperando = false;
                setEntradaActiva(true);
                if (accionFinal != null) mostrarConfirmacionAccion(accionFinal);
            });

        } catch (JSONException e) {
            mostrarErrorIa("⚠️ Respuesta inesperada del proveedor IA");
        }
    }

    /** Extrae el primer bloque [[ACCION]]…[[/ACCION]] del texto del modelo. */
    private JSONObject extraerAccion(String texto) {
        try {
            Pattern p = Pattern.compile(
                    "\\[\\[ACCION\\]\\]\\s*(?:```json\\s*)?(.*?)(?:\\s*```\\s*)?\\[\\[/ACCION\\]\\]",
                    Pattern.DOTALL);
            Matcher m = p.matcher(texto);
            if (m.find()) {
                JSONObject obj = new JSONObject(m.group(1).trim());
                if (obj.has("tipo") && obj.has("datos")) return obj;
            }
        } catch (Exception ignored) {}
        return null;
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
            String hoy = Utilidades.fechaHoyEntrada();
            body.put("fecha",              datos.optString("fecha",     hoy));
            body.put("categoria",          datos.optString("categoria", "Otros"));
            body.put("concepto",           datos.optString("concepto",  "Gasto"));
            body.put("modo",               datos.optString("modo",      "efectivo"));
            body.put("tipo",               datos.optString("tipo",      "ocasional"));
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
        systemPrompt = null; // forzar re-fetch del contexto en el próximo mensaje
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
