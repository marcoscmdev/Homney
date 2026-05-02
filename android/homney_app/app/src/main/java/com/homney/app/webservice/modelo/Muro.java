package com.homney.app.webservice.modelo;

public class Muro {
    /*
    SELECT m.id_pub, m.titulo, m.cuerpo, m.id_usuario, m.fecha_pub
    FROM MURO m
    -- fecha_pub tiene DEFAULT CURRENT_TIMESTAMP, no se envía en el INSERT
     */

    private int id_pub;
    private String titulo;
    private String cuerpo;
    private int id_usuario;
    private String fecha_pub; // generada automáticamente por el servidor (CURRENT_TIMESTAMP)
    private String imagen;    // ruta relativa devuelta por subir_imagen.php (nullable)

    /* El constructor para GSON no es necesario */
    public Muro(String titulo, String cuerpo, int id_usuario) {
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.id_usuario = id_usuario;
    }

    public int getId_pub() { return id_pub; }
    public void setId_pub(int id_pub) { this.id_pub = id_pub; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }

    public int getId_usuario() { return id_usuario; }
    public void setId_usuario(int id_usuario) { this.id_usuario = id_usuario; }

    public String getFecha_pub() { return fecha_pub; }
    public void setFecha_pub(String fecha_pub) { this.fecha_pub = fecha_pub; }

    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
