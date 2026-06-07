package com.homney.app.ui.fragmento2_mihogar;

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

public class FragmentoTareasHabitacion extends Fragment {

    public static final String ARG_ID_HABITACION     = "id_habitacion";
    public static final String ARG_NOMBRE_HABITACION = "nombre_habitacion";

    /* ── Vistas ──────────────────────────────────── */
    private TextView     tvTituloHabitacion;
    private TextView     tagNumTareas;

    private LinearLayout llHeaderMisTareas;
    private View         cardMisTareas;
    private TextView     chevronMisTareas;
    private LinearLayout containerMisTareas;
    private TextView     tvSinMisTareas;

    private LinearLayout llHeaderOtrasTareas;
    private View         cardOtrasTareas;
    private TextView     chevronOtrasTareas;
    private LinearLayout containerOtrasTareas;
    private TextView     tvSinOtrasTareas;

    private LinearLayout llHeaderCompletadas;
    private View         cardCompletadas;
    private TextView     chevronCompletadas;
    private LinearLayout containerCompletadas;
    private TextView     tvSinCompletadas;

    /* ── Estado de secciones colapsables ─────────── */
    private boolean sec1Expanded = true;
    private boolean sec2Expanded = true;
    private boolean sec3Expanded = true;

    /* ── Sesión ──────────────────────────────────── */
    private int idUsuario    = -1;
    private int idHogar      = -1;
    private int idHabitacion = -1;
    private String nombreHabitacion = "";

    /* ── Datos API ───────────────────────────────── */
    private List<AsignacionTarea>  asigMias   = null;
    private List<AsignacionTarea>  allAsig    = null;
    private List<Usuario>          usuarios   = null;
    private List<TareasRealizadas> realizadas = null;
    private List<Tarea>            tareas     = null;

    private LoadingDialog loadingDialog;
    private static final String TAG = "WS_TAREAS_HAB";

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

        View root = inflater.inflate(R.layout.fragment_tareas_habitacion, container, false);

        // Cabecera
        tvTituloHabitacion = root.findViewById(R.id.tv_titulo_habitacion);
        tagNumTareas       = root.findViewById(R.id.tag_num_tareas);

        // Sección 1 — Mis tareas
        llHeaderMisTareas  = root.findViewById(R.id.ll_header_mis_tareas_hab);
        cardMisTareas      = root.findViewById(R.id.card_mis_tareas_hab);
        chevronMisTareas   = root.findViewById(R.id.chevron_mis_tareas_hab);
        containerMisTareas = root.findViewById(R.id.container_mis_tareas_hab);
        tvSinMisTareas     = root.findViewById(R.id.tv_sin_mis_tareas_hab);

        // Sección 2 — Otras tareas
        llHeaderOtrasTareas  = root.findViewById(R.id.ll_header_otras_tareas_hab);
        cardOtrasTareas      = root.findViewById(R.id.card_otras_tareas_hab);
        chevronOtrasTareas   = root.findViewById(R.id.chevron_otras_tareas_hab);
        containerOtrasTareas = root.findViewById(R.id.container_otras_tareas_hab);
        tvSinOtrasTareas     = root.findViewById(R.id.tv_sin_otras_tareas_hab);

        // Sección 3 — Completadas
        llHeaderCompletadas  = root.findViewById(R.id.ll_header_completadas_hab);
        cardCompletadas      = root.findViewById(R.id.card_completadas_hab);
        chevronCompletadas   = root.findViewById(R.id.chevron_completadas_hab);
        containerCompletadas = root.findViewById(R.id.container_completadas_hab);
        tvSinCompletadas     = root.findViewById(R.id.tv_sin_completadas_hab);

        setupColapsables();

        if (getArguments() != null) {
            idHabitacion     = getArguments().getInt(ARG_ID_HABITACION, -1);
            nombreHabitacion = getArguments().getString(ARG_NOMBRE_HABITACION, "Habitación");
        }

        tvTituloHabitacion.setText(nombreHabitacion);

        loadingDialog = new LoadingDialog(requireContext());

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        if (idHabitacion == -1) {
            tvSinMisTareas.setText(getString(R.string.habitacion_no_valida));
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (idHabitacion != -1 && idHogar != -1 && idUsuario != -1
                && Utilidades.hayConexionInternet(requireContext())) {
            resetContainers();
            cargarDatos();
        } else if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), getString(R.string.sin_conexion_internet), Toast.LENGTH_SHORT).show();
            tvSinMisTareas.setText(getString(R.string.sin_conexion_internet));
        }
    }

    private void setupColapsables() {
        if (llHeaderMisTareas != null && cardMisTareas != null) {
            llHeaderMisTareas.setOnClickListener(v -> {
                sec1Expanded = !sec1Expanded;
                cardMisTareas.setVisibility(sec1Expanded ? View.VISIBLE : View.GONE);
                if (chevronMisTareas != null)
                    chevronMisTareas.setText(sec1Expanded ? "▲" : "▼");
            });
        }
        if (llHeaderOtrasTareas != null && cardOtrasTareas != null) {
            llHeaderOtrasTareas.setOnClickListener(v -> {
                sec2Expanded = !sec2Expanded;
                cardOtrasTareas.setVisibility(sec2Expanded ? View.VISIBLE : View.GONE);
                if (chevronOtrasTareas != null)
                    chevronOtrasTareas.setText(sec2Expanded ? "▲" : "▼");
            });
        }
        if (llHeaderCompletadas != null && cardCompletadas != null) {
            llHeaderCompletadas.setOnClickListener(v -> {
                sec3Expanded = !sec3Expanded;
                cardCompletadas.setVisibility(sec3Expanded ? View.VISIBLE : View.GONE);
                if (chevronCompletadas != null)
                    chevronCompletadas.setText(sec3Expanded ? "▲" : "▼");
            });
        }
    }

    private void resetContainers() {
        asigMias = null; allAsig = null; usuarios = null; realizadas = null; tareas = null;
        if (containerMisTareas   != null) containerMisTareas.removeAllViews();
        if (containerOtrasTareas != null) containerOtrasTareas.removeAllViews();
        if (containerCompletadas != null) containerCompletadas.removeAllViews();
        tvSinMisTareas.setText(getString(R.string.cargando));
        tvSinMisTareas.setVisibility(View.VISIBLE);
        tvSinOtrasTareas.setVisibility(View.GONE);
        tvSinCompletadas.setVisibility(View.GONE);
    }

    /* ════════════════════════════════════════════
       CARGA DE DATOS  (5 peticiones paralelas)
    ════════════════════════════════════════════ */

    private void cargarDatos() {
        loadingDialog.show();
        final AtomicInteger pendientes = new AtomicInteger(5);

        // 1. Mis asignaciones
        lanzarPeticion(
                WebService.URL_Asignacion_Tarea + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> { asigMias = resp; checkYRender(pendientes); });

        // 2. Todas las asignaciones del hogar (badges)
        lanzarPeticion(
                WebService.URL_Asignacion_Tarea + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> { allAsig = resp; checkYRender(pendientes); });

        // 3. Usuarios del hogar
        lanzarPeticion(
                WebService.URL_Usuario + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Usuario>>() {}.getType(),
                (List<Usuario> resp) -> { usuarios = resp; checkYRender(pendientes); });

        // 4. Tareas realizadas por el usuario
        lanzarPeticion(
                WebService.URL_Tarea_Realizada + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<TareasRealizadas>>() {}.getType(),
                (List<TareasRealizadas> resp) -> { realizadas = resp; checkYRender(pendientes); });

        // 5. Tareas de esta habitación
        lanzarPeticion(
                WebService.URL_Tarea + "?id_habitacion=" + idHabitacion,
                new TypeToken<RespuestaLista<Tarea>>() {}.getType(),
                (List<Tarea> resp) -> { tareas = resp; checkYRender(pendientes); });
    }

    private void checkYRender(AtomicInteger pendientes) {
        if (pendientes.decrementAndGet() == 0
                && asigMias != null && allAsig != null
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

        // Mapas auxiliares
        Map<Integer, Usuario> userMap  = new HashMap<>();
        Map<Integer, Integer> colorMap = new HashMap<>();
        int ci = 0;
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

        // Última realización por tarea
        Map<Integer, TareasRealizadas> ultimaReal = new HashMap<>();
        for (TareasRealizadas r : realizadas) {
            if (!ultimaReal.containsKey(r.getId_tarea()))
                ultimaReal.put(r.getId_tarea(), r);
        }

        // Completadas este mes (para sección 3)
        String mesActual = new SimpleDateFormat("yyyy-MM", Locale.ROOT).format(new Date());
        List<TareasRealizadas> completadasEsteMes = new ArrayList<>();
        Map<Integer, Tarea>    tareaMap           = new HashMap<>();
        for (Tarea t : tareas) tareaMap.put(t.getId_tarea(), t);
        for (TareasRealizadas r : realizadas) {
            if (r.getFecha_realizacion() == null) continue;
            // Solo las completadas de tareas de ESTA habitación
            if (tareaMap.containsKey(r.getId_tarea())
                    && r.getFecha_realizacion().startsWith(mesActual)) {
                completadasEsteMes.add(r);
            }
        }

        // Separar mis tareas pendientes / otras
        List<Tarea> misTareas   = new ArrayList<>();
        List<Tarea> otrasTareas = new ArrayList<>();
        for (Tarea t : tareas) {
            if (idsMias.contains(t.getId_tarea())) {
                if (esPendiente(t, ultimaReal)) misTareas.add(t);
            } else {
                otrasTareas.add(t);
            }
        }

        LayoutInflater inf = LayoutInflater.from(requireContext());

        // Badge de pendientes en la cabecera
        long pendCount = misTareas.size();
        tagNumTareas.setText(pendCount + (pendCount == 1 ? " pendiente" : " pendientes"));

        // ─── Sección 1: Mis tareas ───────────────────────────
        tvSinMisTareas.setVisibility(View.GONE);
        if (misTareas.isEmpty()) {
            tvSinMisTareas.setText(getString(R.string.sin_tareas_pendientes_yo));
            tvSinMisTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : misTareas) {
                FrameLayout rootFrame = new FrameLayout(requireContext());
                rootFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                // Botón eliminar (debajo, a la derecha)
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

                rellenarFilaTarea(row, deleteBtn, t, true, asigByTarea, userMap, colorMap);
                containerMisTareas.addView(rootFrame);
                agregarDivider(containerMisTareas);
            }
        }

        // ─── Sección 2: Otras tareas ─────────────────────────
        tvSinOtrasTareas.setVisibility(View.GONE);
        if (otrasTareas.isEmpty()) {
            tvSinOtrasTareas.setText(getString(R.string.no_hay_m_s_tareas_en_esta_habitaci_n));
            tvSinOtrasTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : otrasTareas) {
                View row = inf.inflate(R.layout.item_tarea, containerOtrasTareas, false);
                rellenarFilaTarea(row, null, t, false, asigByTarea, userMap, colorMap);
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
                                ? obs : Utilidades.formatearFechaMuro(r.getFecha_realizacion()));
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
    }

    /* ════════════════════════════════════════════
       RELLENA UNA FILA DE TAREA
    ════════════════════════════════════════════ */

    private void rellenarFilaTarea(View row, View deleteBtn, Tarea t, boolean esMia,
                                   Map<Integer, List<Integer>> asigByTarea,
                                   Map<Integer, Usuario> userMap,
                                   Map<Integer, Integer> colorMap) {

        ((TextView) row.findViewById(R.id.tv_nombre_tarea))
                .setText(t.getNombre() != null ? t.getNombre() : "");

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
        tvCheck.setText("");
        tvCheck.setBackgroundResource(R.drawable.bg_tag_muted);
        if (!esMia) tvCheck.setAlpha(0.4f);

        // Badges de asignados
        LinearLayout llAsig = row.findViewById(R.id.ll_asignados);
        List<Integer> asigs = asigByTarea.containsKey(t.getId_tarea())
                ? asigByTarea.get(t.getId_tarea()) : new ArrayList<>();
        for (Integer uid : asigs) {
            Usuario u = userMap.get(uid);
            String nombre = (u != null && u.getNombre() != null && !u.getNombre().isEmpty())
                    ? u.getNombre() : "?";
            char inicial = nombre.charAt(0);

            TextView badge = new TextView(requireContext());
            int sz = dp(24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
            lp.rightMargin = dp(3);
            badge.setLayoutParams(lp);
            badge.setGravity(Gravity.CENTER);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(Color.WHITE);
            badge.setBackgroundColor(colorMap.containsKey(uid) ? colorMap.get(uid) : USER_COLORS[0]);
            badge.setText(String.valueOf(inicial).toUpperCase(Locale.getDefault()));
            llAsig.addView(badge);
        }

        // Texto "Asignado a: …" en otras tareas
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
        tvTag.setText(labelFrecuencia(t.getFrecuencia()).toUpperCase(Locale.getDefault()));
        tvTag.setBackgroundResource(R.drawable.bg_tag_muted);

        // ── Mis tareas: click = completar, swipe = revelar borrar ──
        if (esMia) {
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true);
            row.setForeground(requireContext().getDrawable(outValue.resourceId));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v ->
                    mostrarDialogMarcarRealizada(t, row, tvCheck, tvTag));

            final float[] x1 = {0};
            final float[] initialTx = {0};
            final int MAX_SWIPE = dp(-80);

            row.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1[0]       = event.getRawX();
                        initialTx[0] = v.getTranslationX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float delta = event.getRawX() - x1[0];
                        float newTx  = initialTx[0] + delta;
                        if (newTx <= 0 && newTx >= MAX_SWIPE) v.setTranslationX(newTx);
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

        // ── Otras tareas: click = asignarse ──────────────────
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
       DIALOG: Confirmar tarea realizada
    ════════════════════════════════════════════ */

    private void mostrarDialogMarcarRealizada(Tarea t, View row,
                                              TextView tvCheck, TextView tvTag) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView tvNombre = new TextView(requireContext());
        tvNombre.setText("\"" + t.getNombre() + "\"");
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNombre.setTextColor(0xFF5C4A2A);
        tvNombre.setTypeface(null, Typeface.ITALIC);
        LinearLayout.LayoutParams lpN = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpN.bottomMargin = dp(14);
        tvNombre.setLayoutParams(lpN);
        layout.addView(tvNombre);

        // Tiempo empleado
        TextView tvLabelDur = new TextView(requireContext());
        tvLabelDur.setText(getString(R.string.tiempo_empleado));
        tvLabelDur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelDur.setTypeface(null, Typeface.BOLD);
        tvLabelDur.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLD = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLD.bottomMargin = dp(8);
        tvLabelDur.setLayoutParams(lpLD);
        layout.addView(tvLabelDur);

        final int stepMin    = 5;
        final int maxHoras   = 12;
        final int cantidadMin = 60 / stepMin;

        String[] horasValores = new String[maxHoras + 1];
        for (int i = 0; i <= maxHoras; i++) horasValores[i] = i + " h";
        String[] minutosValores = new String[cantidadMin];
        for (int i = 0; i < cantidadMin; i++)
            minutosValores[i] = String.format(Locale.getDefault(), "%02d m", i * stepMin);

        int preHoras = 0, preMinIdx = 0;
        if (t.getDuracion() != null && t.getDuracion() > 0) {
            preHoras  = Math.min(t.getDuracion() / 60, maxHoras);
            int resto = t.getDuracion() % 60;
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

        LinearLayout fila = new LinearLayout(requireContext());
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpF = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpF.bottomMargin = dp(14);
        fila.setLayoutParams(lpF);
        fila.addView(npHoras);
        fila.addView(npMinutos);
        layout.addView(fila);

        // Observaciones
        TextView tvLabelObs = new TextView(requireContext());
        tvLabelObs.setText(getString(R.string.observaciones_opcional));
        tvLabelObs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelObs.setTypeface(null, Typeface.BOLD);
        tvLabelObs.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLO = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLO.bottomMargin = dp(4);
        tvLabelObs.setLayoutParams(lpLO);
        layout.addView(tvLabelObs);

        EditText etObs = new EditText(requireContext());
        etObs.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etObs.setHint(getString(R.string.hint_como_fue_tarea));
        etObs.setMinLines(2);
        etObs.setMaxLines(4);
        layout.addView(etObs);

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_confirmar_tarea_titulo))
                .setView(layout)
                .setPositiveButton(getString(R.string.confirmar), (dialog, which) -> {
                    int totalMin = npHoras.getValue() * 60 + npMinutos.getValue() * stepMin;
                    String durStr = totalMin > 0 ? String.valueOf(totalMin) : null;
                    String obs    = etObs.getText().toString().trim();
                    marcarTareaRealizada(t, durStr, obs, row, tvCheck, tvTag);
                })
                .setNegativeButton(getString(R.string.btn_cancelar), null)
                .show();
    }

    /* ════════════════════════════════════════════
       POST: registrar tarea como realizada
    ════════════════════════════════════════════ */

    private void marcarTareaRealizada(Tarea t, String durReal, String obs,
                                      View row, TextView tvCheck, TextView tvTag) {
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
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    loadingDialog.dismiss();
                    if (!isAdded()) return;
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // Actualizar badge de pendientes
                            try {
                                String txt = tagNumTareas.getText().toString();
                                int count = Integer.parseInt(txt.split(" ")[0]);
                                if (count > 0) count--;
                                tagNumTareas.setText(count + (count == 1 ? " pendiente" : " pendientes"));
                            } catch (Exception ignored) {}

                            // Animar y quitar la fila de "Mis tareas"
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
                                                containerMisTareas.removeViewAt(idx);
                                                if (idx < containerMisTareas.getChildCount())
                                                    containerMisTareas.removeViewAt(idx);
                                            }
                                            if (containerMisTareas.getChildCount() == 0) {
                                                tvSinMisTareas.setText(getString(R.string.sin_tareas_pendientes_yo));
                                                tvSinMisTareas.setVisibility(View.VISIBLE);
                                            }
                                        }).start();
                            }

                            // Añadir fila a "Completadas este mes"
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
                            agregarDivider(containerCompletadas, 1);

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
        ));
    }

    /* ════════════════════════════════════════════
       DELETE: eliminar tarea (asignaciones → tarea)
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

        // Primero borrar asignaciones, luego la tarea
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.DELETE, urlAsig, null,
                respAsig -> PeticionesRed.anhadirPeticionACola(deleteTarea),
                error -> {
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al eliminar asignaciones", Request.Method.DELETE, urlAsig, error);
                    PeticionesRed.anhadirPeticionACola(deleteTarea);
                }
        ));
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
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
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
        ));
    }

    /* ════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════ */

    private boolean esPendiente(Tarea t, Map<Integer, TareasRealizadas> ultimaReal) {
        TareasRealizadas ultima = ultimaReal.get(t.getId_tarea());
        if (ultima == null || ultima.getFecha_realizacion() == null) return true;

        String fechaUlt = ultima.getFecha_realizacion().length() >= 10
                ? ultima.getFecha_realizacion().substring(0, 10)
                : ultima.getFecha_realizacion();
        String hoy = Utilidades.fechaHoyEntrada();

        if (t.getFrecuencia() == null) return true;
        switch (t.getFrecuencia()) {
            case "dia":      return !fechaUlt.equals(hoy);
            case "semana":   return !esMismaSemana(fechaUlt, hoy);
            case "mes":      return !fechaUlt.substring(0, 7).equals(hoy.substring(0, 7));
            case "variable": return true;
            default:         return true;
        }
    }

    private boolean esMismaSemana(String isoDate1, String isoDate2) {
        try {
            java.util.Calendar c1 = java.util.Calendar.getInstance();
            java.util.Calendar c2 = java.util.Calendar.getInstance();
            c1.setTime(Utilidades.FMT_ENTRADA.parse(isoDate1));
            c2.setTime(Utilidades.FMT_ENTRADA.parse(isoDate2));
            c1.setMinimalDaysInFirstWeek(4);
            c2.setMinimalDaysInFirstWeek(4);
            return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                    && c1.get(java.util.Calendar.WEEK_OF_YEAR) == c2.get(java.util.Calendar.WEEK_OF_YEAR);
        } catch (Exception e) { return false; }
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

    private void agregarDivider(LinearLayout parent) {
        View div = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(50);
        div.setLayoutParams(lp);
        div.setBackgroundColor(0xFFE8DFC0);
        parent.addView(div);
    }

    /** Inserta un divider en una posición concreta (útil al añadir filas al inicio). */
    private void agregarDivider(LinearLayout parent, int index) {
        View div = new View(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = dp(50);
        div.setLayoutParams(lp);
        div.setBackgroundColor(0xFFE8DFC0);
        parent.addView(div, index);
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
