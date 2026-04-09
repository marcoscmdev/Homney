package com.homney.app.webservice;


public class WebService {
    public static final String PROTOCOLO = "http://";
    public static final String CARPETA ="/web_service";
    public static final String SERVIDOR = "10.0.2.2"; // Localhsot en anfitrión del Emulador
    public static final String URL_Departamentos =PROTOCOLO + SERVIDOR + CARPETA + "/departamentos.php";
    public static final String URL_Usuarios =PROTOCOLO + SERVIDOR + CARPETA + "/usuarios.php";
    public static final String URL_EjemploImagen ="https://tesdai.com/imagenes/logoTESDAI.png";
    public static final String URL_Datos =  PROTOCOLO + SERVIDOR +  CARPETA + "/datos.php";

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
