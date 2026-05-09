package com.homney.app.ui.fragmento2_mihogar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.homney.app.R;
import com.homney.app.Utilidades;
import com.homney.app.webservice.PeticionesRed;
import com.homney.app.webservice.WebService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * BottomSheet para añadir una habitación al hogar.
 * <p>
 * Flujo:
 *  1. Muestra un grid de tarjetas con todos los tipos disponibles.
 *  2. Al pulsar una tarjeta queda resaltada y se muestra el campo de nombre
 *     (pre-rellenado con el nombre legible del tipo).
 *  3. El botón «Añadir» envía POST a habitacion.php y llama al listener.
 */
public class AnadirHabitacionBottomSheet extends BottomSheetDialogFragment {

    /* ── Callback hacia fragmento_mihogar ────────────── */
    public interface OnHabitacionAnadidaListener {
        void onHabitacionAnadida();
    }

    private OnHabitacionAnadidaListener listener;

    public void setOnHabitacionAnadidaListener(OnHabitacionAnadidaListener l) {
        this.listener = l;
    }

    /* ── Tipos de habitación disponibles ─────────────── */
    private static final String[][] TIPOS = {
            {"cocina",      "Cocina"},
            {"dormitorio",  "Dormitorio"},
            {"salon",       "Salón"},
            {"aseo",        "Aseo / Baño"},
            {"comedor",     "Comedor"},
            {"oficina",     "Oficina"},
            {"garaje",      "Garaje"},
            {"trastero",    "Trastero"},
            {"recibidor",   "Recibidor"},
            {"terraza",     "Terraza"},
            {"exterior",    "Exterior"},
            {"infantil",    "Infantil"},
            {"deportiva",   "Deportiva"},
            {"generica",    "Genérica"},
    };

    private static final String TAG = "ANADIR_HAB";

    /* ── Estado ──────────────────────────────────────── */
    private String tipoSeleccionado = null;
    private CardView cardSeleccionada = null;

    /* ── Vistas ──────────────────────────────────────── */
    private TextInputLayout tilNombre;
    private com.google.android.material.textfield.TextInputEditText etNombre;
    private Button btnAnadir;

    /* ── Sesión ──────────────────────────────────────── */
    private int idHogar = -1;

    /* ════════════════════════════════════════════════════
       CICLO DE VIDA
    ════════════════════════════════════════════════════ */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_anadir_habitacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilNombre = view.findViewById(R.id.til_nombre_hab);
        etNombre  = view.findViewById(R.id.et_nombre_hab);
        btnAnadir = view.findViewById(R.id.btn_anadir_habitacion);
        LinearLayout grid = view.findViewById(R.id.grid_tipos_habitacion);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("sesion", Context.MODE_PRIVATE);
        idHogar = prefs.getInt("id_hogar", -1);

        // Construir el grid de tarjetas (3 columnas)
        construirGridTipos(grid);

        // Activar/desactivar botón según si hay nombre escrito
        etNombre.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                btnAnadir.setEnabled(tipoSeleccionado != null && s.toString().trim().length() > 0);
            }
        });

        btnAnadir.setOnClickListener(v -> enviarHabitacion());
    }

    /* ════════════════════════════════════════════════════
       GRID DE TIPOS
    ════════════════════════════════════════════════════ */

    private void construirGridTipos(LinearLayout contenedor) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int gap = dp(8);
        final int COLUMNAS = 3;

        LinearLayout fila = null;

        for (int i = 0; i < TIPOS.length; i++) {
            if (i % COLUMNAS == 0) {
                fila = new LinearLayout(requireContext());
                fila.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                filaLp.bottomMargin = gap;
                fila.setLayoutParams(filaLp);
                contenedor.addView(fila);
            }

            final String tipo       = TIPOS[i][0];
            final String nombreLeg  = TIPOS[i][1];

            View cardView = inflater.inflate(R.layout.item_tipo_hab_selector, fila, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            int col = i % COLUMNAS;
            if (col == 0) {
                lp.rightMargin = gap / 2;
            } else if (col == COLUMNAS - 1) {
                lp.leftMargin = gap / 2;
            } else {
                lp.leftMargin  = gap / 2;
                lp.rightMargin = gap / 2;
            }
            cardView.setLayoutParams(lp);

            ((ImageView) cardView.findViewById(R.id.img_tipo_selector))
                    .setImageResource(iconoParaTipo(tipo));
            ((TextView) cardView.findViewById(R.id.tv_nombre_tipo))
                    .setText(nombreLeg);

            CardView card = cardView.findViewById(R.id.card_tipo_hab);
            cardView.setOnClickListener(v -> seleccionarTipo(card, tipo, nombreLeg));

            fila.addView(cardView);
        }

        // Rellenar celdas vacías en la última fila si TIPOS.length no es múltiplo de COLUMNAS
        if (fila != null) {
            int resto = TIPOS.length % COLUMNAS;
            if (resto != 0) {
                for (int k = resto; k < COLUMNAS; k++) {
                    View ph = new View(requireContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
                    lp.leftMargin = gap / 2;
                    fila.addView(ph, lp);
                }
            }
        }
    }

    /* ════════════════════════════════════════════════════
       SELECCIÓN DE TIPO
    ════════════════════════════════════════════════════ */

    private void seleccionarTipo(CardView card, String tipo, String nombreLegible) {
        // Deseleccionar tarjeta anterior
        if (cardSeleccionada != null) {
            cardSeleccionada.setCardBackgroundColor(
                    getResources().getColor(R.color.surface, null));

        }

        // Resaltar nueva tarjeta
        card.setCardBackgroundColor(
                getResources().getColor(R.color.accent_background, null));

        cardSeleccionada  = card;
        tipoSeleccionado  = tipo;

        // Mostrar campo nombre con el nombre legible pre-rellenado
        tilNombre.setVisibility(View.VISIBLE);
        if (etNombre.getText() == null || etNombre.getText().toString().trim().isEmpty()) {
            etNombre.setText(nombreLegible);
        }
        etNombre.setSelection(etNombre.getText() != null ? etNombre.getText().length() : 0);

        btnAnadir.setEnabled(
                etNombre.getText() != null && !etNombre.getText().toString().trim().isEmpty());
    }

    /* ════════════════════════════════════════════════════
       ENVÍO AL BACKEND
    ════════════════════════════════════════════════════ */

    private void enviarHabitacion() {
        if (tipoSeleccionado == null || idHogar == -1) return;

        String nombre = etNombre.getText() != null
                ? etNombre.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            tilNombre.setError("Introduce un nombre");
            return;
        }
        tilNombre.setError(null);

        btnAnadir.setEnabled(false);
        btnAnadir.setText("Añadiendo…");

        try {
            JSONObject body = new JSONObject();
            body.put("nombre",    nombre);
            body.put("tipo",      tipoSeleccionado);
            body.put("id_hogar",  idHogar);

            JsonObjectRequest peticion = new JsonObjectRequest(
                    Request.Method.POST, WebService.URL_Habitacion, body,
                    response -> {
                        try {
                            if (response.getString(WebService.JSON.STATUS)
                                    .equals(WebService.JSON.SUCCESS)) {
                                Toast.makeText(requireContext(),
                                        "Habitación añadida", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onHabitacionAnadida();
                                dismiss();
                            } else {
                                mostrarErrorEnBoton("No se pudo añadir la habitación");
                            }
                        } catch (JSONException e) {
                            mostrarErrorEnBoton("Error al procesar la respuesta");
                        }
                    },
                    error -> {
                        Utilidades.mostrar_error_peticion(requireContext(), TAG,
                                "Error al añadir la habitación",
                                Request.Method.POST, WebService.URL_Habitacion, error);
                        mostrarErrorEnBoton("Error de red");
                    }
            );

            PeticionesRed.anhadirPeticionACola(peticion);

        } catch (JSONException e) {
            mostrarErrorEnBoton("Error interno");
        }
    }

    private void mostrarErrorEnBoton(String msg) {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            btnAnadir.setEnabled(true);
            btnAnadir.setText("Añadir habitación");
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    /* ════════════════════════════════════════════════════
       HELPERS
    ════════════════════════════════════════════════════ */

    private int iconoParaTipo(String tipo) {
        if (tipo == null) return R.drawable.ic_home2;
        switch (tipo.toLowerCase(Locale.getDefault())) {
            case "cocina":     return R.drawable.ic_cocina;
            case "aseo":       return R.drawable.outline_bathroom_24;
            case "garaje":     return R.drawable.outline_garage_24;
            case "exterior":   return R.drawable.outline_outdoor_garden_24;
            case "dormitorio": return R.drawable.ic_dormitorio;
            case "infantil":   return R.drawable.outline_bedroom_baby_24;
            case "comedor":    return R.drawable.ic_comedor;
            case "salon":      return R.drawable.outline_chair_24;
            case "oficina":    return R.drawable.outline_add_home_work_24;
            case "trastero":   return R.drawable.outline_inventory_2_24;
            case "recibidor":  return R.drawable.outline_meeting_room_24;
            case "terraza":    return R.drawable.outline_balcony_24;
            case "deportiva":  return R.drawable.outline_fitness_center_24;
            case "generica":   return R.drawable.outline_nest_multi_room_24;
            default:           return R.drawable.ic_home2;
        }
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
}
