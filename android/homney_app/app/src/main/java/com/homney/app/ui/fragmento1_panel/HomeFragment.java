package com.homney.app.ui.fragmento1_panel;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.APIlib;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Column;
import com.anychart.core.cartesian.series.Line;
import com.anychart.enums.HoverMode;
import com.anychart.enums.TooltipPositionMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.utils.LoadingDialog;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.AsignacionTarea;
import com.homney.app.webservice.modelo.Gasto;
import com.homney.app.webservice.modelo.Habitacion;
import com.homney.app.webservice.modelo.Muro;
import com.homney.app.webservice.modelo.Tarea;
import com.homney.app.webservice.modelo.TareasRealizadas;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {

    /* ── Vistas ──────────────────────────────────── */
    private TextView     tvStatGastoTotal;
    private TextView     tvStatMisTareas;
    private TextView     tvStatPublicaciones;
    private LinearLayout containerUltimosGastos;
    private TextView     tvSinGastos;
    private LinearLayout containerMisTareasDash;
    private TextView     tvSinMisTareasDash;
    private LinearLayout containerPublicaciones;
    private TextView     tvSinPublicaciones;
    private AnyChartView chartTareas;
    private AnyChartView chartGastos;

    private LoadingDialog loadingDialog;

    /* ── Sesión ──────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar   = -1;

    /* ── Datos API ───────────────────────────────── */
    private List<Gasto>           gastos     = null;
    private List<AsignacionTarea> misAsig    = null;
    private List<Muro>            pubs       = null;
    private List<Tarea>           tareas     = null;
    private List<Usuario>         usuarios   = null;
    // Realizadas por usuario (para gráfico de líneas): idUsuario → lista
    private Map<Integer, List<TareasRealizadas>> realizadasPorUsuario = null;

    private static final String TAG = "WS_DASHBOARD";

    // Colores por usuario (igual que el resto de fragmentos)
    private static final String[] USER_COLORS_HEX = {
            "#F5C518", "#7CC87A", "#E05C5C", "#E89A30",
            "#9B59B6", "#3498DB", "#E67E22", "#1ABC9C"
    };

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment1_home, container, false);

        tvStatGastoTotal      = root.findViewById(R.id.tv_stat_gasto_total);
        tvStatMisTareas       = root.findViewById(R.id.tv_stat_mis_tareas);
        tvStatPublicaciones   = root.findViewById(R.id.tv_stat_publicaciones);
        containerUltimosGastos = root.findViewById(R.id.container_ultimos_gastos);
        tvSinGastos           = root.findViewById(R.id.tv_sin_gastos);
        containerMisTareasDash = root.findViewById(R.id.container_mis_tareas_dash);
        tvSinMisTareasDash    = root.findViewById(R.id.tv_sin_mis_tareas_dash);
        containerPublicaciones = root.findViewById(R.id.container_publicaciones);
        tvSinPublicaciones    = root.findViewById(R.id.tv_sin_publicaciones);
        chartTareas           = root.findViewById(R.id.chart_tareas);
        chartGastos           = root.findViewById(R.id.chart_gastos);

        // Desactivar aceleración por hardware para evitar crash en RenderThread con AnyChartView (WebView)
        chartTareas.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        chartGastos.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        loadingDialog = new LoadingDialog(requireContext());

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar   = prefs.getInt("id_hogar",   -1);

        // Navegación "Ver todos →"
        root.findViewById(R.id.btn_ver_gastos).setOnClickListener(v ->
                navegarA(root, R.id.fragmento4));
        root.findViewById(R.id.btn_ver_tareas).setOnClickListener(v ->
                navegarA(root, R.id.fragmento3));
        root.findViewById(R.id.btn_ver_muro).setOnClickListener(v ->
                navegarA(root, R.id.fragmento5));

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

    private void navegarA(View v, int destinoId) {
        NavOptions opts = new NavOptions.Builder()
                .setPopUpTo(destinoId, true).build();
        Navigation.findNavController(v).navigate(destinoId, null, opts);
    }

    /* ════════════════════════════════════════════
       CARGA DE DATOS
       Replica exactamente la lógica de dashboard() del web:
       gastos, misAsig, pubs, allAsig, tareas, usuarios, realizadas
    ════════════════════════════════════════════ */

    private void cargarDatos() {
        loadingDialog.show();
        // 5 peticiones paralelas de fase-1
        final AtomicInteger fase1 = new AtomicInteger(5);

        // Gastos del hogar
        lanzarGet(WebService.URL_Gasto + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Gasto>>() {}.getType(),
                (List<Gasto> r) -> {
                    gastos = r;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        // Mis asignaciones de tareas
        lanzarGet(WebService.URL_Asignacion_Tarea + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<AsignacionTarea>>() {}.getType(),
                (List<AsignacionTarea> r) -> {
                    misAsig = r;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        // Publicaciones del muro
        lanzarGet(WebService.URL_Muro + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Muro>>() {}.getType(),
                (List<Muro> r) -> {
                    pubs = r;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        // Usuarios del hogar (necesarios antes de cargar realizadas por usuario)
        lanzarGet(WebService.URL_Usuario + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Usuario>>() {}.getType(),
                (List<Usuario> r) -> {
                    usuarios = r;
                    // Lanzar carga de realizadas para el gráfico de líneas
                    cargarRealizadasPorUsuario(r);
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        // Habitaciones → encadena carga de tareas
        lanzarGet(WebService.URL_Habitacion + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Habitacion>>() {}.getType(),
                (List<Habitacion> habs) -> {
                    boolean yasiFase1 = fase1.decrementAndGet() == 0;
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
            lanzarGet(WebService.URL_Tarea + "?id_habitacion=" + h.getId_habitacion(),
                    new TypeToken<RespuestaLista<Tarea>>() {}.getType(),
                    (List<Tarea> t) -> {
                        if (t != null) synchronized (buffer) { buffer.addAll(t); }
                        if (pending.decrementAndGet() == 0) {
                            tareas = buffer;
                            intentarRender();
                        }
                    });
        }
    }

    /**
     * Carga las tareas realizadas de TODOS los usuarios del hogar.
     * Cuando terminen todas, establece realizadasPorUsuario y llama a intentarRender.
     * Esto proporciona los datos para el gráfico de líneas.
     */
    private void cargarRealizadasPorUsuario(List<Usuario> lista) {
        if (lista == null || lista.isEmpty()) {
            realizadasPorUsuario = new HashMap<>();
            intentarRender();
            return;
        }
        Map<Integer, List<TareasRealizadas>> map = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(lista.size());
        for (Usuario u : lista) {
            int uid = u.getId_usuario();
            lanzarGet(WebService.URL_Tarea_Realizada + "?id_usuario=" + uid,
                    new TypeToken<RespuestaLista<TareasRealizadas>>() {}.getType(),
                    (List<TareasRealizadas> r) -> {
                        synchronized (map) { map.put(uid, r != null ? r : new ArrayList<>()); }
                        if (pending.decrementAndGet() == 0) {
                            realizadasPorUsuario = map;
                            intentarRender();
                        }
                    });
        }
    }

    /**
     * Se activa cuando los 5 items de fase-1 están listos.
     * En ese momento tareas puede no estar lista todavía,
     * intentarRender() se encarga de verificar todo.
     */
    private void iniciarFase2() {
        intentarRender();
    }

    private void intentarRender() {
        if (gastos != null && misAsig != null && pubs != null
                && usuarios != null && tareas != null && realizadasPorUsuario != null) {
            loadingDialog.dismiss();
            renderDashboard();
        }
    }

    /* ════════════════════════════════════════════
       RENDERIZADO PRINCIPAL
    ════════════════════════════════════════════ */

    private void renderDashboard() {
        if (!isAdded()) return;

        // ─── Stats ─────────────────────────────────────────
        double totalGasto = 0;
        for (Gasto g : gastos) totalGasto += g.getImporte();
        tvStatGastoTotal.setText(fmt(totalGasto));
        tvStatMisTareas.setText(String.valueOf(misAsig.size()));
        tvStatPublicaciones.setText(String.valueOf(pubs.size()));

        // Mapa id_tarea → Tarea (para mostrar nombre en preview)
        Map<Integer, Tarea> tareaMap = new HashMap<>();
        for (Tarea t : tareas) tareaMap.put(t.getId_tarea(), t);

        // Mapa id_usuario → Usuario
        Map<Integer, Usuario> userMap = new HashMap<>();
        for (Usuario u : usuarios) userMap.put(u.getId_usuario(), u);

        // ─── Últimos 4 gastos ───────────────────────────────
        tvSinGastos.setVisibility(View.GONE);
        if (gastos.isEmpty()) {
            tvSinGastos.setText("Sin gastos registrados");
            tvSinGastos.setVisibility(View.VISIBLE);
        } else {
            int max = Math.min(4, gastos.size());
            for (int i = 0; i < max; i++) {
                containerUltimosGastos.addView(buildFilaGasto(gastos.get(i)));
                if (i < max - 1) agregarDivider(containerUltimosGastos);
            }
        }

        // ─── Mis 4 tareas pendientes ────────────────────────
        tvSinMisTareasDash.setVisibility(View.GONE);
        if (misAsig.isEmpty()) {
            tvSinMisTareasDash.setText("Sin tareas pendientes");
            tvSinMisTareasDash.setVisibility(View.VISIBLE);
        } else {
            int max = Math.min(4, misAsig.size());
            for (int i = 0; i < max; i++) {
                Tarea t = tareaMap.get(misAsig.get(i).getId_tarea());
                containerMisTareasDash.addView(buildFilaTarea(t, misAsig.get(i)));
                if (i < max - 1) agregarDivider(containerMisTareasDash);
            }
        }

        // ─── Últimas 3 publicaciones ────────────────────────
        tvSinPublicaciones.setVisibility(View.GONE);
        if (pubs.isEmpty()) {
            tvSinPublicaciones.setText("El muro está vacío");
            tvSinPublicaciones.setVisibility(View.VISIBLE);
        } else {
            int max = Math.min(3, pubs.size());
            for (int i = 0; i < max; i++) {
                containerPublicaciones.addView(buildTarjetaPublicacion(pubs.get(i), userMap));
            }
        }

        // ─── Gráficos ───────────────────────────────────────
        renderChartTareas();
        renderChartGastos(userMap);
    }

    /* ════════════════════════════════════════════
       GRÁFICO 1 — LÍNEAS: Tareas realizadas por usuario
       Últimos 7 días, una línea por miembro del hogar.
    ════════════════════════════════════════════ */

    private void renderChartTareas() {
        APIlib.getInstance().setActiveAnyChartView(chartTareas);
        Cartesian lineChart = AnyChart.line();
        lineChart.animation(true);

        // Preparar etiquetas de los últimos 7 días
        String[] dayLabels = new String[7];
        String[] fullDates = new String[7]; // formato yyyy-MM-dd para comparar
        String[] dayNames  = {"D", "L", "M", "X", "J", "V", "S"};
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            int dayIdx = 6 - i;
            String label = i == 0 ? "Hoy"
                    : i == 1 ? "Ayer"
                    : dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];
            dayLabels[dayIdx] = label;
            fullDates[dayIdx] = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(cal.getTime());
        }

        for (int ci = 0; ci < usuarios.size(); ci++) {
            Usuario u = usuarios.get(ci);
            List<TareasRealizadas> realizadas =
                    realizadasPorUsuario.getOrDefault(u.getId_usuario(), new ArrayList<>());

            List<DataEntry> puntos = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                final String fecha = fullDates[d];
                long count = realizadas.stream()
                        .filter(r -> r.getFecha_realizacion() != null
                                && r.getFecha_realizacion().startsWith(fecha))
                        .count();
                puntos.add(new ValueDataEntry(dayLabels[d], count));
            }

            Line linea = (Line) lineChart.line(puntos);
            linea.name(u.getNombre());
            String color = USER_COLORS_HEX[ci % USER_COLORS_HEX.length];
            linea.stroke("2 " + color);
            linea.hovered().markers().enabled(true);
        }

        lineChart.tooltip()
                .positionMode(TooltipPositionMode.POINT)
                .title(false);
        lineChart.xAxis(0).title(false);
        lineChart.yAxis(0).title(false);
        lineChart.yScale().minimum(0);
        lineChart.background().fill("#FFFFFFFF");

        chartTareas.setChart(lineChart);
    }

    /* ════════════════════════════════════════════
       GRÁFICO 2 — COLUMNAS: Total pagado por usuario
       Una columna por miembro del hogar.
    ════════════════════════════════════════════ */

    private void renderChartGastos(Map<Integer, Usuario> userMap) {
        APIlib.getInstance().setActiveAnyChartView(chartGastos);
        Cartesian colChart = AnyChart.column();
        colChart.animation(true);

        // Calcular total pagado por cada usuario (id_usuario_pagador)
        Map<Integer, Double> pagadoPorUsuario = new HashMap<>();
        for (Gasto g : gastos) {
            int uid = g.getId_usuario_pagador();
            pagadoPorUsuario.merge(uid, g.getImporte(), Double::sum);
        }

        List<DataEntry> data = new ArrayList<>();
        for (int ci = 0; ci < usuarios.size(); ci++) {
            Usuario u = usuarios.get(ci);
            double total = pagadoPorUsuario.getOrDefault(u.getId_usuario(), 0.0);
            data.add(new ValueDataEntry(u.getNombre(), total));
        }

        Column column = (Column) colChart.column(data);
        column.tooltip().format("{%Value}{groupsSeparator: ,} €");

        // Colorear cada barra con el color del usuario
        String colorJs = buildColumnColorJs(usuarios);
        column.fill(colorJs);
        column.stroke("none");

        colChart.tooltip().positionMode(TooltipPositionMode.POINT);
        colChart.xAxis(0).title(false);
        colChart.yAxis(0).title(false);
        colChart.yScale().minimum(0);
        colChart.background().fill("#FFFFFFFF");

        chartGastos.setChart(colChart);
    }

    /** Genera una función JS para colorear cada columna con el color del usuario. */
    private String buildColumnColorJs(List<Usuario> lista) {
        if (lista.isEmpty()) return "'#F5C518'";
        StringBuilder sb = new StringBuilder("function() { var colors = [");
        for (int i = 0; i < lista.size(); i++) {
            sb.append("'").append(USER_COLORS_HEX[i % USER_COLORS_HEX.length]).append("'");
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]; return colors[this.index % colors.length]; }");
        return sb.toString();
    }

    /* ════════════════════════════════════════════
       BUILDERS DE VISTAS
    ════════════════════════════════════════════ */

    /** Fila compacta de gasto: icono · concepto / fecha · importe */
    private View buildFilaGasto(Gasto g) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        ImageView img = new ImageView(requireContext());
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        imgLp.rightMargin = dp(10);
        img.setLayoutParams(imgLp);
        img.setImageResource(R.drawable.ic_credit_card);
        img.setBackgroundColor(0xFFFFF3C0);
        img.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.addView(img);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvConcepto = new TextView(requireContext());
        tvConcepto.setText(g.getConcepto());
        tvConcepto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvConcepto.setTypeface(null, Typeface.BOLD);
        tvConcepto.setTextColor(0xFF2D2416);
        tvConcepto.setMaxLines(1);
        tvConcepto.setEllipsize(android.text.TextUtils.TruncateAt.END);
        body.addView(tvConcepto);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText((g.getCategoria() != null ? g.getCategoria() : "—")
                + " · " + formatFecha(g.getFecha()));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(0xFF9A8A6A);
        body.addView(tvSub);
        row.addView(body);

        TextView tvImporte = new TextView(requireContext());
        tvImporte.setText("−" + fmt(g.getImporte()));
        tvImporte.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvImporte.setTypeface(null, Typeface.BOLD);
        tvImporte.setTextColor(0xFFE05C5C);
        row.addView(tvImporte);

        return row;
    }

    /**
     * Fila compacta de tarea del dashboard.
     * Si la tarea es real (t != null), al pulsar abre el dialog de "Marcar realizada".
     */
    private View buildFilaTarea(Tarea t, AsignacionTarea asig) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        // Check circular muted
        TextView tvCheck = new TextView(requireContext());
        LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        chkLp.rightMargin = dp(12);
        tvCheck.setLayoutParams(chkLp);
        tvCheck.setGravity(Gravity.CENTER);
        tvCheck.setBackgroundResource(R.drawable.bg_tag_muted);
        tvCheck.setAlpha(0.5f);
        row.addView(tvCheck);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvNombre = new TextView(requireContext());
        tvNombre.setText(t != null ? t.getNombre() : "Tarea #" + asig.getId_tarea());
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setTextColor(0xFF2D2416);
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        body.addView(tvNombre);

        TextView tvFrec = new TextView(requireContext());
        tvFrec.setText(t != null ? labelFrecuencia(t.getFrecuencia()) : "");
        tvFrec.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFrec.setTextColor(0xFF9A8A6A);
        body.addView(tvFrec);
        row.addView(body);

        // Click → dialog marcar realizada (solo si tenemos la tarea completa)
        if (t != null) {
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true);
            row.setBackgroundResource(outValue.resourceId);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v ->
                    mostrarDialogMarcarRealizada(t, row, tvFrec, tvCheck));
        }

        return row;
    }

    /* ════════════════════════════════════════════
       DIALOG: ¿Confirmas realizar la tarea?
    ════════════════════════════════════════════ */

    private void mostrarDialogMarcarRealizada(Tarea t, View row,
                                              TextView tvFrec, TextView tvCheck) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), dp(4));

        TextView tvTareaNombre = new TextView(requireContext());
        tvTareaNombre.setText("\"" + t.getNombre() + "\"");
        tvTareaNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvTareaNombre.setTextColor(0xFF5C4A2A);
        tvTareaNombre.setTypeface(null, Typeface.ITALIC);
        LinearLayout.LayoutParams lpN = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpN.bottomMargin = dp(14);
        tvTareaNombre.setLayoutParams(lpN);
        layout.addView(tvTareaNombre);

        // Tiempo (min)
        TextView tvLabelDur = new TextView(requireContext());
        tvLabelDur.setText("Tiempo empleado (min)");
        tvLabelDur.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelDur.setTypeface(null, Typeface.BOLD);
        tvLabelDur.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLd.bottomMargin = dp(4);
        tvLabelDur.setLayoutParams(lpLd);
        layout.addView(tvLabelDur);

        EditText etDuracion = new EditText(requireContext());
        etDuracion.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDuracion.setHint("ej. 30");
        if (t.getDuracion() != null) etDuracion.setText(String.valueOf(t.getDuracion()));
        LinearLayout.LayoutParams lpEd = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpEd.bottomMargin = dp(14);
        etDuracion.setLayoutParams(lpEd);
        layout.addView(etDuracion);

        // Observaciones
        TextView tvLabelObs = new TextView(requireContext());
        tvLabelObs.setText("Observaciones (opcional)");
        tvLabelObs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvLabelObs.setTypeface(null, Typeface.BOLD);
        tvLabelObs.setTextColor(0xFF9A8A6A);
        LinearLayout.LayoutParams lpLo = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLo.bottomMargin = dp(4);
        tvLabelObs.setLayoutParams(lpLo);
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
                    marcarTareaRealizada(t, durStr.isEmpty() ? null : durStr,
                            obs, row, tvFrec, tvCheck);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void marcarTareaRealizada(Tarea t, String durReal, String obs,
                                      View row, TextView tvFrec, TextView tvCheck) {

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

                            // Actualizar fila en tiempo real
                            tvCheck.setText("✓");
                            tvCheck.setBackgroundResource(R.drawable.bg_tag_green);
                            tvCheck.setAlpha(1f);
                            if (obs != null && !obs.isEmpty()) {
                                tvFrec.setText(obs);
                                tvFrec.setTextColor(0xFF5C4A2A);
                            }
                            row.setOnClickListener(null);
                            row.setClickable(false);

                            Toast.makeText(requireContext(),
                                    "¡Tarea completada!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    response.optString("message", "Error al registrar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(),
                        "WS_HOME", "Error al marcar realizada",
                        Request.Method.POST, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /** Tarjeta de publicación del muro con avatar inicial + nombre/fecha + título + cuerpo */
    private View buildTarjetaPublicacion(Muro pub, Map<Integer, Usuario> userMap) {
        androidx.cardview.widget.CardView cv =
                new androidx.cardview.widget.CardView(requireContext());
        LinearLayout.LayoutParams cvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cvLp.bottomMargin = dp(10);
        cv.setLayoutParams(cvLp);
        cv.setRadius(dp(14));
        cv.setCardElevation(dp(2));
        cv.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(14), dp(12), dp(14), dp(12));

        // Cabecera: avatar + nombre + fecha
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvIni = new TextView(requireContext());
        int sz = dp(34);
        LinearLayout.LayoutParams iniLp = new LinearLayout.LayoutParams(sz, sz);
        iniLp.rightMargin = dp(10);
        tvIni.setLayoutParams(iniLp);
        tvIni.setGravity(Gravity.CENTER);
        tvIni.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvIni.setTypeface(null, Typeface.BOLD);
        tvIni.setTextColor(0xFFFFFFFF);
        tvIni.setBackgroundColor(0xFFF5C518);
        Usuario autor = userMap.get(pub.getId_usuario());
        String nombre = autor != null ? autor.getNombre() : "?";
        tvIni.setText(String.valueOf(nombre.charAt(0)).toUpperCase(Locale.getDefault()));
        header.addView(tvIni);

        LinearLayout metaCol = new LinearLayout(requireContext());
        metaCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        metaCol.setLayoutParams(metaLp);

        TextView tvNombre = new TextView(requireContext());
        tvNombre.setText(nombre);
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setTextColor(0xFF2D2416);
        metaCol.addView(tvNombre);

        TextView tvFecha = new TextView(requireContext());
        tvFecha.setText(formatFecha(pub.getFecha_pub() != null
                ? pub.getFecha_pub().substring(0, 10) : ""));
        tvFecha.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvFecha.setTextColor(0xFF9A8A6A);
        metaCol.addView(tvFecha);
        header.addView(metaCol);
        inner.addView(header);

        // Título
        TextView tvTitulo = new TextView(requireContext());
        tvTitulo.setText(pub.getTitulo());
        tvTitulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitulo.setTypeface(null, Typeface.BOLD);
        tvTitulo.setTextColor(0xFF2D2416);
        LinearLayout.LayoutParams titLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titLp.topMargin = dp(8);
        tvTitulo.setLayoutParams(titLp);
        inner.addView(tvTitulo);

        // Cuerpo (truncado a 3 líneas)
        TextView tvCuerpo = new TextView(requireContext());
        tvCuerpo.setText(pub.getCuerpo());
        tvCuerpo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvCuerpo.setTextColor(0xFF5C4A2A);
        tvCuerpo.setMaxLines(3);
        tvCuerpo.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams cuerpoLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cuerpoLp.topMargin = dp(4);
        tvCuerpo.setLayoutParams(cuerpoLp);
        inner.addView(tvCuerpo);

        cv.addView(inner);
        return cv;
    }

    /* ════════════════════════════════════════════
       HELPER: petición GET genérica
    ════════════════════════════════════════════ */

    private <T> void lanzarGet(String url, Type tipo,
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
       HELPERS VARIOS
    ════════════════════════════════════════════ */

    private String labelFrecuencia(String freq) {
        if (freq == null) return "—";
        switch (freq) {
            case "dia":      return "Diaria";
            case "semana":   return "Semanal";
            case "mes":      return "Mensual";
            case "variable": return "Variable";
            default:         return freq;
        }
    }

    private String fmt(double importe) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("es", "ES"));
        return new DecimalFormat("#,##0.00", sym).format(importe) + " €";
    }

    private String formatFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isEmpty()) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaStr);
            return d != null
                    ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
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
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
