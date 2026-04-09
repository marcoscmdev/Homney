package com.homney.app.webservice.modelo;

public class TareasRealizadas {
    /*
    SELECT tr.id_tarea, tr.id_usuario, tr.fecha_realizacion,
           tr.observaciones, tr.duracion_real
    FROM TAREAS_REALIZADAS tr
    -- PK compuesta: id_tarea + id_usuario + fecha_realizacion (sin autoincrement)
     */

    private int id_tarea;
    private int id_usuario;
    private String fecha_realizacion;
    private String observaciones; // nullable (opcional en INSERT)
    private String duracion_real; // nullable (opcional en INSERT)

    /* El constructor para GSON no es necesario */
    public TareasRealizadas(int id_tarea, int id_usuario, String fecha_realizacion,
                            String observaciones, String duracion_real) {
        this.id_tarea = id_tarea;
        this.id_usuario = id_usuario;
        this.fecha_realizacion = fecha_realizacion;
        this.observaciones = observaciones;
        this.duracion_real = duracion_real;
    }

    public int getId_tarea() { return id_tarea; }
    public void setId_tarea(int id_tarea) { this.id_tarea = id_tarea; }

    public int getId_usuario() { return id_usuario; }
    public void setId_usuario(int id_usuario) { this.id_usuario = id_usuario; }

    public String getFecha_realizacion() { return fecha_realizacion; }
    public void setFecha_realizacion(String fecha_realizacion) { this.fecha_realizacion = fecha_realizacion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getDuracion_real() { return duracion_real; }
    public void setDuracion_real(String duracion_real) { this.duracion_real = duracion_real; }
}
