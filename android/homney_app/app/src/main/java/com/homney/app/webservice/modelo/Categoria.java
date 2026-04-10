package com.homney.app.webservice.modelo;

public class Categoria {
    /*
    SELECT c.nombre, c.categ_padre, c.descripcion
    FROM CATEGORIA c
    -- PK: nombre (String, sin autoincrement)
    -- categ_padre es FK auto-referenciada y nullable
     */

    private String nombre;       // PK (String, no numérica)
    private String categ_padre;  // nullable (referencia a otra Categoria)
    private String descripcion;

    /* El constructor para GSON no es necesario */
    public Categoria(String nombre, String descripcion, String categ_padre) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categ_padre = categ_padre;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCateg_padre() { return categ_padre; }
    public void setCateg_padre(String categ_padre) { this.categ_padre = categ_padre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

}
