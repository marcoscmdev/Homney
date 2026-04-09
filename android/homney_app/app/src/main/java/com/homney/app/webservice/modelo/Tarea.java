package com.homney.app.webservice.modelo;

public class Tarea {
    /*
    SELECT t.id_tarea, t.nombre, t.duracion, t.frecuencia,
           t.num_veces, t.explicacion_frecuencia_variable, t.id_habitacion
    FROM TAREA t
     */

    private int id_tarea;
    private String nombre;
    private Integer duracion;                       // nullable (opcional en INSERT)
    private String frecuencia;
    private String num_veces;
    private String explicacion_frecuencia_variable; // nullable (opcional en INSERT)
    private Integer id_habitacion;                  // nullable (opcional en INSERT)

    /* El constructor para GSON no es necesario */
    public Tarea(String nombre, String frecuencia, String num_veces,
                 Integer duracion, String explicacion_frecuencia_variable, Integer id_habitacion) {
        this.nombre = nombre;
        this.frecuencia = frecuencia;
        this.num_veces = num_veces;
        this.duracion = duracion;
        this.explicacion_frecuencia_variable = explicacion_frecuencia_variable;
        this.id_habitacion = id_habitacion;
    }

    public int getId_tarea() { return id_tarea; }
    public void setId_tarea(int id_tarea) { this.id_tarea = id_tarea; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getDuracion() { return duracion; }
    public void setDuracion(Integer duracion) { this.duracion = duracion; }

    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }

    public String getNum_veces() { return num_veces; }
    public void setNum_veces(String num_veces) { this.num_veces = num_veces; }

    public String getExplicacion_frecuencia_variable() { return explicacion_frecuencia_variable; }
    public void setExplicacion_frecuencia_variable(String explicacion_frecuencia_variable) {
        this.explicacion_frecuencia_variable = explicacion_frecuencia_variable;
    }

    public Integer getId_habitacion() { return id_habitacion; }
    public void setId_habitacion(Integer id_habitacion) { this.id_habitacion = id_habitacion; }
}
