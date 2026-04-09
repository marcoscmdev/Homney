package com.homney.app.webservice.modelo;

public class Gasto {
    /*
    SELECT g.id_gasto, g.fecha, g.categoria, g.concepto,
           g.modo, g.tipo, g.importe, g.id_hogar, g.id_usuario_pagador
    FROM GASTO g
     */

    private int id_gasto;
    private String fecha;
    private String categoria;
    private String concepto;
    private String modo;
    private String tipo;
    private double importe; // double para soportar decimales (ej: 12.50 €)
    private int id_hogar;
    private int id_usuario_pagador;

    /* El constructor para GSON no es necesario */
    public Gasto(String fecha, String categoria, String concepto, String modo, String tipo,
                 double importe, int id_hogar, int id_usuario_pagador) {
        this.fecha = fecha;
        this.categoria = categoria;
        this.concepto = concepto;
        this.modo = modo;
        this.tipo = tipo;
        this.importe = importe;
        this.id_hogar = id_hogar;
        this.id_usuario_pagador = id_usuario_pagador;
    }

    public int getId_gasto() { return id_gasto; }
    public void setId_gasto(int id_gasto) { this.id_gasto = id_gasto; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public double getImporte() { return importe; }
    public void setImporte(double importe) { this.importe = importe; }

    public int getId_hogar() { return id_hogar; }
    public void setId_hogar(int id_hogar) { this.id_hogar = id_hogar; }

    public int getId_usuario_pagador() { return id_usuario_pagador; }
    public void setId_usuario_pagador(int id_usuario_pagador) { this.id_usuario_pagador = id_usuario_pagador; }
}
