package com.homney.app.ui.fragmento5_muro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.homney.app.R;
import com.homney.app.webservice.WebService;
import com.homney.app.webservice.modelo.Muro;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RvMuroAdapter extends RecyclerView.Adapter<RvMuroAdapter.MuroViewHolder> {

    /** Callback para eliminar una publicación. */
    public interface OnDeleteListener {
        void onDelete(int idPub, int position);
    }

    private final List<Muro>           publicacionesList;
    /** id_usuario → nombre del autor */
    private final Map<Integer, String> nombresPorUsuario;
    /** id del usuario en sesión — para mostrar la papelera solo en sus publicaciones */
    private final int                  idUsuarioActual;
    private final OnDeleteListener     deleteListener;

    private static final SimpleDateFormat FMT_ENTRADA =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat FMT_SALIDA  =
            new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault());

    public RvMuroAdapter(List<Muro> publicacionesList,
                         Map<Integer, String> nombresPorUsuario,
                         int idUsuarioActual,
                         OnDeleteListener deleteListener) {
        this.publicacionesList  = publicacionesList;
        this.nombresPorUsuario  = nombresPorUsuario;
        this.idUsuarioActual    = idUsuarioActual;
        this.deleteListener     = deleteListener;
    }

    @NonNull
    @Override
    public MuroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_layout_muro, parent, false);
        return new MuroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MuroViewHolder holder, int position) {
        Muro pub = publicacionesList.get(position);

        holder.titulo_publi_muro.setText(pub.getTitulo());
        holder.body_muro.setText(pub.getCuerpo());

        // Nombre del autor (fallback al id si no está en el mapa)
        String autor = nombresPorUsuario.containsKey(pub.getId_usuario())
                ? nombresPorUsuario.get(pub.getId_usuario())
                : "Usuario #" + pub.getId_usuario();

        // Fecha formateada
        String fechaTexto = pub.getFecha_pub() != null
                ? formatearFecha(pub.getFecha_pub())
                : "";

        holder.fecha_nombre.setText(fechaTexto + "  ·  " + autor);

        // Imagen de la publicación — se sirve a través del proxy PHP para evitar
        // el bloqueo de ficheros estáticos que aplica AwardSpace en uploads/
        String rutaImagen = pub.getImagen();
        if (rutaImagen != null && !rutaImagen.isEmpty()) {
            String urlCompleta = WebService.urlImagen(rutaImagen, 0);
            holder.img_muro_publi.setVisibility(View.VISIBLE);
            Glide.with(holder.img_muro_publi.getContext())
                    .load(urlCompleta)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.img_muro_publi);
        } else {
            holder.img_muro_publi.setVisibility(View.GONE);
        }

        // imagen_publi_muro (el otro ImageView del layout) permanece oculto
        holder.imagen_publi_muro.setVisibility(View.GONE);

        // Papelera: visible solo si la publicación es del usuario en sesión
        if (pub.getId_usuario() == idUsuarioActual && deleteListener != null) {
            holder.btn_eliminar_publi.setVisibility(View.VISIBLE);
            holder.btn_eliminar_publi.setOnClickListener(v ->
                    deleteListener.onDelete(pub.getId_pub(), holder.getAdapterPosition()));
        } else {
            holder.btn_eliminar_publi.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return publicacionesList != null ? publicacionesList.size() : 0;
    }

    /** Convierte "yyyy-MM-dd HH:mm:ss" → "dd/MM/yyyy · HH:mm" */
    private String formatearFecha(String raw) {
        try {
            Date d = FMT_ENTRADA.parse(raw);
            return d != null ? FMT_SALIDA.format(d) : raw;
        } catch (ParseException e) {
            return raw; // si no puede parsear, muestra el texto tal cual
        }
    }

    /* ── ViewHolder ─────────────────────────────────────── */
    public static class MuroViewHolder extends RecyclerView.ViewHolder {
        TextView  titulo_publi_muro, fecha_nombre, body_muro;
        ImageView imagen_publi_muro;   // ImageView legacy (oculto)
        ImageView img_muro_publi;      // ImageView principal para la imagen de la publicación
        ImageView btn_eliminar_publi;  // Papelera (visible solo para publicaciones propias)

        public MuroViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo_publi_muro  = itemView.findViewById(R.id.titulo_publi_muro);
            fecha_nombre       = itemView.findViewById(R.id.fecha_nombre);
            body_muro          = itemView.findViewById(R.id.body_muro);
            imagen_publi_muro  = itemView.findViewById(R.id.imagen_publi_muro);
            img_muro_publi     = itemView.findViewById(R.id.img_muro_publi);
            btn_eliminar_publi = itemView.findViewById(R.id.btn_eliminar_publi);
        }
    }
}
