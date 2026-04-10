package com.homney.app.webservice.respuestas;

import java.util.List;

/**
 *
 * El servidor devuelve siempre:
 *   { "status": "success", "data": [ {...}, {...} ] }
 *   { "status": "success", "data": null }   (sin resultados)
 *
 * Uso con GSON + TypeToken:
 *   Type tipo = new TypeToken<RespuestaLista<Usuario>>(){}.getType();
 *   RespuestaLista<Usuario> respuesta = new Gson().fromJson(json, tipo);
 *
 *   List<Usuario> lista = respuesta.data;  // puede ser null si no hay datos
 */
public class RespuestaLista<T> {
    public String status;
    public List<T> data;

    @Override
    public String toString() {
        return "RespuestaLista{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }
}
