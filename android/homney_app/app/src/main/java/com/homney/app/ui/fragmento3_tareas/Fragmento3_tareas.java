package com.homney.app.ui.fragmento3_tareas;

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
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.AsignacionTarea;
import com.homney.app.webservice.modelo.Habitacion;
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

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        if (idHogar == -1 || idUsuario == -1) {
            Toast.makeText(requireContext(),
                    "Sesión no válida — vuelve a iniciar sesión", Toast.LENGTH_LONG).show();
            return root;
        }
        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            return root;
        }

        cargarDatos();
        return root;
    }

    /* ════════════════════════════════════════════
       CARGA DE DATOS
    ════════════════════════════════════════════ */

    private void cargarDatos() {
        final AtomicInteger fase1 = new AtomicInteger(5);

        lanzarPeticion(WebService.URL_Asignacion_Tarea + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> {
                    asigMias = resp;
                    if (fase1.decrementAndGet() == 0) intentarRender();
                });

        lanzarPeticion(WebService.URL_Asignacion_Tarea,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> resp) -> {
                    allAsig = resp;
                    if (fase1.decrementAndGet() == 0) intentarRender();
                });

        lanzarPeticion(WebService.URL_Usuario + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Usuario>>() {}.getType(),
                (List<Usuario> resp) -> {
                    usuarios = resp;
                    if (fase1.decrementAndGet() == 0) intentarRender();
                });

        lanzarPeticion(WebService.URL_Tarea_Realizada + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<TareasRealizadas>>() {}.getType(),
                (List<TareasRealizadas> resp) -> {
                    realizadas = resp;
                    if (fase1.decrementAndGet() == 0) intentarRender();
                });

        lanzarPeticion(WebService.URL_Habitacion + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Habitacion>>() {}.getType(),
                (List<Habitacion> habs) -> {
                    boolean fase1Lista = fase1.decrementAndGet() == 0;
                    if (habs == null || habs.isEmpty()) {
                        tareas = new ArrayList<>();
                        intentarRender();
                    } else {
                        cargarTareasDeCadaHabitacion(habs);
                    }
                });
    }

    private void cargarTareasDeCadaHabitacion(List<Habitacion> habs) {
        List<Tarea> buffer = new ArrayList<>();
        AtomicInteger pending = new AtomicInteger(habs.size());
        for (Habitacion h : habs) {
            lanzarPeticion(WebService.URL_Tarea + "?id_habitacion=" + h.getId_habitacion(),
                    new TypeToken<RespuestaLista<Tarea>>() {}.getType(),
                    (List<Tarea> tareasHab) -> {
                        if (tareasHab != null) synchronized (buffer) { buffer.addAll(tareasHab); }
                        if (pending.decrementAndGet() == 0) {
                            tareas = buffer;
                            intentarRender();
                        }
                    });
        }
    }

    private void intentarRender() {
        if (asigMias != null && allAsig != null
                && usuarios != null && realizadas != null && tareas != null) {
            renderTareas();
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
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error petición", Request.Method.GET, url, error);
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

        // id_tarea → lista de usuarios asignados
        Map<Integer, List<Integer>> asigByTarea = new HashMap<>();
        for (AsignacionTarea a : allAsig) {
            asigByTarea.computeIfAbsent(a.getId_tarea(), k -> new ArrayList<>())
                       .add(a.getId_usuario());
        }

        // Completadas hoy
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Set<Integer> completadasHoy = new HashSet<>();
        for (TareasRealizadas r : realizadas) {
            if (r.getFecha_realizacion() != null && r.getFecha_realizacion().startsWith(hoy))
                completadasHoy.add(r.getId_tarea());
        }

        // Realizadas últimos 7 días
        long hace7ms = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        List<TareasRealizadas> recientes = new ArrayList<>();
        for (TareasRealizadas r : realizadas) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .parse(r.getFecha_realizacion());
                if (d != null && d.getTime() >= hace7ms) recientes.add(r);
            } catch (Exception ignored) {}
        }

        // Separar: mis tareas / otras (asignadas a otros o sin asignar)
        List<Tarea> misTareas   = new ArrayList<>();
        List<Tarea> otrasTareas = new ArrayList<>();
        for (Tarea t : tareas) {
            if (idsMias.contains(t.getId_tarea())) misTareas.add(t);
            else otrasTareas.add(t);
        }

        LayoutInflater inf = LayoutInflater.from(requireContext());

        // ─── Sección 1: Mis tareas ───────────────────────────
        tvSinMisTareas.setVisibility(View.GONE);
        long pendCount = misTareas.stream()
                .filter(t -> !completadasHoy.contains(t.getId_tarea())).count();
        tagPendientes.setText(pendCount + (pendCount == 1 ? " pendiente" : " pendientes"));

        if (misTareas.isEmpty()) {
            tvSinMisTareas.setText("Sin tareas pendientes.");
            tvSinMisTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : misTareas) {
                // Contenedor para el efecto de deslizar
                FrameLayout rootFrame = new FrameLayout(requireContext());
                rootFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                // Botón de eliminar (rojo, debajo)
                LinearLayout deleteBtn = new LinearLayout(requireContext());
                deleteBtn.setBackgroundColor(0xFFE05C5C); // Rojo suave
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

                // Fila de la tarea (encima, fondo blanco sólido para tapar el botón rojo)
                View row = inf.inflate(R.layout.item_tarea, rootFrame, false);
                row.setBackgroundColor(0xFFFFFFFF); // opaco: el deleteBtn queda oculto hasta que se deslice
                rootFrame.addView(row);

                rellenarFilaTarea(row, deleteBtn, t, true, completadasHoy, asigByTarea, userMap, colorMap);
                containerMisTareas.addView(rootFrame);
                agregarDivider(containerMisTareas);
            }
        }

        // ─── Sección 2: Otras tareas del hogar ──────────────
        // Incluye tanto las no asignadas como las asignadas a otros usuarios
        tvSinOtrasTareas.setVisibility(View.GONE);
        if (otrasTareas.isEmpty()) {
            tvSinOtrasTareas.setText("No hay más tareas en el hogar.");
            tvSinOtrasTareas.setVisibility(View.VISIBLE);
        } else {
            for (Tarea t : otrasTareas) {
                View row = inf.inflate(R.layout.item_tarea, containerOtrasTareas, false);
                rellenarFilaTarea(row, null, t, false, completadasHoy, asigByTarea, userMap, colorMap);
                containerOtrasTareas.addView(row);
                agregarDivider(containerOtrasTareas);
            }
        }

        // ─── Sección 3: Completadas esta semana ─────────────
        tvSinCompletadas.setVisibility(View.GONE);
        if (recientes.isEmpty()) {
            tvSinCompletadas.setText("Sin tareas completadas en los últimos 7 días.");
            tvSinCompletadas.setVisibility(View.VISIBLE);
        } else {
            for (TareasRealizadas r : recientes) {
                Tarea t = tareaMap.get(r.getId_tarea());
                View row = inf.inflate(R.layout.item_tarea, containerCompletadas, false);
                ((TextView) row.findViewById(R.id.tv_nombre_tarea))
                        .setText(t != null ? t.getNombre() : "Tarea #" + r.getId_tarea());
                // Mostrar observaciones si las tiene, si no la fecha
                String obs = r.getObservaciones();
                ((TextView) row.findViewById(R.id.tv_frecuencia))
                        .setText((obs != null && !obs.isEmpty()) ? obs : formatFecha(r.getFecha_realizacion()));
                TextView tvCheck = row.findViewById(R.id.tv_check);
                tvCheck.setText("✓");
                tvCheck.setBackgroundResource(R.drawable.bg_tag_green);
                TextView tvTag = row.findViewById(R.id.tv_tag_estado);
                tvTag.setText("COMPLETADA");
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
                                   Set<Integer> completadasHoy,
                                   Map<Integer, List<Integer>> asigByTarea,
                                   Map<Integer, Usuario> userMap,
                                   Map<Integer, Integer> colorMap) {

        boolean yaHecha = completadasHoy.contains(t.getId_tarea());
        ((TextView) row.findViewById(R.id.tv_nombre_tarea)).setText(t.getNombre());

        StringBuilder fr = new StringBuilder(labelFrecuencia(t.getFrecuencia()));
        if (t.getNum_veces() != null && !"1".equals(t.getNum_veces()))
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
            badge.setText(u != null
                    ? String.valueOf(u.getNombre().charAt(0)).toUpperCase(Locale.getDefault())
                    : "?");
            llAsig.addView(badge);
        }

        // Para "otras tareas": añadir "Asignado a: Nombre1, Nombre2" debajo de los badges
        // (si no hay nadie asignado: "Sin asignar")
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
                tvAsig.setText("Sin asignar");
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
            tvTag.setText("HECHA HOY");
            tvTag.setBackgroundResource(R.drawable.bg_tag_green);
        } else {
            tvTag.setText(labelFrecuencia(t.getFrecuencia()).toUpperCase(Locale.getDefault()));
            tvTag.setBackgroundResource(R.drawable.bg_tag_muted);
        }

        // ── Click + swipe: solo "mis tareas" PENDIENTES ──
        if (esMia && !yaHecha) {
            // Foreground ripple → el background blanco sólido permanece intacto
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true);
            row.setForeground(requireContext().getDrawable(outValue.resourceId));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v ->
                    mostrarDialogMarcarRealizada(t, row, tvFrec, tvCheck, tvTag));

            // Variables para el swipe
            final float[] x1 = {0};
            final float[] initialTranslationX = {0};
            final int MAX_SWIPE = dp(-80); // Ancho del botón rojo

            row.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1[0] = event.getRawX();
                        initialTranslationX[0] = v.getTranslationX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - x1[0];
                        float newTranslationX = initialTranslationX[0] + deltaX;
                        if (newTranslationX <= 0 && newTranslationX >= MAX_SWIPE) {
                            v.setTranslationX(newTranslationX);
                        }
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

            // Evento para el botón rojo (borrar)
            deleteBtn.setOnClickListener(v -> borrarTarea(t, (View) row.getParent()));
        }
    }

    /* ════════════════════════════════════════════
       DIALOG: ¿Confirmas realizar la tarea?
       Campos: Tiempo (min) + Observaciones
    ════════════════════════════════════════════ */

    private void mostrarDialogMarcarRealizada(Tarea t, View row,
                                              TextView tvFrec, TextView tvCheck, TextView tvTag) {
        // Construir el contenido del dialog
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(4));

        // Nombre de la tarea (subtítulo del dialog)
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

        // Label + EditText: Tiempo (min)
        TextView tvLabelDur = new TextView(requireContext());
        tvLabelDur.setText("Tiempo empleado (min)");
        tvLabelDur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelDur.setTypeface(null, Typeface.BOLD);
        tvLabelDur.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLabel.bottomMargin = dp(4);
        tvLabelDur.setLayoutParams(lpLabel);
        layout.addView(tvLabelDur);

        EditText etDuracion = new EditText(requireContext());
        etDuracion.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDuracion.setHint("ej. 30");
        if (t.getDuracion() != null)
            etDuracion.setText(String.valueOf(t.getDuracion())); // valor por defecto = duración de la tarea
        LinearLayout.LayoutParams lpDur = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpDur.bottomMargin = dp(14);
        etDuracion.setLayoutParams(lpDur);
        layout.addView(etDuracion);

        // Label + EditText: Observaciones
        TextView tvLabelObs = new TextView(requireContext());
        tvLabelObs.setText("Observaciones (opcional)");
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
        etObservaciones.setHint("¿Cómo fue la tarea?");
        etObservaciones.setMinLines(2);
        etObservaciones.setMaxLines(4);
        layout.addView(etObservaciones);

        new AlertDialog.Builder(requireContext())
                .setTitle("¿Confirmas realizar la tarea?")
                .setView(layout)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String durStr = etDuracion.getText().toString().trim();
                    String obs    = etObservaciones.getText().toString().trim();
                    marcarTareaRealizada(t, durStr.isEmpty() ? null : durStr, obs,
                            row, tvFrec, tvCheck, tvTag);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /* ════════════════════════════════════════════
       DELETE: eliminar tarea — réplica exacta de deleteTarea() del web
       Paso 1: DELETE asignacion_tarea.php?id_tarea=X  (libera la FK)
       Paso 2: DELETE tarea.php?id_tarea=X
       Paso 3: quitar la fila visualmente
    ════════════════════════════════════════════ */
    private void borrarTarea(Tarea t, View rootFrame) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar tarea?")
                .setMessage("¿Seguro que quieres eliminar «" + t.getNombre()
                        + "» y todas sus asignaciones?")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        eliminarAsignacionesYTarea(t, rootFrame))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Encadena dos DELETE igual que deleteTarea() del web:
     *   1) asignacion_tarea.php?id_tarea=X  — elimina las FK
     *   2) tarea.php?id_tarea=X             — elimina la tarea
     */
    private void eliminarAsignacionesYTarea(Tarea t, View rootFrame) {
        String urlAsig  = WebService.URL_Asignacion_Tarea + "?id_tarea=" + t.getId_tarea();
        String urlTarea = WebService.URL_Tarea            + "?id_tarea=" + t.getId_tarea();

        // ─── Paso 2: eliminar la tarea (se llama desde el callback del paso 1) ───
        JsonObjectRequest deleteTarea = new JsonObjectRequest(
                Request.Method.DELETE, urlTarea, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    "Tarea eliminada", Toast.LENGTH_SHORT).show();
                            // Quitar el FrameLayout (rootFrame) del contenedor
                            ViewGroup parent = (ViewGroup) rootFrame.getParent();
                            if (parent != null) parent.removeView(rootFrame);
                        } else {
                            Toast.makeText(requireContext(),
                                    "No se pudo eliminar la tarea: "
                                            + response.optString("message", ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error al eliminar tarea", Request.Method.DELETE, urlTarea, error)
        );

        // ─── Paso 1: eliminar asignaciones (en callback lanza el paso 2) ─────────
        JsonObjectRequest deleteAsig = new JsonObjectRequest(
                Request.Method.DELETE, urlAsig, null,
                respAsig -> {
                    // Con o sin asignaciones, procedemos a borrar la tarea
                    PeticionesRed.anhadirPeticionACola(deleteTarea);
                },
                error -> {
                    // Si falla la petición de asignaciones, intentamos igualmente
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

        String fechaAhora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

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
            Toast.makeText(requireContext(), "Error al preparar el registro", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WebService.URL_Tarea_Realizada;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.POST, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            // Actualizar la fila en tiempo real
                            tvCheck.setText("✓");
                            tvCheck.setBackgroundResource(R.drawable.bg_tag_green);
                            tvCheck.setAlpha(1f);

                            tvTag.setText("HECHA HOY");
                            tvTag.setBackgroundResource(R.drawable.bg_tag_green);

                            // Mostrar observaciones en el label de frecuencia (si las hay)
                            if (obs != null && !obs.isEmpty()) {
                                tvFrec.setText(obs);
                                tvFrec.setTextColor(0xFF5C4A2A);
                            }

                            // Quitar el click (ya no es accionable)
                            row.setOnClickListener(null);
                            row.setClickable(false);

                            // Decrementar badge de pendientes
                            try {
                                String txt = tagPendientes.getText().toString();
                                int count = Integer.parseInt(txt.split(" ")[0]);
                                if (count > 0) count--;
                                tagPendientes.setText(count + (count == 1 ? " pendiente" : " pendientes"));
                            } catch (Exception ignored) {}

                            Toast.makeText(requireContext(),
                                    "¡Tarea completada!", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = response.optString("message", "Error al registrar");
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error al marcar realizada", Request.Method.POST, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════ */

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
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .parse(fechaStr);
            return d != null
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(d)
                    : fechaStr;
        } catch (Exception e) { return fechaStr; }
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
