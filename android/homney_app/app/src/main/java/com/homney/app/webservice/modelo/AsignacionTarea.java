package com.homney.app.webservice.modelo;

public class AsignacionTarea {
    /*
    SELECT at.id_tarea, at.id_usuario, at.observaciones
    FROM ASIGNACION_TAREA at
    -- PK compuesta: id_tarea + id_usuario (sin autoincrement)
     */

    private int id_tarea;
    private int id_usuario;
    private String observaciones; // nullable (opcional en INSERT)

    /* El constructor para GSON no es necesario */
    public AsignacionTarea(int id_tarea, int id_usuario, String observaciones) {
        this.id_tarea = id_tarea;
        this.id_usuario = id_usuario;
        this.observaciones = observaciones;
    }

    public int getId_tarea() { return id_tarea; }
    public void setId_tarea(int id_tarea) { this.id_tarea = id_tarea; }

    public int getId_usuario() { return id_usuario; }
    public void setId_usuario(int id_usuario) { this.id_usuario = id_usuario; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
