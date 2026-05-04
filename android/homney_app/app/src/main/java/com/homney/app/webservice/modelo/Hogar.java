package com.homney.app.webservice.modelo;

public class Hogar {
    /*
    SELECT h.id_hogar, h.clave_inv
    FROM HOGAR h
     */

    private int id_hogar;
    private String clave_inv;
    private String nombre;

    /* El constructor para GSON no es necesario */
    public Hogar(String clave_inv) {
        this.clave_inv = clave_inv;
    }

    public int getId_hogar() { return id_hogar; }
    public void setId_hogar(int id_hogar) { this.id_hogar = id_hogar; }

    public String getClave_inv() { return clave_inv; }
    public void setClave_inv(String clave_inv) { this.clave_inv = clave_inv; }

    public String getNombre() {return nombre;}


}
