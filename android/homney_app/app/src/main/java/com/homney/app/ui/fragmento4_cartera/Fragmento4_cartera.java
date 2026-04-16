package com.homney.app.ui.fragmento4_cartera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.homney.app.webservice.modelo.Gasto;
import com.homney.app.webservice.modelo.RepartoGasto;
import com.homney.app.webservice.modelo.Tarea;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Fragmento4_cartera extends Fragment {

    /* ── Vistas ──────────────────────────────────── */
    private TextView tvStatMeDeben;
    private TextView tvStatDebo;
    private TextView tvStatBalance;
    private TextView tagMeDeben;
    private TextView tagDebo;
    private TextView tagHistorial;
    private LinearLayout containerMeDeben;
    private LinearLayout containerDebo;
    private LinearLayout containerHistorial;
    private TextView tvSinMeDeben;
    private TextView tvSinDebo;
    private TextView tvSinHistorial;

    /* ── Sesión ──────────────────────────────────── */
    private int idUsuario = -1;
    private int idHogar = -1;

    /* ── Datos API ───────────────────────────────── */
    private List<Gasto> gastos = null;
    private List<RepartoGasto> misRepartos = null;
    private List<Usuario> usuarios = null;
    private Map<Integer, List<RepartoGasto>> repartosDeDeudores = null;

    private static final String TAG = "WS_CARTERA";

    /* ════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════ */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment4_cartera, container, false);

        tvStatMeDeben = root.findViewById(R.id.tv_stat_me_deben);
        tvStatDebo = root.findViewById(R.id.tv_stat_debo);
        tvStatBalance = root.findViewById(R.id.tv_stat_balance);
        tagMeDeben = root.findViewById(R.id.tag_me_deben);
        tagDebo = root.findViewById(R.id.tag_debo);
        tagHistorial = root.findViewById(R.id.tag_historial);
        containerMeDeben = root.findViewById(R.id.container_me_deben);
        containerDebo = root.findViewById(R.id.container_debo);
        containerHistorial = root.findViewById(R.id.container_historial);
        tvSinMeDeben = root.findViewById(R.id.tv_sin_me_deben);
        tvSinDebo = root.findViewById(R.id.tv_sin_debo);
        tvSinHistorial = root.findViewById(R.id.tv_sin_historial);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idUsuario = prefs.getInt("id_usuario", -1);
        idHogar = prefs.getInt("id_hogar", -1);

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
       Fase 1: gastos, misRepartos, usuarios (paralelos)
       Fase 2: repartos de cada gasto mío
    ════════════════════════════════════════════ */

    private void cargarDatos() {
        final AtomicInteger fase1 = new AtomicInteger(3);

        lanzarPeticion(WebService.URL_Gasto + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Gasto>>() {
                }.getType(),
                (List<Gasto> resp) -> {
                    gastos = resp;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        lanzarPeticion(WebService.URL_RepartoGasto + "?id_usuario=" + idUsuario,
                new TypeToken<RespuestaLista<RepartoGasto>>() {
                }.getType(),
                (List<RepartoGasto> resp) -> {
                    misRepartos = resp;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });

        lanzarPeticion(WebService.URL_Usuario + "?id_hogar=" + idHogar,
                new TypeToken<RespuestaLista<Usuario>>() {
                }.getType(),
                (List<Usuario> resp) -> {
                    usuarios = resp;
                    if (fase1.decrementAndGet() == 0) iniciarFase2();
                });
    }

    private void iniciarFase2() {
        List<Gasto> misGastos = new ArrayList<>();
        for (Gasto g : gastos) {
            if (g.getId_usuario_pagador() == idUsuario) misGastos.add(g);
        }
        if (misGastos.isEmpty()) {
            repartosDeDeudores = new HashMap<>();
            renderCartera();
            return;
        }
        repartosDeDeudores = new HashMap<>();
        AtomicInteger pendiente = new AtomicInteger(misGastos.size());
        for (Gasto g : misGastos) {
            String url = WebService.URL_RepartoGasto + "?id_gasto=" + g.getId_gasto();
            lanzarPeticion(url,
                    new TypeToken<RespuestaLista<RepartoGasto>>() {
                    }.getType(),
                    (List<RepartoGasto> resp) -> {
                        List<RepartoGasto> deudores = new ArrayList<>();
                        for (RepartoGasto r : resp) {
                            if (!r.isPagador() && r.getId_usuario() != idUsuario)
                                deudores.add(r);
                        }
                        synchronized (repartosDeDeudores) {
                            repartosDeDeudores.put(g.getId_gasto(), deudores);
                        }
                        if (pendiente.decrementAndGet() == 0) renderCartera();
                    });
        }
    }

    /* ════════════════════════════════════════════
       RECARGA (se llama tras confirmar un pago)
    ════════════════════════════════════════════ */

    private void recargarCartera() {
        if (!isAdded()) return;

        // Resetear containers restaurando los placeholders
        containerMeDeben.removeAllViews();
        tvSinMeDeben.setText("Cargando…");
        tvSinMeDeben.setVisibility(View.VISIBLE);
        containerMeDeben.addView(tvSinMeDeben);

        containerDebo.removeAllViews();
        tvSinDebo.setText("Cargando…");
        tvSinDebo.setVisibility(View.VISIBLE);
        containerDebo.addView(tvSinDebo);

        containerHistorial.removeAllViews();
        tvSinHistorial.setText("Cargando…");
        tvSinHistorial.setVisibility(View.VISIBLE);
        containerHistorial.addView(tvSinHistorial);

        tvStatMeDeben.setText("—");
        tvStatDebo.setText("—");
        tvStatBalance.setText("—");

        // Resetear datos y recargar
        gastos = null;
        misRepartos = null;
        usuarios = null;
        repartosDeDeudores = null;

        cargarDatos();
    }

    /* ════════════════════════════════════════════
       RENDERIZADO
    ════════════════════════════════════════════ */

    private void renderCartera() {
        if (!isAdded()) return;

        Map<Integer, Gasto> gastoMap = new HashMap<>();
        Map<Integer, Usuario> userMap = new HashMap<>();
        for (Gasto g : gastos) gastoMap.put(g.getId_gasto(), g);
        for (Usuario u : usuarios) userMap.put(u.getId_usuario(), u);

        // "Me deben": suma pendiente de mis gastos
        Map<Integer, DeudorInfo> meDebenMap = new HashMap<>();
        for (Gasto g : gastos) {
            if (g.getId_usuario_pagador() != idUsuario) continue;
            List<RepartoGasto> reps = repartosDeDeudores.getOrDefault(
                    g.getId_gasto(), new ArrayList<>());
            for (RepartoGasto r : reps) {
                int uid = r.getId_usuario();
                if (!meDebenMap.containsKey(uid))
                    meDebenMap.put(uid, new DeudorInfo(userMap.get(uid)));
                meDebenMap.get(uid).agregarItem(g, r);
            }
        }

        // "Debo yo": mis repartos donde no soy pagador
        Map<Integer, DeudorInfo> deboMap = new HashMap<>();
        for (RepartoGasto r : misRepartos) {
            if (r.isPagador()) continue;
            Gasto g = gastoMap.get(r.getId_gasto());
            if (g == null) continue;
            int pagadorId = g.getId_usuario_pagador();
            if (pagadorId == idUsuario) continue;
            if (!deboMap.containsKey(pagadorId))
                deboMap.put(pagadorId, new DeudorInfo(userMap.get(pagadorId)));
            deboMap.get(pagadorId).agregarItem(g, r);
        }

        double totalMeDeben = 0, totalDebo = 0;
        for (DeudorInfo d : meDebenMap.values()) totalMeDeben += d.totalPendiente;
        for (DeudorInfo d : deboMap.values()) totalDebo += d.totalPendiente;
        double balance = totalMeDeben - totalDebo;

        tvStatMeDeben.setText("+" + fmt(totalMeDeben));
        tvStatDebo.setText("−" + fmt(totalDebo));
        tvStatBalance.setText((balance >= 0 ? "+" : "") + fmt(balance));
        tvStatBalance.setTextColor(balance >= 0 ? 0xFF58A856 : 0xFFE05C5C);
        tagMeDeben.setText(fmt(totalMeDeben) + " pendiente");
        tagDebo.setText(fmt(totalDebo) + " pendiente");
        tagHistorial.setText(gastos.size() + (gastos.size() == 1 ? " registro" : " registros"));

        // Lo que te deben
        tvSinMeDeben.setVisibility(View.GONE);
        if (meDebenMap.isEmpty()) {
            tvSinMeDeben.setText("¡Nadie te debe dinero!");
            tvSinMeDeben.setVisibility(View.VISIBLE);
        } else {
            for (DeudorInfo d : meDebenMap.values())
                containerMeDeben.addView(buildBloqueReparto(d, true));
        }

        // Lo que debes tú
        tvSinDebo.setVisibility(View.GONE);
        if (deboMap.isEmpty()) {
            tvSinDebo.setText("¡No debes nada a nadie!");
            tvSinDebo.setVisibility(View.VISIBLE);
        } else {
            for (DeudorInfo d : deboMap.values())
                containerDebo.addView(buildBloqueReparto(d, false));
        }

        // Historial de gastos con swipe-to-delete
        tvSinHistorial.setVisibility(View.GONE);
        if (gastos.isEmpty()) {
            tvSinHistorial.setText("Sin gastos registrados.");
            tvSinHistorial.setVisibility(View.VISIBLE);
        } else {
            LayoutInflater inf = LayoutInflater.from(requireContext());
            for (Gasto g : gastos) {
                // 1. FrameLayout contenedor (misma técnica que en Tareas)
                FrameLayout rootFrame = new FrameLayout(requireContext());
                rootFrame.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                // 2. Botón rojo DEBAJO (capa inferior)
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

                // 3. Fila de gasto ENCIMA (capa superior, fondo blanco opaco)
                View row = inf.inflate(R.layout.item_gasto_historial, rootFrame, false);
                row.setBackgroundColor(0xFFFFFFFF);
                rootFrame.addView(row);

                // 4. Rellenar datos de la fila
                rellenarFilaGasto(row, g, userMap);

                // 5. Touch listener para swipe + click en botón rojo
                setupSwipeDeleteGasto(row, deleteBtn, g, rootFrame);

                containerHistorial.addView(rootFrame);
                agregarDivider(containerHistorial);
            }
        }
    }

    /* ════════════════════════════════════════════
       SWIPE-TO-DELETE — Historial de gastos
    ════════════════════════════════════════════ */

    private void setupSwipeDeleteGasto(View row, LinearLayout deleteBtn,
                                       Gasto g, FrameLayout rootFrame) {
        // Foreground ripple para feedback de tap (el fondo blanco sólido permanece)
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, outValue, true);
        row.setForeground(requireContext().getDrawable(outValue.resourceId));
        row.setClickable(true);
        row.setFocusable(true);

        final float[] x1    = {0};
        final float[] initTX = {0};
        final int MAX_SWIPE  = dp(-80);

        row.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1[0]     = event.getRawX();
                    initTX[0] = v.getTranslationX();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float delta  = event.getRawX() - x1[0];
                    float newTX  = initTX[0] + delta;
                    if (newTX <= 0 && newTX >= MAX_SWIPE)
                        v.setTranslationX(newTX);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (v.getTranslationX() < MAX_SWIPE / 2f)
                        v.animate().translationX(MAX_SWIPE).setDuration(200).start();
                    else
                        v.animate().translationX(0).setDuration(200).start();
                    return true;
            }
            return false;
        });

        deleteBtn.setOnClickListener(v -> borrarGasto(g, rootFrame));
    }

    /* ════════════════════════════════════════════
       BORRAR GASTO — réplica de deleteGasto() del web:
       1) DELETE reparto_gasto.php?id_gasto=X
       2) DELETE gasto.php?id_gasto=X
       3) recargarCartera() para actualizar totales
    ════════════════════════════════════════════ */

    private void borrarGasto(Gasto g, FrameLayout rootFrame) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar gasto?")
                .setMessage("¿Seguro que quieres eliminar «" + g.getConcepto()
                        + "» y su reparto asociado?")
                .setPositiveButton("Eliminar", (dialog, which) ->
                        eliminarRepartoYGasto(g, rootFrame))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarRepartoYGasto(Gasto g, FrameLayout rootFrame) {
        String urlReparto = WebService.URL_RepartoGasto + "?id_gasto=" + g.getId_gasto();
        String urlGasto   = WebService.URL_Gasto        + "?id_gasto=" + g.getId_gasto();

        // Paso 2: DELETE gasto (se lanza desde el callback del paso 1)
        JsonObjectRequest deleteGasto = new JsonObjectRequest(
                Request.Method.DELETE, urlGasto, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    "Gasto eliminado", Toast.LENGTH_SHORT).show();
                            // Recalcular todo (totales + historial sin el gasto borrado)
                            recargarCartera();
                        } else {
                            Toast.makeText(requireContext(),
                                    "No se pudo eliminar: "
                                            + response.optString("message", ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error al eliminar gasto", Request.Method.DELETE, urlGasto, error)
        );

        // Paso 1: DELETE reparto (libera FK); luego lanza el paso 2
        JsonObjectRequest deleteReparto = new JsonObjectRequest(
                Request.Method.DELETE, urlReparto, null,
                respReparto -> PeticionesRed.anhadirPeticionACola(deleteGasto),
                error -> {
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al eliminar reparto", Request.Method.DELETE, urlReparto, error);
                    // Intentamos borrar el gasto de todas formas
                    PeticionesRed.anhadirPeticionACola(deleteGasto);
                }
        );

        PeticionesRed.anhadirPeticionACola(deleteReparto);
    }

    /* ════════════════════════════════════════════
       BUILDER: bloque de reparto por persona
    ════════════════════════════════════════════ */

    private View buildBloqueReparto(DeudorInfo d, boolean esMeDeben) {
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

        // Cabecera
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));
        header.setBackgroundColor(esMeDeben ? 0xFFE8F5E8 : 0xFFFDEAEA);

        TextView tvIni = new TextView(requireContext());
        int sz = dp(36);
        LinearLayout.LayoutParams iniLp = new LinearLayout.LayoutParams(sz, sz);
        iniLp.rightMargin = dp(10);
        tvIni.setLayoutParams(iniLp);
        tvIni.setGravity(android.view.Gravity.CENTER);
        tvIni.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvIni.setTypeface(null, Typeface.BOLD);
        tvIni.setTextColor(esMeDeben ? 0xFF58A856 : 0xFFE05C5C);
        tvIni.setBackgroundColor(esMeDeben ? 0xFFC8E6C9 : 0xFFFFCDD2);
        String nombre = d.user != null ? d.user.getNombre() : "?";
        tvIni.setText(String.valueOf(nombre.charAt(0)).toUpperCase(Locale.getDefault()));
        header.addView(tvIni);

        LinearLayout infoCol = new LinearLayout(requireContext());
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvNombre = new TextView(requireContext());
        tvNombre.setText(nombre);
        tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setTextColor(0xFF2D2416);
        infoCol.addView(tvNombre);
        long countPend = d.items.stream().filter(i -> !i.reparto.isAbonado()).count();
        TextView tvCount = new TextView(requireContext());
        tvCount.setText(countPend + " gasto" + (countPend != 1 ? "s" : "")
                + " pendiente" + (countPend != 1 ? "s" : ""));
        tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvCount.setTextColor(0xFF9A8A6A);
        infoCol.addView(tvCount);
        header.addView(infoCol);

        TextView tvTotal = new TextView(requireContext());
        tvTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvTotal.setTypeface(null, Typeface.BOLD);
        tvTotal.setTextColor(esMeDeben ? 0xFF58A856 : 0xFFE05C5C);
        tvTotal.setText((esMeDeben ? "+" : "−") + fmt(d.totalPendiente));
        tvTotal.setGravity(android.view.Gravity.END);
        header.addView(tvTotal);
        inner.addView(header);

        for (ItemReparto item : d.items) {
            inner.addView(buildItemDeuda(item, esMeDeben));
        }

        cv.addView(inner);
        return cv;
    }

    /**
     * Construye una fila de deuda individual.
     * Si el item NO está abonado → el click abre un AlertDialog para confirmar el pago.
     * La lógica replica cobrarDeuda() / abonarDeuda() del web.
     */
    private View buildItemDeuda(ItemReparto item, boolean esMeDeben) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        if (item.reparto.isAbonado()) row.setAlpha(0.55f);

        // Icono modo
        android.widget.ImageView imgModo = new android.widget.ImageView(requireContext());
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        imgLp.rightMargin = dp(10);
        imgModo.setLayoutParams(imgLp);
        imgModo.setImageResource(R.drawable.ic_credit_card);
        imgModo.setBackgroundColor(0xFFFFF3C0);
        imgModo.setPadding(dp(5), dp(5), dp(5), dp(5));
        row.addView(imgModo);

        // Concepto + meta
        LinearLayout bodyCol = new LinearLayout(requireContext());
        bodyCol.setOrientation(LinearLayout.VERTICAL);
        bodyCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvConcepto = new TextView(requireContext());
        tvConcepto.setText(item.gasto.getConcepto());
        tvConcepto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvConcepto.setTextColor(item.reparto.isAbonado() ? 0xFF9A8A6A : 0xFF2D2416);
        if (item.reparto.isAbonado())
            tvConcepto.setPaintFlags(tvConcepto.getPaintFlags()
                    | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        bodyCol.addView(tvConcepto);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText(formatFecha(item.gasto.getFecha())
                + (item.gasto.getCategoria() != null ? " · " + item.gasto.getCategoria() : ""));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tvSub.setTextColor(0xFF9A8A6A);
        bodyCol.addView(tvSub);
        row.addView(bodyCol);

        // Importe + estado
        LinearLayout rightCol = new LinearLayout(requireContext());
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(android.view.Gravity.END);

        TextView tvImporte = new TextView(requireContext());
        tvImporte.setText((esMeDeben ? "+" : "−") + fmt(item.reparto.getImporte()));
        tvImporte.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvImporte.setTypeface(null, Typeface.BOLD);
        tvImporte.setTextColor(esMeDeben ? 0xFF58A856 : 0xFFE05C5C);
        rightCol.addView(tvImporte);

        if (item.reparto.isAbonado()) {
            TextView tvTag = new TextView(requireContext());
            tvTag.setText(esMeDeben ? "COBRADO" : "PAGADO");
            tvTag.setBackgroundResource(R.drawable.bg_tag_green);
            tvTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            tvTag.setTextColor(0xFF58A856);
            tvTag.setTypeface(null, Typeface.BOLD);
            tvTag.setPadding(dp(8), dp(2), dp(8), dp(2));
            rightCol.addView(tvTag);
        } else {
            // Indicador de tap disponible
            TextView tvTap = new TextView(requireContext());
            tvTap.setText("Pulsa para confirmar");
            tvTap.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
            tvTap.setTextColor(esMeDeben ? 0xFF58A856 : 0xFFE05C5C);
            tvTap.setTypeface(null, Typeface.ITALIC);
            rightCol.addView(tvTap);
        }
        row.addView(rightCol);

        // Wrapper con divisor
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);

        View div = new View(requireContext());
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divLp.leftMargin = dp(52);
        div.setLayoutParams(divLp);
        div.setBackgroundColor(0xFFE8DFC0);
        wrapper.addView(div);

        // ── Click listener para items PENDIENTES ──────────────
        if (!item.reparto.isAbonado()) {
            wrapper.setClickable(true);
            wrapper.setFocusable(true);
            // Ripple effect
            TypedValue outValue = new TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true);
            wrapper.setBackgroundResource(outValue.resourceId);

            wrapper.setOnClickListener(v2 -> mostrarDialogConfirmacion(item, esMeDeben));
        }

        return wrapper;
    }

    /* ════════════════════════════════════════════
       AlertDialog de confirmación de pago/cobro
    ════════════════════════════════════════════ */

    private void mostrarDialogConfirmacion(ItemReparto item, boolean esMeDeben) {
        String importe = fmt(item.reparto.getImporte());
        String concepto = item.gasto.getConcepto();

        String titulo = esMeDeben ? "Confirmar cobro" : "Confirmar pago";
        String mensaje = esMeDeben
                ? "¿Confirmas que te han pagado " + importe + "?\n\n«" + concepto + "»"
                : "¿Confirmas el pago de " + importe + "?\n\n«" + concepto + "»";

        new AlertDialog.Builder(requireContext())
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Sí, confirmar", (dialog, which) ->
                        marcarAbonado(item))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * PUT reparto_gasto.php → marca la deuda como abonada.
     * Equivale a cobrarDeuda() / abonarDeuda() del web.
     */
    private void marcarAbonado(ItemReparto item) {
        JSONObject body = new JSONObject();
        try {
            body.put("id_gasto", item.gasto.getId_gasto());
            body.put("id_usuario", item.reparto.getId_usuario());
            body.put("abonado", true);
            body.put("importe", item.reparto.getImporte());
            body.put("pagador", false);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error al preparar la solicitud", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = WebService.URL_RepartoGasto;
        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.PUT, url, body,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    "Pago registrado ✓", Toast.LENGTH_SHORT).show();
                            recargarCartera();
                        } else {
                            String msg = response.optString("message", "Error al registrar el pago");
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error al abonar", Request.Method.PUT, url, error)
        );
        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════
       HISTORIAL: rellena fila de gasto
    ════════════════════════════════════════════ */

    private void rellenarFilaGasto(View row, Gasto g,
                                   Map<Integer, Usuario> userMap) {
        boolean esPropio = g.getId_usuario_pagador() == idUsuario;
        ((TextView) row.findViewById(R.id.tv_concepto)).setText(g.getConcepto());

        Usuario pagador = userMap.get(g.getId_usuario_pagador());
        String quienPago = esPropio ? "Pagaste tú"
                : "Pagó " + (pagador != null ? pagador.getNombre() : "#" + g.getId_usuario_pagador());
        String meta = (g.getCategoria() != null ? g.getCategoria() : "—")
                + " · " + formatFecha(g.getFecha())
                + " · " + (g.getModo() != null ? g.getModo() : "—")
                + " · " + quienPago;
        ((TextView) row.findViewById(R.id.tv_meta_gasto)).setText(meta);

        TextView tvImporte = row.findViewById(R.id.tv_importe);
        tvImporte.setText((esPropio ? "+" : "−") + fmt(g.getImporte()));
        tvImporte.setTextColor(esPropio ? 0xFF58A856 : 0xFFE05C5C);

        TextView tvTag = row.findViewById(R.id.tv_tag_tipo);
        tvTag.setText(g.getTipo() != null ? g.getTipo().toUpperCase(Locale.getDefault()) : "—");
        tvTag.setBackgroundResource("fijo".equals(g.getTipo())
                ? R.drawable.bg_tag_yellow : R.drawable.bg_tag_warn);
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

    private void eliminarRepartosyGastos(Gasto g, View rootFrame) {
        String urlReparto = WebService.URL_RepartoGasto + "?reparto_gasto=" + g.getId_gasto();
        String urlGasto = WebService.URL_Gasto + "?id_gasto=" + g.getId_gasto();

        // ─── Paso 2: eliminar la tarea (se llama desde el callback del paso 1) ───
        JsonObjectRequest deleteGasto = new JsonObjectRequest(
                Request.Method.DELETE, urlGasto, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Toast.makeText(requireContext(),
                                    "Gasto Eliminado", Toast.LENGTH_SHORT).show();
                            // Quitar el FrameLayout (rootFrame) del contenedor
                            ViewGroup parent = (ViewGroup) rootFrame.getParent();
                            if (parent != null) parent.removeView(rootFrame);
                        } else {
                            Toast.makeText(requireContext(),
                                    "No se pudo eliminar el gasto: "
                                            + response.optString("message", ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Utilidades.mostrar_error_peticion(requireContext(), TAG,
                        "Error al eliminar gasto", Request.Method.DELETE, urlGasto, error)
        );

        // ─── Paso 1: eliminar el reparto (en callback lanza el paso 2) ─────────
        JsonObjectRequest deleteReparto = new JsonObjectRequest(
                Request.Method.DELETE, urlReparto, null,
                respRepart -> {
                    // Con o sin asignaciones, procedemos a borrar la tarea
                    PeticionesRed.anhadirPeticionACola(deleteGasto);
                },
                error -> {
                    // Si falla la petición de asignaciones, intentamos igualmente
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error al eliminar repartos", Request.Method.DELETE, urlReparto, error);
                    PeticionesRed.anhadirPeticionACola(deleteGasto);
                }
        );

        PeticionesRed.anhadirPeticionACola(deleteReparto);
    }

    /* ════════════════════════════════════════════
       MODELOS INTERNOS
    ════════════════════════════════════════════ */

    private static class ItemReparto {
        Gasto gasto;
        RepartoGasto reparto;

        ItemReparto(Gasto g, RepartoGasto r) {
            gasto = g;
            reparto = r;
        }
    }

    private static class DeudorInfo {
        Usuario user;
        List<ItemReparto> items = new ArrayList<>();
        double totalPendiente = 0;
        double totalAbonado = 0;

        DeudorInfo(Usuario u) {
            user = u;
        }

        void agregarItem(Gasto g, RepartoGasto r) {
            items.add(new ItemReparto(g, r));
            if (!r.isAbonado()) totalPendiente += r.getImporte();
            else totalAbonado += r.getImporte();
        }
    }

    /* ════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════ */

    private String fmt(double importe) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("es", "ES"));
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        return df.format(importe) + " €";
    }

    private String formatFecha(String fechaStr) {
        if (fechaStr == null) return "—";
        try {
            SimpleDateFormat p = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = p.parse(fechaStr);
            return d != null ? f.format(d) : fechaStr;
        } catch (Exception e) {
            return fechaStr;
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
