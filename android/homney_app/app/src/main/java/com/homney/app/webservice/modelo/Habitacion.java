package com.homney.app.webservice.modelo;

public class Habitacion {
    /*
    SELECT h.id_habitacion, h.nombre, h.tipo, h.id_hogar
    FROM HABITACION h
     */

    private int id_habitacion;
    private String nombre;
    private String tipo;
    private int id_hogar;

    /* El constructor para GSON no es necesario */
    public Habitacion(String nombre, String tipo, int id_hogar) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.id_hogar = id_hogar;
    }

    public int getId_habitacion() { return id_habitacion; }
    public void setId_habitacion(int id_habitacion) { this.id_habitacion = id_habitacion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getId_hogar() { return id_hogar; }
    public void setId_hogar(int id_hogar) { this.id_hogar = id_hogar; }
}
