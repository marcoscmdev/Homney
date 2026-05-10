<?php
// En todos lo ficheros PHP, es importante NO poner espacios ni líneas en blanco antes de <?php.
// Generaría un error al usar la función header (las cabeceras HTTP deben enviarse antes de cualquier contenido).

/* ============================================================
 *  INSTRUCCIONES DE CONFIGURACIÓN
 * ============================================================
 *  1. Copia este fichero como  conexion.php  (en la misma carpeta /backend)
 *  2. Rellena las constantes de conexión con tus credenciales reales
 *  3. NUNCA subas conexion.php a Git  (ya está en .gitignore)
 *
 *  Formato de respuesta JSend: https://github.com/omniti-labs/jsend
 * ============================================================ */

// ── Constantes de respuesta JSON (JSend) ─────────────────────
define('STATUS',  'status');   // 'success' | 'fail' | 'error'
define('FAIL',    'fail');     // Petición no satisfecha → mensaje en DATA
define('SUCCESS', 'success');  // Todo OK
define('DATA',    'data');     // Payload en FAIL y SUCCESS
define('SINDATOS', null);
define('ERROR',   'error');    // Error grave de servidor
define('MESSAGE', 'message');  // Mensaje descriptivo en ERROR
define('CODE',    'code');     // Código HTTP opcional
define('ERROR_EN_SINTAXIS_PETICION', 400);

// ── Suprime warnings en la salida JSON ───────────────────────
ini_set('display_errors', '0');
ini_set('display_startup_errors', '0');
error_reporting(E_ALL & ~E_DEPRECATED & ~E_NOTICE & ~E_WARNING);

// ── Funciones de error de uso habitual ───────────────────────

function die_por_fallo_en_sintaxis_peticion() {
    header("Content-type: application/json");
    $respuesta[STATUS] = FAIL;
    $respuesta[DATA]   = 'Sintaxis petición errónea: '
                        . $_SERVER['QUERY_STRING']
                        . file_get_contents('php://input');
    die(json_encode($respuesta));
}

function die_por_fallo_en_consulta($consultaSQL, $conexionMySQL) {
    header("Content-type: application/json");
    $respuesta[STATUS] = FAIL;
    $respuesta[DATA]   = 'SQL: ' . $consultaSQL
                        . ' Causa: ' . mysqli_error($conexionMySQL);
    die(json_encode($respuesta));
}

// ── Funciones de transformación MySQL → JSON ─────────────────

function intaboolean($x) {
    return $x ? true : false;
}

function fechaHoraJSON($fechaMySQL) {
    $dt = new DateTime($fechaMySQL);
    $dt->setTimezone(new DateTimeZone('UTC'));
    return $dt->format('Y-m-d\TH:i:s\Z');
}

function ajustaColumnasFormatoJSON(mysqli_result $resultado, array &$fila): void
{
    $fila   = array_change_key_case($fila, CASE_LOWER);
    $campos = $resultado->fetch_fields();
    $i = 0;

    foreach ($fila as $columna => $dato) {
        $tipo = $campos[$i]->type;

        if ($dato === null) { $i++; continue; }

        switch ($tipo) {
            case MYSQLI_TYPE_DATE:
            case MYSQLI_TYPE_DATETIME:
            case MYSQLI_TYPE_TIMESTAMP:
                $fila[$columna] = fechaHoraJSON($dato);
                break;
            case MYSQLI_TYPE_TINY:
                $fila[$columna] = ($dato === '0' || $dato === '1')
                    ? (bool)$dato : (int)$dato;
                break;
            case MYSQLI_TYPE_SHORT:
            case MYSQLI_TYPE_LONG:
                $fila[$columna] = (int)$dato;
                break;
            case MYSQLI_TYPE_LONGLONG:
                $fila[$columna] = (string)$dato;
                break;
            case MYSQLI_TYPE_FLOAT:
            case MYSQLI_TYPE_DOUBLE:
                $fila[$columna] = (float)$dato;
                break;
            case MYSQLI_TYPE_DECIMAL:
            case MYSQLI_TYPE_NEWDECIMAL:
                $fila[$columna] = (string)$dato;
                break;
            default:
                $fila[$columna] = (string)$dato;
        }
        $i++;
    }
}

// ── Conexión con MySQL ────────────────────────────────────────
//
//  Cambia los valores entre comillas por tus credenciales reales.
//  En producción se recomienda leerlos desde variables de entorno
//  o un fichero .env fuera del directorio público.
//
if ($_SERVER['HTTP_HOST'] === 'localhost' || $_SERVER['HTTP_HOST'] === '127.0.0.1') {
    // ── Entorno LOCAL ─────────────────────────────────────────
    define('DB_HOST', 'localhost');
    define('DB_NAME', 'nombre_bbdd_local');   // ← tu BD local
    define('DB_USER', 'usuario_local');        // ← tu usuario MySQL local
    define('DB_PASS', 'contraseña_local');     // ← tu contraseña local
} else {
    // ── Entorno PRODUCCIÓN ────────────────────────────────────
    define('DB_HOST', 'host_produccion');      // ej: fdb34.awardspace.net
    define('DB_NAME', 'nombre_bbdd_prod');     // ← tu BD de producción
    define('DB_USER', 'usuario_prod');         // ← tu usuario de producción
    define('DB_PASS', 'contraseña_prod');      // ← tu contraseña de producción
}

mysqli_report(MYSQLI_REPORT_OFF);

$conexion = @mysqli_connect(DB_HOST, DB_USER, DB_PASS, DB_NAME);
if (!$conexion) {
    header("Content-type: application/json");
    $respuesta[STATUS]  = ERROR;
    $respuesta[MESSAGE] = 'Error de conexión con servidor MySQL: '
                         . utf8_encode(@mysqli_connect_error());
    die(json_encode($respuesta));
} else {
    mysqli_set_charset($conexion, "utf8");
}

// ── Sanitización de datos de entrada ─────────────────────────

function sanarDatos($conexion, $data) {
    if (is_object($data)) {
        $data = (array)$data;
        foreach ($data as $key => $value) {
            $data[$key] = sanarDatos($conexion, $value);
        }
        return (object)$data;
    } elseif (is_array($data)) {
        foreach ($data as $key => $value) {
            $data[$key] = sanarDatos($conexion, $value);
        }
        return $data;
    } else {
        if ($data === null)  return null;
        if ($data === false) return '0';
        return mysqli_real_escape_string($conexion, $data);
    }
}
