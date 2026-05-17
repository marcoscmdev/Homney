package com.homney.app.ui.fragmento5_muro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.homney.app.webservice.modelo.Muro;
import com.homney.app.webservice.modelo.Usuario;
import com.homney.app.webservice.respuestas.RespuestaLista;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Fragmento5_muro extends Fragment {

    /* ── Vistas ─────────────────────────────────────────── */
    private RecyclerView recycler;
    private TextView     tvSinPublicaciones;

    private LoadingDialog loadingDialog;

    /* ── Sesión ─────────────────────────────────────────── */
    private int idHogar   = -1;
    private int idUsuario = -1;

    /* ── Datos ──────────────────────────────────────────── */
    private List<Muro>    listaPublis   = new ArrayList<>();
    private List<Usuario> listaUsuarios = new ArrayList<>();

    private static final String TAG = "WS_MURO";

    /* ════════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════════ */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment5_muro, container, false);

        // Enlazar vistas
        recycler           = root.findViewById(R.id.recycler);
        tvSinPublicaciones = root.findViewById(R.id.tv_sin_publicaciones);

        loadingDialog = new LoadingDialog(requireContext());

        // Configurar RecyclerView (el adapter se asigna cuando lleguen los datos)
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        // Leer sesión
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar   = prefs.getInt("id_hogar",   -1);
        idUsuario = prefs.getInt("id_usuario", -1);

        if (idHogar != -1) {
            if (Utilidades.hayConexionInternet(requireContext())) {
                cargarDatos();
            } else {
                tvSinPublicaciones.setText("Sin conexión a Internet");
            }
        } else {
            Toast.makeText(requireContext(),
                    "Sesión no válida — vuelve a iniciar sesión",
                    Toast.LENGTH_LONG).show();
        }

        return root;
    }

    /* ════════════════════════════════════════════════════════
       PETICIONES — Publicaciones y Usuarios en paralelo
       Cuando las dos lleguen → mostrarPublicaciones()
    ════════════════════════════════════════════════════════ */

    private void cargarDatos() {
        loadingDialog.show();
        AtomicInteger pendiente = new AtomicInteger(2);

        // 1) Publicaciones del muro filtradas por hogar
        String urlMuro = WebService.URL_Muro + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, urlMuro, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Muro>>() {}.getType();
                            RespuestaLista<Muro> resp = gson.fromJson(response.toString(), tipo);
                            if (resp.data != null) listaPublis = resp.data;
                        }
                    } catch (JSONException e) { /* ignorar */ }
                    if (pendiente.decrementAndGet() == 0) mostrarPublicaciones();
                },
                error -> {
                    Utilidades.mostrar_error_peticion(requireContext(), TAG,
                            "Error cargando muro", Request.Method.GET, urlMuro, error);
                    if (pendiente.decrementAndGet() == 0) mostrarPublicaciones();
                }
        ));

        // 2) Usuarios del hogar (para mostrar el nombre del autor)
        String urlUsuarios = WebService.URL_Usuario + "?id_hogar=" + idHogar;
        PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                Request.Method.GET, urlUsuarios, null,
                response -> {
                    try {
                        if (response.getString(WebService.JSON.STATUS)
                                .equals(WebService.JSON.SUCCESS)) {
                            Gson gson = new GsonBuilder().create();
                            Type tipo = new TypeToken<RespuestaLista<Usuario>>() {}.getType();
                            RespuestaLista<Usuario> resp = gson.fromJson(response.toString(), tipo);
                            if (resp.data != null) listaUsuarios = resp.data;
                        }
                    } catch (JSONException e) { /* ignorar */ }
                    if (pendiente.decrementAndGet() == 0) mostrarPublicaciones();
                },
                error -> { if (pendiente.decrementAndGet() == 0) mostrarPublicaciones(); }
        ));
    }

    /* ════════════════════════════════════════════════════════
       RENDERIZADO
    ════════════════════════════════════════════════════════ */

    private void mostrarPublicaciones() {
        if (!isAdded()) return;
        loadingDialog.dismiss();

        if (listaPublis.isEmpty()) {
            tvSinPublicaciones.setText("El muro está vacío");
            tvSinPublicaciones.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            return;
        }

        // Construir mapa id_usuario → nombre para el adapter
        Map<Integer, String> nombresPorUsuario = new HashMap<>();
        for (Usuario u : listaUsuarios) {
            nombresPorUsuario.put(u.getId_usuario(), u.getNombre());
        }

        tvSinPublicaciones.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        recycler.setAdapter(new RvMuroAdapter(
                listaPublis, nombresPorUsuario, idUsuario, this::eliminarPublicacion));
    }

    /* ════════════════════════════════════════════════════════
       ELIMINAR PUBLICACIÓN
    ════════════════════════════════════════════════════════ */

    private void eliminarPublicacion(int idPub, int position) {
        if (!isAdded()) return;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar publicación")
                .setMessage("¿Seguro que quieres eliminar esta publicación? No se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (!Utilidades.hayConexionInternet(requireContext())) {
                        Toast.makeText(requireContext(),
                                "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String url = WebService.URL_Muro + "?id_pub=" + idPub;
                    PeticionesRed.anhadirPeticionACola(new JsonObjectRequest(
                            Request.Method.DELETE, url, null,
                            response -> {
                                if (!isAdded()) return;
                                try {
                                    if (WebService.JSON.SUCCESS.equals(
                                            response.getString(WebService.JSON.STATUS))) {
                                        requireActivity().runOnUiThread(() -> {
                                            if (position >= 0 && position < listaPublis.size()) {
                                                listaPublis.remove(position);
                                                recycler.getAdapter().notifyItemRemoved(position);
                                                if (listaPublis.isEmpty()) {
                                                    tvSinPublicaciones.setText("El muro está vacío");
                                                    tvSinPublicaciones.setVisibility(View.VISIBLE);
                                                    recycler.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                                    } else {
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(requireContext(),
                                                        "No se pudo eliminar la publicación",
                                                        Toast.LENGTH_SHORT).show());
                                    }
                                } catch (org.json.JSONException e) { /* ignorar */ }
                            },
                            error -> {
                                if (!isAdded()) return;
                                Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                        "Error al eliminar", Request.Method.DELETE, url, error);
                            }
                    ));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
