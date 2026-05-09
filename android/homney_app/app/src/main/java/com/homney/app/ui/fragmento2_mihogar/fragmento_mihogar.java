package com.homney.app.ui.fragmento2_mihogar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.homney.app.webservice.modelo.Habitacion;
import com.homney.app.webservice.modelo.Hogar;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class fragmento_mihogar extends Fragment {

    /* ── Vistas del layout ──────────────────────────────── */
    private TextView     tvHogarTitulo;
    private TextView     tvClaveHogar;
    private TextView     tvSinUsuarios;
    private TextView     tvSinHabitaciones;
    private LinearLayout containerUsuarios;
    private LinearLayout gridHabitaciones;
    private ImageView    btnAnadirHabitacion;

    /* ── Datos de sesión ────────────────────────────────── */
    private int    idHogar   = -1;
    private String rolUsuario = "";

    private LoadingDialog loadingDialog;
    private final AtomicInteger peticionesPendientes = new AtomicInteger(0);

    private static final String TAG = "WS_HOGAR";


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment2_hogar, container, false);

        // Enlazar vistas
        tvHogarTitulo        = root.findViewById(R.id.tv_hogar_titulo);
        tvClaveHogar         = root.findViewById(R.id.tareas_pendientes);
        tvSinUsuarios        = root.findViewById(R.id.tv_sin_usuarios);
        tvSinHabitaciones    = root.findViewById(R.id.tv_sin_habitaciones);
        containerUsuarios    = root.findViewById(R.id.container_usuarios);
        gridHabitaciones     = root.findViewById(R.id.grid_habitaciones);
        btnAnadirHabitacion  = root.findViewById(R.id.btn_anadir_habitacion);

        loadingDialog = new LoadingDialog(requireContext());

        // Botón "+" → abre el BottomSheet para añadir habitación
        btnAnadirHabitacion.setOnClickListener(v -> {
            AnadirHabitacionBottomSheet sheet = new AnadirHabitacionBottomSheet();
            sheet.setOnHabitacionAnadidaListener(() -> recargarHabitaciones());
            sheet.show(getChildFragmentManager(), "anadir_habitacion");
        });

        // Leer datos de sesión guardados en login
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar    = prefs.getInt("id_hogar", -1);
        rolUsuario = prefs.getString("rol", "");


        // Iniciar las tres peticiones en paralelo
        if (idHogar != -1) {
            if (Utilidades.hayConexionInternet(requireContext())) {
                peticionesPendientes.set(3);
                loadingDialog.show();
                cargarInfoHogar();
                cargarUsuarios();
                cargarHabitaciones();
            } else {
                Toast.makeText(requireContext(),
                        "No hay conexión a Internet", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(),
                    "Sesión no válida — vuelve a iniciar sesión", Toast.LENGTH_LONG).show();
        }

        return root;
    }
    // PETICIONES WEB SERVICE

    private void cargarInfoHogar() {
        String endPoint = WebService.URL_Hogar + "?id_hogar=" + idHogar;

        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, endPoint, null,
                response -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Hogar>>() {}.getType();
                            RespuestaLista<Hogar> resp =
                                    gson.fromJson(response.toString(), tipo);

                            if (resp.data != null && !resp.data.isEmpty()) {
                                Hogar h = resp.data.get(0);
                                if(h.getNombre() != null){
                                    tvHogarTitulo.setText(h.getNombre());
                                }else{
                                    tvHogarTitulo.setText("Hogar");
                                }
                                tvClaveHogar.setText(h.getClave_inv());
                            }
                        }
                    } catch (JSONException e) {
                        // Si falla la info del hogar no bloqueamos el resto
                    }
                },
                error -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    Utilidades.mostrar_error_peticion(
                            requireContext(), TAG,
                            "Error cargando hogar",
                            Request.Method.GET, endPoint, error);
                }
        );

        PeticionesRed.anhadirPeticionACola(peticion);
    }


    private void cargarUsuarios() {
        String endPoint = WebService.URL_Usuario + "?id_hogar=" + idHogar;

        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, endPoint, null,
                response -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> resp =
                                    gson.fromJson(response.toString(), tipo);

                            if (resp.data != null && !resp.data.isEmpty()) {
                                mostrarUsuarios(resp.data);
                            } else {
                                tvSinUsuarios.setText("Sin compañeros registrados");
                                tvSinUsuarios.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar usuarios", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    Utilidades.mostrar_error_peticion(
                            requireContext(), TAG,
                            "Error cargando usuarios",
                            Request.Method.GET, endPoint, error);
                }
        );

        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════════════════
       PETICIÓN 3 — Habitaciones del Hogar
    ════════════════════════════════════════════════════════ */

    private void cargarHabitaciones() {
        String endPoint = WebService.URL_Habitacion + "?id_hogar=" + idHogar;

        JsonObjectRequest peticion = new JsonObjectRequest(
                Request.Method.GET, endPoint, null,
                response -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {

                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Habitacion>>() {}.getType();
                            RespuestaLista<Habitacion> resp =
                                    gson.fromJson(response.toString(), tipo);

                            if (resp.data != null && !resp.data.isEmpty()) {
                                mostrarHabitaciones(resp.data);
                            } else {
                                tvSinHabitaciones.setText("Sin habitaciones registradas");
                                tvSinHabitaciones.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (JSONException e) {
                        Toast.makeText(requireContext(),
                                "Error al procesar habitaciones", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (peticionesPendientes.decrementAndGet() == 0) {
                        loadingDialog.dismiss();
                    }
                    Utilidades.mostrar_error_peticion(
                            requireContext(), TAG,
                            "Error cargando habitaciones",
                            Request.Method.GET, endPoint, error);
                }
        );

        PeticionesRed.anhadirPeticionACola(peticion);
    }

    /* ════════════════════════════════════════════════════════
       RENDERIZADO — Tarjetas de usuarios
    ════════════════════════════════════════════════════════ */

    private void mostrarUsuarios(List<Usuario> usuarios) {
        // Quitar el placeholder "Cargando…"
        tvSinUsuarios.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Usuario u : usuarios) {
            View card = inflater.inflate(
                    R.layout.item_usuario_hogar, containerUsuarios, false);

            ((TextView) card.findViewById(R.id.tv_nombre)).setText(u.getNombre());
            ((TextView) card.findViewById(R.id.tv_email)).setText(u.getEmail());

            TextView tvRol = card.findViewById(R.id.tv_rol);
            String rol = u.getRol() != null ? u.getRol() : "miembro";
            tvRol.setText(rol.toUpperCase(Locale.getDefault()));

            if ("fundador".equalsIgnoreCase(rol)) {
                tvRol.setBackgroundResource(R.drawable.bg_tag_yellow);
            } else {
                tvRol.setBackgroundResource(R.drawable.bg_tag_green);
            }

            containerUsuarios.addView(card);
        }
    }

    /* ════════════════════════════════════════════════════════
       RENDERIZADO — Grid de habitaciones (2 columnas)
    ════════════════════════════════════════════════════════ */

    private void mostrarHabitaciones(List<Habitacion> habitaciones) {
        // Quitar el placeholder "Cargando…"
        tvSinHabitaciones.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = dp(8);   // separación horizontal entre las dos columnas

        LinearLayout fila = null;

        for (int i = 0; i < habitaciones.size(); i++) {

            // Cada 2 items creamos una nueva fila horizontal
            if (i % 2 == 0) {
                fila = new LinearLayout(requireContext());
                fila.setOrientation(LinearLayout.HORIZONTAL);

                LinearLayout.LayoutParams filaParms = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                filaParms.bottomMargin = dp(10);
                fila.setLayoutParams(filaParms);
                gridHabitaciones.addView(fila);
            }

            // Inflar tarjeta de habitación
            View cardHab = inflater.inflate(
                    R.layout.item_habitacion_hogar, fila, false);

            // Ocupar la mitad del ancho disponible con weight=1
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i % 2 == 0) {
                lp.rightMargin = gap / 2;   // columna izquierda
            } else {
                lp.leftMargin  = gap / 2;   // columna derecha
            }
            cardHab.setLayoutParams(lp);

            Habitacion h = habitaciones.get(i);
            ((TextView) cardHab.findViewById(R.id.tv_nombre_hab)).setText(h.getNombre());
            ((TextView) cardHab.findViewById(R.id.tv_tipo_hab)).setText(h.getTipo());

            // Icono según tipo de habitación
            ImageView imgTipo = cardHab.findViewById(R.id.img_tipo_hab);
            imgTipo.setImageResource(iconoParaTipo(h.getTipo()));

            fila.addView(cardHab);
        }

        // Si el número de habitaciones es impar, añadir una vista vacía
        // para que la última tarjeta ocupe solo la mitad izquierda
        if (fila != null && habitaciones.size() % 2 != 0) {
            View placeholder = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
            lp.leftMargin = gap / 2;
            fila.addView(placeholder, lp);
        }
    }

    /* ════════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════════ */

    private int iconoParaTipo(String tipo) {
        if (tipo == null) return R.drawable.ic_home2;
        switch (tipo.toLowerCase(Locale.getDefault())) {
            case "cocina":          return R.drawable.ic_cocina;
            case "aseo":            return R.drawable.outline_bathroom_24;
            case "garaje":          return R.drawable.outline_garage_24;
            case "exterior":        return R.drawable.outline_outdoor_garden_24;
            case "dormitorio":      return R.drawable.ic_dormitorio;
            case "infantil":        return R.drawable.outline_bedroom_baby_24;
            case "comedor":         return R.drawable.ic_comedor;
            case "salon":           return R.drawable.outline_chair_24;
            case "oficina":         return R.drawable.outline_add_home_work_24;
            case "trastero":        return R.drawable.outline_inventory_2_24;
            case "recibidor":       return R.drawable.outline_meeting_room_24;
            case "terraza":         return R.drawable.outline_balcony_24;
            case "deportiva":       return R.drawable.outline_fitness_center_24;
            case "generica":        return R.drawable.outline_nest_multi_room_24;
            default:         return R.drawable.ic_home2;
        }
    }

    /** Convierte dp a píxeles usando la densidad de la pantalla actual. */
    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }

    /* ════════════════════════════════════════════════════════
       RECARGA DEL GRID DE HABITACIONES
       Llamado desde AnadirHabitacionBottomSheet tras un POST exitoso
    ════════════════════════════════════════════════════════ */

    private void recargarHabitaciones() {
        // Limpiar el grid conservando el placeholder tv_sin_habitaciones
        gridHabitaciones.removeAllViews();
        gridHabitaciones.addView(tvSinHabitaciones);
        tvSinHabitaciones.setText("Cargando habitaciones…");
        tvSinHabitaciones.setVisibility(View.VISIBLE);

        cargarHabitaciones();
    }
}
