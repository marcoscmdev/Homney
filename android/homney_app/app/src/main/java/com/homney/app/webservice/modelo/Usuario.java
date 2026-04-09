package com.homney.app.webservice.modelo;

public class Usuario {
    /*
    SELECT u.id_usuario, u.nombre, u.email, u.telefono_movil,
           u.fecha_nacimiento, u.sexo, u.avatar,
           u.id_hogar, u.rol, u.fecha_registro
    FROM USUARIO u
    -- NOTA: la clave (hash) NO se devuelve en el GET
    -- fecha_nacimiento y avatar son opcionales en el INSERT
     */

    private int id_usuario;
    private String nombre;
    private String email;
    private String telefono_movil;
    private String clave;            // solo se envía en INSERT/UPDATE, nunca se recibe en GET
    private String fecha_nacimiento; // nullable (opcional en INSERT)
    private String sexo;
    private String avatar;           // opcional en INSERT (el servidor usa default.png si es null)
    private String rol;
    private int id_hogar;
    private String fecha_registro;   // generada automáticamente por el servidor, solo se recibe en GET

    /* El constructor para GSON no es necesario */
    public Usuario(String nombre, String email, String telefono_movil, String clave,
                   String fecha_nacimiento, String sexo, String avatar, String rol, int id_hogar) {
        this.nombre = nombre;
        this.email = email;
        this.telefono_movil = telefono_movil;
        this.clave = clave;
        this.fecha_nacimiento = fecha_nacimiento;
        this.sexo = sexo;
        this.avatar = avatar;
        this.rol = rol;
        this.id_hogar = id_hogar;
    }

    public int getId_usuario() { return id_usuario; }
    public void setId_usuario(int id_usuario) { this.id_usuario = id_usuario; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono_movil() { return telefono_movil; }
    public void setTelefono_movil(String telefono_movil) { this.telefono_movil = telefono_movil; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public String getFecha_nacimiento() { return fecha_nacimiento; }
    public void setFecha_nacimiento(String fecha_nacimiento) { this.fecha_nacimiento = fecha_nacimiento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public int getId_hogar() { return id_hogar; }
    public void setId_hogar(int id_hogar) { this.id_hogar = id_hogar; }

    public String getFecha_registro() { return fecha_registro; }
    public void setFecha_registro(String fecha_registro) { this.fecha_registro = fecha_registro; }
}
