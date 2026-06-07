package com.homney.app.ui.fragmento3_tareas;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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
import com.homney.app.webservice.modelo.AsignacionTarea;
import com.homney.app.webservice.modelo.Tarea;
import com.homney.app.webservice.modelo.TareasRealizadas;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Fragmento3_tareas extends Fragment {

    /* ── Vistas (IDs del fragment3_tareas.xml) ───── */
    private LinearLayout containerMisTareas;
    private TextView     tvSinMisTareas;
    private TextView     tagPendientes;
    private LinearLayout containerOtrasTareas;
    private TextView     tvSinOtrasTareas;
    private LinearLayout containerCompletadas;
    private TextView     tvSinCompletadas;
    private LinearLayout sectionHistorialMesTareas;
    private TextView     tvHistorialMesTareasTitulo;
    private TextView     tagHistorialMesTareas;
    private LinearLayout containerHistorialMesTareas;
    private TextView     tvSinHistorialMesTareas;

    /* ── Colapso de secciones ─────────────────────── */
    private LinearLayout llHeaderMisTareas;
    private View         dividerMisTareas;
    private LinearLayout llHeaderOtrasTareas;
    private View         cardOtrasTareas;
    private TextView     chevronOtrasTareas;
    private LinearLayout llHeaderCompletadas;
    private View         cardCompletadas;
    private TextView     chevronCompletadas;
    private LinearLayout llHeaderHistorialMes;
    private View         cardHistorialMes;
    private TextView     chevronHistorialMes;
    private boolean      sec1Expanded = true;
    private boolean      sec2Expanded = true;
    private boolean      sec3Expanded = true;
    private boolean      sec4Expanded = true;

    private LoadingDialog loadingDialog;

    /* ── Sesión ──────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

    /* ── Datos API ───────────────────────────────── */
    private List<AsignacionTarea>  asigMias   = null;
    private List<AsignacionTarea>  allAsig    = null;
    private List<Usuario>          usuarios   = null;
    private List<TareasRealizadas> realizadas = null;
    private List<Tarea>            tareas     = null;
    private static final String TAG = "WS_TAREAS";

    private static final int[] USER_COLORS = {
            0xFFF5C518, 0xFF7CC87A, 0xFFE05C5C, 0xFFE89A30,
            0xFF9B59B6, 0xFF3498DB, 0xFFE67E22, 0xFF1ABC9C
    };

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment3_tareas, container, false);

        containerMisTareas   = root.findViewById(R.id.container_tareas_usuario);
        tvSinMisTareas       = root.findViewById(R.id.tv_sin_tareas_usuario);
        tagPendientes        = root.findViewById(R.id.tareas_pendientes);
        containerOtrasTareas = root.findViewById(R.id.container_otras_tareas);
        tvSinOtrasTareas     = root.findViewById(R.id.tv_sin_otras_tareas);
        containerCompletadas = root.findViewById(R.id.container_completadas);
        tvSinCompletadas     = root.findViewById(R.id.tv_sin_completadas);
        sectionHistorialMesTareas    = root.findViewById(R.id.section_historial_mes_tareas);
        tvHistorialMesTareasTitulo   = root.findViewById(R.id.tv_historial_mes_tareas_titulo);
        tagHistorialMesTareas        = root.findViewById(R.id.tag_historial_mes_tareas);
        containerHistorialMesTareas  = root.findViewById(R.id.container_historial_mes_tareas);
        tvSinHistorialMesTareas      = root.findViewById(R.id.tv_sin_historial_mes_tareas);

        // Colapso
        llHeaderMisTareas   = root.findViewById(R.id.ll_header_mis_tareas);
        dividerMisTareas     = root.findViewById(R.id.divider_mis_tareas);
        llHeaderOtrasTareas  = root.findViewById(R.id.ll_header_otras_tareas);
        cardOtrasTareas      = root.findViewById(R.id.card_otras_tareas);
        chevronOtrasTareas   = root.findViewById(R.id.chevron_otras_tareas);
        llHeaderCompletadas  = root.findViewById(R.id.ll_header_completadas);
        cardCompletadas      = root.findViewById(R.id.card_completadas);
        chevronCompletadas   = root.findViewById(R.id.chevron_completadas);
        llHeaderHistorialMes = root.findViewById(R.id.ll_header_historial_mes);
        cardHistorialMes     = root.findViewById(R.id.card_historial_mes);
        chevronHistorialMes  = root.findViewById(R.id.chevron_historial_mes);
        setupColapsables();

        loadingDialog = new LoadingDialog(requireContext());

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        if (idHogar == -1 || idUsuario == -1) {
            Toast.makeText(requireContext(),
                    getString(R.string.sesion_no_valida), Toast.LENGTH_LONG).show();
        }

        // Los datos se cargan en onResume para refrescarse al volver de crear/editar
        return root;
    }

    /** Configura el comportamiento colapsar/expandir de cada sección. */
    private void setupColapsables() {
        // Sección 1 — Mis tareas (header dentro del CardView)
        if (llHeaderMisTareas != null) {
            llHeaderMisTareas.setClickable(true);
            llHeaderMisTareas.setFocusable(true);
            llHeaderMisTareas.setOnClickListener(v -> {
                sec1Expanded = !sec1Expanded;
                int vis = sec1Expanded ? View.VISIBLE : View.GONE;
                if (dividerMisTareas != null)  dividerMisTareas.setVisibility(vis);
                containerMisTareas.setVisibility(vis);
                tvSinMisTareas.setVisibility(sec1Expanded ? View.VISIBLE : View.GONE);
                // Actualiza el chevron del badge de pendientes (usa tag como chevron visual)
                tagPendientes.setText(sec1Expanded
                        ? tagPendientes.getText().toString().replace("▼", "").trim()
                        : "▼");
            });
        }

        // Sección 2 — Otras tareas
        if (llHeaderOtrasTareas != null && cardOtrasTareas != null) {
            llHeaderOtrasTareas.setClickable(true);
            llHeaderOtrasTareas.setFocusable(true);
            llHeaderOtrasTareas.setOnClickListener(v -> {
                sec2Expanded = !sec2Expanded;
                cardOtrasTareas.setVisibility(sec2Expanded ? View.VISIBLE : View.GONE);
                if (chevronOtrasTareas != null)
                    chevronOtrasTareas.setText(sec2Expanded ? "▲" : "▼");
            });
        }

        // Sección 3 — Completadas este mes
        if (llHeaderCompletadas != null && cardCompletadas != null) {
            llHeaderCompletadas.setClickable(true);
            llHeaderCompletadas.setFocusable(true);
            llHeaderCompletadas.setOnClickListener(v -> {
                sec3Expanded = !sec3Expanded;
                cardCompletadas.setVisibility(sec3Expanded ? View.VISIBLE : View.GONE);
                if (chevronCompletadas != null)
                    chevronCompletadas.setText(sec3Expanded ? "▲" : "▼");
            });
        }

        // Sección 4 — Historial mes anterior
        if (llHeaderHistorialMes != null && cardHistorialMes != null) {
            llHeaderHistorialMes.setClickable(true);
            llHeaderHistorialMes.setFocusable(true);
            llHeaderHistorialMes.setOnClickListener(v -> {
                sec4Expanded = !sec4Expanded;
                cardHistorialMes.setVisibility(sec4Expanded ? View.VISIBLE : View.GONE);
                if (chevronHistorialMes != null)
                    chevronHistorialMes.setText(sec4Expanded ? "▲" : "▼");
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (idHogar != -1 && idUsuario != -1
                && Utilidades.hayConexionInternet(requireContext())) {
            resetContainers();
            cargarDatos();
        } else if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.sin_conexion_internet), Toast.LENGTH_SHORT).show();
        }
    }

    /** Limpia el estado anterior para evitar duplicados al recargar. */
    private void resetContainers() {
        asigMias = null; allAsig = null; usuarios = null; realizadas = null; tareas = null;
        if (containerMisTareas   != null) containerMisTareas.removeAllViews();
        if (containerOtrasTareas != null) containerOtrasTareas.removeAllViews();
        if (containerCompletadas != null) containerCompletadas.removeAllViews();
        if (containerHistorialMesTareas != null) containerHistorialMesTareas.removeAllViews();
    }

    /* ════════════════════════════════════════════
       CARGA DE DATOS  (5 peticiones paralelas)
    ════════════════════════════════════════════ */

    private void cargarDatos() {
        loadingDialog.show();
        final AtomicInteger pendientes = new AtomicInteger(5);

        // 1. Mis asignaciones (para saber qué tareas son "mías")
        lanzarPeticion(
                WebService.URL_Asignacion_Tarea + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> { asigMias = resp; if (pendientes.decrementAndGet() == 0) intentarRender(); });

        // 2. Todas las asignaciones del hogar (para mostrar badges de asignados)
        //    Filtrado por id_hogar gracias al nuevo soporte en asignacion_tarea.php
        lanzarPeticion(
                WebService.URL_Asignacion_Tarea + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> { allAsig = resp; if (pendientes.decrementAndGet() == 0) intentarRender(); });

        // 3. Usuarios del hogar (para los badges de nombre)
        lanzarPeticion(
                WebService.URL_Usuario + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Usuario>>() {}.getType(),
                (List<Usuario> resp) -> { usuarios = resp; if (pendientes.decrementAndGet() == 0) intentarRender(); });

        // 4. Tareas realizadas por el usuario (para detectar "completadas hoy")
        lanzarPeticion(
                WebService.URL_Tarea_Realizada + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<TareasRealizadas>>() {}.getType(),
                (List<TareasRealizadas> resp) -> { realizadas = resp; if (pendientes.decrementAndGet() == 0) intentarRender(); });

        // 5. Todas las tareas del hogar (una sola petición con JOIN, no por habitación)
        lanzarPeticion(
                WebService.URL_Tarea + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Tarea>>() {}.getType(),
                (List<Tarea> resp) -> { tareas = resp; if (pendientes.decrementAndGet() == 0) intentarRender(); });
    }

    private void intentarRender() {
        if (asigMias != null && allAsig != null
                && usuarios != null && realizadas != null && tareas != null) {
            if (isAdded()) {
                loadingDialog.dismiss();
                renderTareas();
            }
        }
    }

    /* ════════════════════════════════════════════
       HELPER: petición GET genérica
    ════════════════════════════════════════════ */

    private <T> void lanzarPeticion(String url, Type tipo,
                                    java.util.function.Consumer<List<T>> callback) {
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            RespuestaLista<T> resp = gson.fromJson(response.toString(), tipo);
                            callback.accept(resp.data != null ? resp.data : new ArrayList<>());
                        } else {
                            callback.accept(new ArrayList<>());
                        }
                    } catch (JSONException e) {
                        callback.accept(new ArrayList<>());
                    }
                },
                error -> {
                    if (isAdded()) {
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error petición", Request.Method.GET, url, error);
                    }
                    callback.accept(new ArrayList<>());
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       RENDERIZADO
    ════════════════════════════════════════════ */

    private void renderTareas() {
        if (!isAdded()) return;

        Map<Integer, Tarea>   tareaMap = new HashMap<>();
        Map<Integer, Usuario> userMap  = new HashMap<>();
        Map<Integer, Integer> colorMap = new HashMap<>();
        int ci = 0;
        for (Tarea t : tareas) tareaMap.put(t.getId_tarea(), t);
        for (Usuario u : usuarios) {
            userMap.put(u.getId_usuario(), u);
            colorMap.put(u.getId_usuario(), USER_COLORS[ci++ % USER_COLORS.length]);
        }

        Set<Integer> idsMias = new HashSet<>();
        for (AsignacionTarea a : asigMias) idsMias.add(a.getId_tarea());

        Map<Integer, List<Integer>> asigByTarea = new HashMap<>();
        for (AsignacionTarea a : allAsig) {
            asigByTarea.computeIfAbsent(a.getId_tarea(), k -> new ArrayList<>())
                       .add(a.getId_usuario());
        }

        // Mapa: id_tarea → realización más reciente (realizadas ya vienen ORDER BY fecha DESC)
        Map<Integer, TareasRealizadas> ultimaRealizacion = new HashMap<>();
        for (TareasRealizadas r : realizadas) {
            if (!ultimaRealizacion.containsKey(r.getId_tarea()))
                ultimaRealizacion.put(r.getId_tarea(), r);
        }

        String mesActual = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(new Date());
        java.util.Calendar prevCal = java.util.Calendar.getInstance();
        prevCal.add(java.util.Calendar.MONTH, -1);
        String mesPrev  = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(prevCal.getTime());
        String nombreMesPrev = new SimpleDateFormat("MMMM yyyy", new Locale("es")).format(prevCal.getTime());

        // Completadas este mes (para la sección 3)
        List<TareasRealizadas> completadasEsteMes = new ArrayList<>();
        // Completadas el mes anterior (para historial)
        List<TareasRealizadas> completadasMesPrev = new ArrayList<>();
        for (TareasRealizadas r : realizadas) {
            if (r.getFecha_realizacion() == null) continue;
            if (r.getFecha_realizacion().startsWith(mesActual))
                completadasEsteMes.add(r);
            else if (r.getFecha_realizacion().startsWith(mesPrev))
                completadasMesPrev.add(r);
        }

        // Separar: mis tareas PENDIENTES (según frecuencia) / otras
        List<Tarea> misTareas   = new ArrayList<>();
        List<Tarea> otrasTareas = new ArrayList<>();
        for (Tarea t : tareas) {
            if (idsMias.contains(t.getId_tarea())) {
                if (esPendiente(t, ultimaRealizacion)) misTareas.add(t);
                // Si no es pendiente (ya completada en período actual) → no va a ninguna lista de arriba,
                // aparecerá en "Completadas este mes"
            } else {
                otrasTareas.add(t);
            }
        }

        LayoutInflater inf = LayoutInflater.from(requireContext());

        // ─── Sección 1: Mis tareas ───────────────────────────
        tvSinMisTareas.setVisibility(View.GONE);
        long pendCount = misTareas.size();
        tagPendientes.setText(pendCount + (pendCount == 1 ? " pendiente" : " pendientes"));

        if (misTareas.isEmpty()) {
            tvSinMisTareas.setText(getString(R.string.sin_mis_tareas));
            tvSinMisTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : misTareas) {
                FrameLayout rootFrame = new FrameLayout(requireContext());
                rootFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout deleteBtn = new LinearLayout(requireContext());
                deleteBtn.setBackgroundColor(0xFFE05C5C);
                deleteBtn.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                        dp(80), ViewGroup.LayoutParams.MATCH_PARENT);
                btnLp.gravity = Gravity.END;
                deleteBtn.setLayoutParams(btnLp);
                ImageView icDelete = new ImageView(requireContext());
                icDelete.setImageResource(android.R.drawable.ic_menu_delete);
                icDelete.setColorFilter(Color.WHITE);
                deleteBtn.addView(icDelete);
                rootFrame.addView(deleteBtn);

                View row = inf.inflate(R.layout.item_tarea, rootFrame, false);
                row.setBackgroundColor(0xFFFFFFFF);
                rootFrame.addView(row);

                rellenarFilaTarea(row, deleteBtn, t, true, new HashSet<>(), asigByTarea, userMap, colorMap);
                containerMisTareas.addView(rootFrame);
                agregarDivider(containerMisTareas);
            }
        }

        // ─── Sección 2: Otras tareas del hogar ──────────────
        tvSinOtrasTareas.setVisibility(View.GONE);
        if (otrasTareas.isEmpty()) {
            tvSinOtrasTareas.setText(getString(R.string.sin_tareas_hogar));
            tvSinOtrasTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : otrasTareas) {
                View row = inf.inflate(R.layout.item_tarea, containerOtrasTareas, false);
                rellenarFilaTarea(row, null, t, false, new HashSet<>(), asigByTarea, userMap, colorMap);
                containerOtrasTareas.addView(row);
                agregarDivider(containerOtrasTareas);
            }
        }

        // ─── Sección 3: Completadas este mes ─────────────────
        tvSinCompletadas.setVisibility(View.GONE);
        if (completadasEsteMes.isEmpty()) {
            tvSinCompletadas.setText(getString(R.string.sin_tareas_completadas_este_mes));
            tvSinCompletadas.setVisibility(View.VISIBLE);
        } else {
            for (TareasRealizadas r : completadasEsteMes) {
                Tarea t = tareaMap.get(r.getId_tarea());
                View row = inf.inflate(R.layout.item_tarea, containerCompletadas, false);
                ((TextView) row.findViewById(R.id.tv_nombre_tarea))
                        .setText(t != null ? t.getNombre() : "Tarea #" + r.getId_tarea());
                String obs = r.getObservaciones();
                ((TextView) row.findViewById(R.id.tv_frecuencia))
                        .setText((obs != null && !obs.isEmpty())
                                ? obs : formatFecha(r.getFecha_realizacion()));
                TextView tvCheck = row.findViewById(R.id.tv_check);
                tvCheck.setText("✓");
                tvCheck.setBackgroundResource(R.drawable.bg_tag_green);
                TextView tvTag = row.findViewById(R.id.tv_tag_estado);
                tvTag.setText(getString(R.string.tag_completada));
                tvTag.setBackgroundResource(R.drawable.bg_tag_green);
                containerCompletadas.addView(row);
                agregarDivider(containerCompletadas);
            }
        }

        // ─── Sección 4: Historial mes anterior ───────────────
        if (!completadasMesPrev.isEmpty() && sectionHistorialMesTareas != null) {
            sectionHistorialMesTareas.setVisibility(View.VISIBLE);
            String titulo = capitalize(nombreMesPrev);
            tvHistorialMesTareasTitulo.setText(titulo);
            tagHistorialMesTareas.setText(completadasMesPrev.size()
                    + (completadasMesPrev.size() == 1 ? " completada" : " completadas"));

            containerHistorialMesTareas.removeAllViews();
            for (TareasRealizadas r : completadasMesPrev) {
                Tarea t = tareaMap.get(r.getId_tarea());
                View row = inf.inflate(R.layout.item_tarea, containerHistorialMesTareas, false);
                ((TextView) row.findViewById(R.id.tv_nombre_tarea))
                        .setText(t != null ? t.getNombre() : "Tarea #" + r.getId_tarea());
                String obs = r.getObservaciones();
                ((TextView) row.findViewById(R.id.tv_frecuencia))
                        .setText((obs != null && !obs.isEmpty())
                                ? obs : formatFecha(r.getFecha_realizacion()));
                TextView tvCheck = row.findViewById(R.id.tv_check);
                tvCheck.setText("✓");
                tvCheck.setBackgroundResource(R.drawable.bg_tag_muted);
                tvCheck.setAlpha(0.7f);
                TextView tvTag = row.findViewById(R.id.tv_tag_estado);
                tvTag.setText(getString(R.string.tag_anterior));
                tvTag.setBackgroundResource(R.drawable.bg_tag_muted);
                containerHistorialMesTareas.addView(row);
                agregarDivider(containerHistorialMesTareas);
            }
        }
    }

    /* ════════════════════════════════════════════
       RELLENA UNA FILA DE TAREA
    ════════════════════════════════════════════ */

    private void rellenarFilaTarea(View row, View deleteBtn, Tarea t, boolean esMia,
                                   Set<Integer> completadasHoy,
                                   Map<Integer, List<Integer>> asigByTarea,
                                   Map<Integer, Usuario> userMap,
                                   Map<Integer, Integer> colorMap) {

        boolean yaHecha = completadasHoy.contains(t.getId_tarea());
        ((TextView) row.findViewById(R.id.tv_nombre_tarea)).setText(
                t.getNombre() != null ? t.getNombre() : "");

        StringBuilder fr = new StringBuilder(labelFrecuencia(t.getFrecuencia()));
        if (t.getNum_veces() != null
                && t.getNum_veces().matches("\\d+")
                && Integer.parseInt(t.getNum_veces()) > 1)
            fr.append(" · ").append(t.getNum_veces()).append("x");
        fr.append(t.getDuracion() != null
                ? " · " + t.getDuracion() + " min" : " · Sin duración");
        TextView tvFrec = row.findViewById(R.id.tv_frecuencia);
        tvFrec.setText(fr.toString());

        // Checkbox visual
        TextView tvCheck = row.findViewById(R.id.tv_check);
        if (esMia && yaHecha) {
            tvCheck.setText("✓");
            tvCheck.setBackgroundResource(R.drawable.bg_tag_green);
        } else {
            tvCheck.setText("");
            tvCheck.setBackgroundResource(R.drawable.bg_tag_muted);
            if (!esMia) tvCheck.setAlpha(0.4f);
        }

        // Badges de asignados
        LinearLayout llAsig = row.findViewById(R.id.ll_asignados);
        List<Integer> asigs = asigByTarea.containsKey(t.getId_tarea())
                ? asigByTarea.get(t.getId_tarea()) : new ArrayList<>();
        for (Integer uid : asigs) {
            Usuario u = userMap.get(uid);
            // ── FIX: null-safety en getNombre() ──────────────────────────
            String nombre = (u != null && u.getNombre() != null && !u.getNombre().isEmpty())
                    ? u.getNombre() : "?";
            char inicial = nombre.charAt(0);

            TextView badge = new TextView(requireContext());
            int sz = dp(24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
            lp.rightMargin = dp(3);
            badge.setLayoutParams(lp);
            badge.setGravity(android.view.Gravity.CENTER);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(Color.WHITE);
            badge.setBackgroundColor(colorMap.containsKey(uid) ? colorMap.get(uid) : USER_COLORS[0]);
            badge.setText(String.valueOf(inicial).toUpperCase(Locale.getDefault()));
            llAsig.addView(badge);
        }

        // Para "otras tareas": mostrar "Asignado a: …" o "Sin asignar"
        if (!esMia) {
            LinearLayout bodyLayout = (LinearLayout) llAsig.getParent();
            TextView tvAsig = new TextView(requireContext());
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvLp.topMargin = dp(3);
            tvAsig.setLayoutParams(tvLp);
            tvAsig.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvAsig.setTypeface(null, Typeface.ITALIC);

            if (asigs.isEmpty()) {
                tvAsig.setText(getString(R.string.sin_asignar));
                tvAsig.setTextColor(0xFFBCAB88);
            } else {
                StringBuilder names = new StringBuilder("Asignado a: ");
                for (int i = 0; i < asigs.size(); i++) {
                    Usuario u = userMap.get(asigs.get(i));
                    names.append(u != null ? u.getNombre() : "#" + asigs.get(i));
                    if (i < asigs.size() - 1) names.append(", ");
                }
                tvAsig.setText(names.toString());
                tvAsig.setTextColor(0xFF5C4A2A);
            }
            bodyLayout.addView(tvAsig);
        }

        // Tag de estado
        TextView tvTag = row.findViewById(R.id.tv_tag_estado);
        if (yaHecha) {
            tvTag.setText(getString(R.string.tag_hecha_hoy));
            tvTag.setBackgroundResource(R.drawable.bg_tag_green);
        } else {
            tvTag.setText(labelFrecuencia(t.getFrecuencia()).toUpperCase(Locale.getDefault()));
            tvTag.setBackgroundResource(R.drawable.bg_tag_muted);
        }

        // ── Click + swipe: solo "mis tareas" PENDIENTES ──
        if (esMia && !yaHecha) {
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true);
            row.setForeground(requireContext().getDrawable(outValue.resourceId));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v ->
                    mostrarDialogMarcarRealizada(t, row, tvFrec, tvCheck, tvTag));

            final float[] x1 = {0};
            final float[] initialTranslationX = {0};
            final int MAX_SWIPE = dp(-80);

            row.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1[0] = event.getRawX();
                        initialTranslationX[0] = v.getTranslationX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - x1[0];
                        float newTranslationX = initialTranslationX[0] + deltaX;
                        if (newTranslationX <= 0 && newTranslationX >= MAX_SWIPE)
                            v.setTranslationX(newTranslationX);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (v.getTranslationX() < MAX_SWIPE / 2f) {
                            v.animate().translationX(MAX_SWIPE).setDuration(200).start();
                        } else {
                            v.animate().translationX(0).setDuration(200).start();
                            if (Math.abs(v.getTranslationX()) < 10) v.performClick();
                        }
                        return true;
                }
                return false;
            });

            if (deleteBtn != null)
                deleteBtn.setOnClickListener(v -> borrarTarea(t, (View) row.getParent()));
        }

        // ── Click: "otras tareas" → asignarse ──────────────────
        if (!esMia) {
            TypedValue outValue2 = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue2, true);
            row.setForeground(requireContext().getDrawable(outValue2.resourceId));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> mostrarDialogAsignarme(t, asigByTarea, userMap));
        }
    }

    /* ════════════════════════════════════════════
       DIALOG: ¿Confirmas realizar la tarea?
    ════════════════════════════════════════════ */

    private void mostrarDialogMarcarRealizada(Tarea t, View row,
                                              TextView tvFrec, TextView tvCheck, TextView tvTag) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView tvNombreTarea = new TextView(requireContext());
        tvNombreTarea.setText("\"" + t.getNombre() + "\"");
        tvNombreTarea.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNombreTarea.setTextColor(0xFF5C4A2A);
        tvNombreTarea.setTypeface(null, Typeface.ITALIC);
        LinearLayout.LayoutParams lpNombre = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpNombre.bottomMargin = dp(14);
        tvNombreTarea.setLayoutParams(lpNombre);
        layout.addView(tvNombreTarea);

        // ── Label getString(R.string.tiempo_empleado) ──────────────────────────────────────────
        TextView tvLabelDur = new TextView(requireContext());
        tvLabelDur.setText(getString(R.string.tiempo_empleado));
        tvLabelDur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelDur.setTypeface(null, Typeface.BOLD);
        tvLabelDur.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLabel.bottomMargin = dp(8);
        tvLabelDur.setLayoutParams(lpLabel);
        layout.addView(tvLabelDur);

        // ── Fila horizontal con npHoras y npMinutos ──────────────────────────
        // Misma mecánica que fragment_crear_tarea: valores en pasos de 5 min
        final int stepMin = 5;
        final int maxHoras = 12;
        final int cantidadMin = 60 / stepMin;   // 12 valores: 00,05,10,...,55

        // Construir displayedValues
        String[] horasValores = new String[maxHoras + 1];
        for (int i = 0; i <= maxHoras; i++) horasValores[i] = i + " h";

        String[] minutosValores = new String[cantidadMin];
        for (int i = 0; i < cantidadMin; i++)
            minutosValores[i] = String.format("%02d m", i * stepMin);

        // Pre-poblar según duracion estimada de la tarea
        int preHoras = 0, preMinIdx = 0;
        if (t.getDuracion() != null && t.getDuracion() > 0) {
            preHoras  = Math.min(t.getDuracion() / 60, maxHoras);
            int resto = t.getDuracion() % 60;
            // Redondear al step más cercano
            preMinIdx = Math.min(Math.round((float) resto / stepMin), cantidadMin - 1);
        }

        NumberPicker npHoras = new NumberPicker(requireContext());
        npHoras.setMinValue(0);
        npHoras.setMaxValue(maxHoras);
        npHoras.setDisplayedValues(horasValores);
        npHoras.setValue(preHoras);

        NumberPicker npMinutos = new NumberPicker(requireContext());
        npMinutos.setMinValue(0);
        npMinutos.setMaxValue(cantidadMin - 1);
        npMinutos.setDisplayedValues(minutosValores);
        npMinutos.setValue(preMinIdx);

        LinearLayout filaPickets = new LinearLayout(requireContext());
        filaPickets.setOrientation(LinearLayout.HORIZONTAL);
        filaPickets.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lpFila = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpFila.bottomMargin = dp(14);
        filaPickets.setLayoutParams(lpFila);
        filaPickets.addView(npHoras);
        filaPickets.addView(npMinutos);
        layout.addView(filaPickets);

        TextView tvLabelObs = new TextView(requireContext());
        tvLabelObs.setText(getString(R.string.observaciones_opcional));
        tvLabelObs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelObs.setTypeface(null, Typeface.BOLD);
        tvLabelObs.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLabelObs = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLabelObs.bottomMargin = dp(4);
        tvLabelObs.setLayoutParams(lpLabelObs);
        layout.addView(tvLabelObs);

        EditText etObservaciones = new EditText(requireContext());
        etObservaciones.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etObservaciones.setHint(getString(R.string.hint_como_fue_tarea));
        etObservaciones.setMinLines(2);
        etObservaciones.setMaxLines(4);
        layout.addView(etObservaciones);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_confirmar_tarea_titulo))
                .setView(layout)
                .setPositiveButton(getString(R.string.confirmar), (dialog, which) -> {
                    int totalMinutos = npHoras.getValue() * 60 + npMinutos.getValue() * stepMin;
                    String durStr = totalMinutos > 0 ? String.valueOf(totalMinutos) : null;
                    String obs    = etObservaciones.getText().toString().trim();
                    marcarTareaRealizada(t, durStr, obs, row, tvFrec, tvCheck, tvTag);
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    /* ════════════════════════════════════════════
       DELETE: eliminar tarea
       Paso 1: DELETE asignacion_tarea.php?id_tarea=X
       Paso 2: DELETE tarea.php?id_tarea=X
    ════════════════════════════════════════════ */

    private void borrarTarea(Tarea t, View rootFrame) {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_eliminar_tarea_titulo))
                .setMessage(getString(R.string.dialog_eliminar_tarea_msg, t.getNombre()))
                .setPositiveButton(getString(R.string.eliminar), (dialog, which) ->
                        eliminarAsignacionesYTarea(t, rootFrame))
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void eliminarAsignacionesYTarea(Tarea t, View rootFrame) {
        loadingDialog.show();
        String urlAsig  = WebService.URL_Asignacion_Tarea + "?id_tarea=" + t.getId_tarea();
        String urlTarea = WebService.URL_Tarea            + "?id_tarea=" + t.getId_tarea();

        JsonObjectRequest deleteTarea = new JsonObjectRequest(
                Request.Method.DELETE, urlTarea, null,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.tarea_eliminada), Toast.LENGTH_SHORT).show();
                            ViewGroup parent = (ViewGroup) rootFrame.getParent();
                            if (parent != null) parent.removeView(rootFrame);
                        } else {
                            Toast.makeText(requireContext(),
                                    getString(R.string.no_pudo_eliminar_tarea), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al eliminar tarea", Request.Method.DELETE, urlTarea, error);
                }
        );

        JsonObjectRequest deleteAsig = new JsonObjectRequest(
                Request.Method.DELETE, urlAsig, null,
                respAsig -> PeticionesRed.anhadirPeticionACola(deleteTarea),
                error -> {
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al eliminar asignaciones", Request.Method.DELETE, urlAsig, error);
                    PeticionesRed.anhadirPeticionACola(deleteTarea);
                }
        );
        PeticionesRed.anhadirPeticionACola(deleteAsig);
    }

    /* ════════════════════════════════════════════
       POST: registrar tarea como realizada
    ════════════════════════════════════════════ */

    private void marcarTareaRealizada(Tarea t, String durReal, String obs,
                                      View row, TextView tvFrec,
                                      TextView tvCheck, TextView tvTag) {
        loadingDialog.show();
        String fechaAhora = Utilidades.FMT_ENTRADA_DATETIME.format(new Date());

        JSONObject body = new JSONObject();
        try {
            body.put("id_tarea",          t.getId_tarea());
            body.put("id_usuario",        idUsuario);
            body.put("fecha_realizacion", fechaAhora);
            if (durReal != null && !durReal.isEmpty())
                body.put("duracion_real", Integer.parseInt(durReal));
            if (obs != null && !obs.isEmpty())
                body.put("observaciones", obs);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.error_preparar_registro), Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WebService.URL_Tarea_Realizada;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // ── 1. Actualizar contador de pendientes ───────────
                            try {
                                String txt = tagPendientes.getText().toString();
                                int count = Integer.parseInt(txt.split(" ")[0]);
                                if (count > 0) count--;
                                tagPendientes.setText(count + (count == 1 ? " pendiente" : " pendientes"));
                            } catch (Exception ignored) {}

                            // ── 2. Animar y quitar la fila de "Mis tareas" ────
                            ViewGroup rootFrame = (ViewGroup) row.getParent();
                            if (rootFrame != null) {
                                rootFrame.animate()
                                        .alpha(0f)
                                        .translationX(-rootFrame.getWidth())
                                        .setDuration(300)
                                        .withEndAction(() -> {
                                            if (!isAdded()) return;
                                            int idx = containerMisTareas.indexOfChild(rootFrame);
                                            if (idx >= 0) {
                                                containerMisTareas.removeViewAt(idx); // quita el FrameLayout
                                                // quita el divider que queda en esa posición
                                                if (idx < containerMisTareas.getChildCount())
                                                    containerMisTareas.removeViewAt(idx);
                                            }
                                            // Si el contenedor quedó vacío, mostrar mensaje
                                            if (containerMisTareas.getChildCount() == 0) {
                                                tvSinMisTareas.setText(getString(R.string.sin_mis_tareas));
                                                tvSinMisTareas.setVisibility(View.VISIBLE);
                                            }
                                        }).start();
                            }

                            // ── 3. Añadir fila al inicio de "Completadas esta semana" ──
                            tvSinCompletadas.setVisibility(View.GONE);
                            LayoutInflater inf2 = LayoutInflater.from(requireContext());
                            View compRow = inf2.inflate(R.layout.item_tarea, containerCompletadas, false);
                            ((TextView) compRow.findViewById(R.id.tv_nombre_tarea))
                                    .setText(t.getNombre());
                            ((TextView) compRow.findViewById(R.id.tv_frecuencia))
                                    .setText((obs != null && !obs.isEmpty())
                                            ? obs : getString(R.string.completada_ahora));
                            TextView ck = compRow.findViewById(R.id.tv_check);
                            ck.setText("✓");
                            ck.setBackgroundResource(R.drawable.bg_tag_green);
                            TextView tg = compRow.findViewById(R.id.tv_tag_estado);
                            tg.setText(getString(R.string.tag_completada));
                            tg.setBackgroundResource(R.drawable.bg_tag_green);
                            containerCompletadas.addView(compRow, 0);
                            // Divider justo debajo de la nueva fila
                            View div = new View(requireContext());
                            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                            divLp.leftMargin = dp(50);
                            div.setLayoutParams(divLp);
                            div.setBackgroundColor(0xFFE8DFC0);
                            containerCompletadas.addView(div, 1);

                            Toast.makeText(requireContext(),
                                    getString(R.string.tarea_completada), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al registrar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al marcar realizada", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       DIALOG: asignarse una tarea de "Otras tareas"
    ════════════════════════════════════════════ */

    private void mostrarDialogAsignarme(Tarea t,
                                        Map<Integer, List<Integer>> asigByTarea,
                                        Map<Integer, Usuario> userMap) {
        List<Integer> asigs = asigByTarea.containsKey(t.getId_tarea())
                ? asigByTarea.get(t.getId_tarea()) : new ArrayList<>();

        StringBuilder msg = new StringBuilder();
        if (asigs.isEmpty()) {
            msg.append("Esta tarea no está asignada a nadie.\n\n¿Quieres asignártela?");
        } else {
            msg.append("Asignada a: ");
            for (int i = 0; i < asigs.size(); i++) {
                Usuario u = userMap.get(asigs.get(i));
                msg.append(u != null ? u.getNombre() : "#" + asigs.get(i));
                if (i < asigs.size() - 1) msg.append(", ");
            }
            msg.append(".\n\n¿Quieres asignártela también a ti?");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("«" + t.getNombre() + "»")
                .setMessage(msg.toString())
                .setPositiveButton(getString(R.string.dialog_asignar_tarea_btn), (dialog, which) -> asignarseaTarea(t))
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    private void asignarseaTarea(Tarea t) {
        loadingDialog.show();
        JSONObject body = new JSONObject();
        try {
            body.put("id_tarea",   t.getId_tarea());
            body.put("id_usuario", idUsuario);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            Toast.makeText(requireContext(), getString(R.string.error_preparar_peticion), Toast.LENGTH_SHORT).show();
            return;
        }
        String url = WebService.URL_Asignacion_Tarea;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.tarea_asignada_msg),
                                    Toast.LENGTH_SHORT).show();
                            // Recargar toda la pantalla para reflejar el cambio
                            resetContainers();
                            cargarDatos();
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString("message", "No se pudo asignar la tarea"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_procesar_respuesta), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loadingDialog.dismiss();
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al asignar tarea", Request.Method.POST, url, error);
                }
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════ */

    /**
     * Determina si una tarea debe aparecer como PENDIENTE
     * según su frecuencia y su última realización.
     */
    private boolean esPendiente(Tarea t, Map<Integer, TareasRealizadas> ultimaRealizacion) {
        TareasRealizadas ultima = ultimaRealizacion.get(t.getId_tarea());
        if (ultima == null || ultima.getFecha_realizacion() == null) return true;

        // Tomamos solo los primeros 10 chars (yyyy-MM-dd) para comparar
        String fechaUlt = ultima.getFecha_realizacion().length() >= 10
                ? ultima.getFecha_realizacion().substring(0, 10) : ultima.getFecha_realizacion();
        String hoy = Utilidades.fechaHoyEntrada();

        if (t.getFrecuencia() == null) return true;
        switch (t.getFrecuencia()) {
            case "dia":
                return !fechaUlt.equals(hoy);
            case "semana":
                return !esMismaSemana(fechaUlt, hoy);
            case "mes":
                return !fechaUlt.substring(0, 7).equals(hoy.substring(0, 7));
            case "variable":
                return true; // Recurrencia libre: siempre pendiente
            default:
                return true;
        }
    }

    /** Compara si dos fechas ISO (yyyy-MM-dd) pertenecen a la misma semana ISO. */
    private boolean esMismaSemana(String isoDate1, String isoDate2) {
        try {
            SimpleDateFormat sdf = Utilidades.FMT_ENTRADA;
            java.util.Calendar c1 = java.util.Calendar.getInstance();
            java.util.Calendar c2 = java.util.Calendar.getInstance();
            c1.setTime(sdf.parse(isoDate1));
            c2.setTime(sdf.parse(isoDate2));
            c1.setMinimalDaysInFirstWeek(4);
            c2.setMinimalDaysInFirstWeek(4);
            return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                    && c1.get(java.util.Calendar.WEEK_OF_YEAR) == c2.get(java.util.Calendar.WEEK_OF_YEAR);
        } catch (Exception e) { return false; }
    }

    /** Capitaliza la primera letra de un string. */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String labelFrecuencia(String freq) {
        if (freq == null) return "-";
        switch (freq) {
            case "dia":      return "Diaria";
            case "semana":   return "Semanal";
            case "mes":      return "Mensual";
            case "variable": return "Variable";
            default:         return freq;
        }
    }

    private String formatFecha(String fechaStr) {
        if (fechaStr == null) return "";
        return Utilidades.formatearFechaMuro(fechaStr);
    }

    private void agregarDivider(LinearLayout parent) {
        View div = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(50);
        div.setLayoutParams(lp);
        div.setBackgroundColor(0xFFE8DFC0);
        parent.addView(div);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, this.getResources().getDisplayMetrics()));
    }
}
