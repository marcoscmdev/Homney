package com.homney.app.ui.fragmento2_mihogar;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.homney.app.webservice.modelo.Tarea;
import com.homney.app.webservice.respuestas.RespuestaLista;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentoTareasHabitacion extends Fragment {

    public static final String ARG_ID_HABITACION     = "id_habitacion";
    public static final String ARG_NOMBRE_HABITACION = "nombre_habitacion";

    private TextView     tvTituloHabitacion;
    private TextView     tagNumTareas;
    private LinearLayout containerTareasHab;
    private TextView     tvSinTareasHab;

    private int    idHabitacion     = -1;
    private String nombreHabitacion = "";

    private LoadingDialog loadingDialog;

    private static final String TAG = "WS_TAREAS_HAB";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tareas_habitacion, container, false);

        tvTituloHabitacion = root.findViewById(R.id.tv_titulo_habitacion);
        tagNumTareas       = root.findViewById(R.id.tag_num_tareas);
        containerTareasHab = root.findViewById(R.id.container_tareas_hab);
        tvSinTareasHab     = root.findViewById(R.id.tv_sin_tareas_hab);

        if (getArguments() != null) {
            idHabitacion     = getArguments().getInt(ARG_ID_HABITACION, -1);
            nombreHabitacion = getArguments().getString(ARG_NOMBRE_HABITACION, "Habitación");
        }

        tvTituloHabitacion.setText(nombreHabitacion);

        loadingDialog = new LoadingDialog(requireContext());

        if (idHabitacion == -1) {
            tvSinTareasHab.setText("Habitación no válida.");
            return root;
        }

        if (!Utilidades.hayConexionInternet(requireContext())) {
            Toast.makeText(requireContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            tvSinTareasHab.setText("Sin conexión a Internet.");
            return root;
        }

        cargarTareas();
        return root;
    }

    private void cargarTareas() {
        loadingDialog.show();
        String urlTareas = WebService.URL_Tarea + "?id_habitacion=" + idHabitacion;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, urlTareas, null,
                response -> {
                    List<Tarea> tareas = new ArrayList<>();
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Tarea>>() {}.getType();
                            RespuestaLista<Tarea> resp = gson.fromJson(response.toString(), tipo);
                            tareas = resp.data != null ? resp.data : new ArrayList<>();
                        }
                    } catch (Exception ignored) {}
                    renderTareas(tareas);
                },
                error -> {
                    if (isAdded())
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error cargando tareas", Request.Method.GET, urlTareas, error);
                    renderTareas(new ArrayList<>());
                }
        ));
    }

    private void renderTareas(List<Tarea> tareas) {
        if (!isAdded()) return;
        loadingDialog.dismiss();

        int n = tareas.size();
        tagNumTareas.setText(n + (n == 1 ? " TAREA" : " TAREAS"));
        tvSinTareasHab.setVisibility(View.GONE);

        if (tareas.isEmpty()) {
            tvSinTareasHab.setText("No hay tareas asignadas a esta habitación.");
            tvSinTareasHab.setVisibility(View.VISIBLE);
            return;
        }

        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (Tarea t : tareas) {
            View row = inf.inflate(R.layout.item_tarea, containerTareasHab, false);

            ((TextView) row.findViewById(R.id.tv_nombre_tarea))
                    .setText(t.getNombre() != null ? t.getNombre() : "");

            StringBuilder fr = new StringBuilder(labelFrecuencia(t.getFrecuencia()));
            if (t.getNum_veces() != null && !"1".equals(t.getNum_veces()))
                fr.append(" · ").append(t.getNum_veces()).append("x");
            fr.append(t.getDuracion() != null
                    ? " · " + t.getDuracion() + " min" : "");
            ((TextView) row.findViewById(R.id.tv_frecuencia)).setText(fr.toString());

            TextView tvCheck = row.findViewById(R.id.tv_check);
            tvCheck.setText("");
            tvCheck.setBackgroundResource(R.drawable.bg_tag_muted);
            tvCheck.setAlpha(0.4f);

            TextView tvTag = row.findViewById(R.id.tv_tag_estado);
            tvTag.setText(t.getFrecuencia() != null
                    ? t.getFrecuencia().toUpperCase(Locale.getDefault()) : "—");

            containerTareasHab.addView(row);
            agregarDivider(containerTareasHab);
        }
    }

    private String labelFrecuencia(String f) {
        if (f == null) return "—";
        switch (f.toLowerCase(Locale.getDefault())) {
            case "diaria":    return "Diaria";
            case "semanal":   return "Semanal";
            case "quincenal": return "Quincenal";
            case "mensual":   return "Mensual";
            case "variable":  return "Variable";
            default:          return f;
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

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
