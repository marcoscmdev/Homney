package com.homney.app.webservice.modelo;

public class RepartoGasto {
    /*
    SELECT rg.id_gasto, rg.id_usuario, rg.pagador, rg.importe, rg.abonado
    FROM REPARTO_GASTO rg
    -- PK compuesta: id_gasto + id_usuario (sin autoincrement)
    -- pagador: 1 = es el pagador principal, 0 = no lo es
    -- abonado: 1 = ha abonado su parte, 0 = pendiente de abono
     */

    private int id_gasto;
    private int id_usuario;
    private int pagador;  // 0 = no pagador, 1 = pagador principal
    private double importe;
    private int abonado;  // 0 = pendiente, 1 = abonado

    /* El constructor para GSON no es necesario */
    public RepartoGasto(int id_gasto, int id_usuario, int pagador, double importe, int abonado) {
        this.id_gasto = id_gasto;
        this.id_usuario = id_usuario;
        this.pagador = pagador;
        this.importe = importe;
        this.abonado = abonado;
    }

    public int getId_gasto() { return id_gasto; }
    public void setId_gasto(int id_gasto) { this.id_gasto = id_gasto; }

    public int getId_usuario() { return id_usuario; }
    public void setId_usuario(int id_usuario) { this.id_usuario = id_usuario; }

    public int getPagador() { return pagador; }
    public void setPagador(int pagador) { this.pagador = pagador; }

    public double getImporte() { return importe; }
    public void setImporte(double importe) { this.importe = importe; }

    public int getAbonado() { return abonado; }
    public void setAbonado(int abonado) { this.abonado = abonado; }
}
