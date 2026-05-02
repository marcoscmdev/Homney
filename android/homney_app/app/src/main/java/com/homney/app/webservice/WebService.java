package com.homney.app.webservice;


import android.os.Build;

public class WebService {
    public static final String PROTOCOLO = "http://";
    public static final String CARPETA ="/homney/backend";

    public static final String SERVIDOR = enLocal() ? "10.0.2.2" : "192.168.1.77";

    private static boolean enLocal() {
        return Build.FINGERPRINT.startsWith("generic");
    }

    // 10.0.2.2 = localhost del anfitrión en emulador
    // 192.168.1.77 = IP fija del Mac en red local
    public static final String URL_Asignacion_Tarea =PROTOCOLO + SERVIDOR + CARPETA + "/asignacion_tarea.php";
    public static final String URL_Categoria =PROTOCOLO + SERVIDOR + CARPETA + "/categoria.php";
    public static final String URL_Gasto =PROTOCOLO + SERVIDOR + CARPETA + "/gasto.php";
    public static final String URL_Habitacion =PROTOCOLO + SERVIDOR + CARPETA + "/habitacion.php";
    public static final String URL_Hogar =PROTOCOLO + SERVIDOR + CARPETA + "/hogar.php";
    public static final String URL_Muro =PROTOCOLO + SERVIDOR + CARPETA + "/muro.php";
    public static final String URL_RepartoGasto =PROTOCOLO + SERVIDOR + CARPETA + "/reparto_gasto.php";
    public static final String URL_Tarea =PROTOCOLO + SERVIDOR + CARPETA + "/tarea.php";
    public static final String URL_Tarea_Realizada =PROTOCOLO + SERVIDOR + CARPETA + "/tareas_realizadas.php";
    public static final String URL_Usuario =PROTOCOLO + SERVIDOR + CARPETA + "/usuario.php";
    public static final String URL_Login   =PROTOCOLO + SERVIDOR + CARPETA + "/login.php";
    public static final String URL_EjemploImagen ="https://tesdai.com/imagenes/logoTESDAI.png";

    public final static class JSON {
        // Constantes para mensajes json. Formato: https://github.com/omniti-labs/jsend
        public final static String STATUS = "status"; // Puede ser: ERROR, FAIL, SUCCESS
        public final static String ERROR = "error"; // Error grave al intentar procesar la petición.
        public final static String MESSAGE = "message"; // Mensaje de error.
        public final static String CODE = "code"; // Código de error (opcional).
        public final static int ERROR_EN_SINTAXIS_PETICION = 400;

        public final static String FAIL = "fail"; // No se ha podido satisfacer la petición, mensaje en DATA.
        public final static String SUCCESS = "success"; // Todo OK
        public final static String DATA = "data"; // Se envían datos de respuesta con FAIL Y SUCCESS.
        public final static String SINDATOS = null;

    }
}
