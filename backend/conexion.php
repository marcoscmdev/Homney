<?php 
// En todos lo ficheros PHP, es importante NO poner espacios ni líneas en blanco antes de <?php. Generaría un error al usar la función header.(las cabeceras HTTP deben enviarse al navegador/cliente antes de cualquier contenido).
/* Nota: Para probar el WebService se puede utilizar la extensión del navegador Chrome: Advanced REST client o POSTMAN
	 // Constantes para mensajes json. Se sigue el formato: https://github.com/omniti-labs/jsend
	 // Ejemplos:
	{
		status : "success",
		data : {
			"post" : { "id" : 1, "title" : "A blog post", "body" : "Some useful content" }
		 }
	}

	{
		"status" : "fail",
		"data" : { "title" : "A title is required" }
	}

	{
		"status" : "error",
		"message" : "Unable to communicate with database"
	}

	Nota 1: Se genera la salida JSON usando la función json_encode: http://php.net/manual/es/function.json-encode.php
	Nota 2: Se genera la salida JSON usando la función json_decode: http://php.net/manual/es/function.json-decode.php
*/

define('STATUS','status');  // El valor de 'status' puede ser: error, fail (fallo) o SUCCESS (éxito) 
define('FAIL','fail'); // No se ha podido satisfacer la petición, mensaje en DATA.
define('SUCCESS','success'); // Todo OK
		define('DATA','data'); // Se envían datos de respuesta con FAIL Y SUCCESS.
				define('SINDATOS',null);
define('ERROR','error'); // Error grave del servidor al intentar procesar la petición.
		   define('MESSAGE','message'); // Mensaje en caso de ERROR.
		   define('CODE','code'); // Código de error HTTP (opcional).
					define('ERROR_EN_SINTAXIS_PETICION',400); // Código de ejemplo

// Evitar que se impriman warnings/deprecations como HTML en la salida JSON
ini_set('display_errors', '0');
ini_set('display_startup_errors', '0');
error_reporting(E_ALL & ~E_DEPRECATED & ~E_NOTICE & ~E_WARNING);

 /**** Functiones de error uso habitual ****/
 function die_por_fallo_en_sintaxis_peticion() { // Error en los parámetros.
	  header("Content-type: application/json");
	  $respuesta[STATUS]=FAIL;									// url                 // JSON
	  $respuesta[DATA]='Sintaxis peticion errónea: ' . $_SERVER['QUERY_STRING'] . file_get_contents('php://input'); 
	//  http_response_code(ERROR_EN_SINTAXIS_PETICION);
	  die (json_encode($respuesta));
 }
 
 function die_por_fallo_en_consulta($consultaSQL,$conexionMySQL){
	  header("Content-type: application/json");
	  $respuesta[STATUS]=FAIL;
	  $respuesta[DATA]='SQL: ' . $consultaSQL . ' Causa: ' . mysqli_error($conexionMySQL);
	  die (json_encode($respuesta));
 }
 
 /** funciones de transformación de tipos de datos MySQL a formato JSON **/
 
 function intaboolean($x){
	 if ($x) return true;
	 else return false;
 }
 
 function fechaHoraJSON($fechaMySQL){
	 $dt = new DateTime($fechaMySQL);
	 $dt->setTimezone(new DateTimeZone('UTC'));

	 return $dt->format('Y-m-d\TH:i:s\Z');
 } 

 
function ajustaColumnasFormatoJSON(mysqli_result $resultado, array &$fila): void
{
    $fila = array_change_key_case($fila, CASE_LOWER);

    $campos = $resultado->fetch_fields();

    $i = 0;
    foreach ($fila as $columna => $dato) {
        $tipo = $campos[$i]->type;

        if ($dato === null) {
            $fila[$columna] = null;
            $i++;
            continue;
        }

        switch ($tipo) {

            // DATE, DATETIME, TIMESTAMP
            case MYSQLI_TYPE_DATE:      // 10
            case MYSQLI_TYPE_DATETIME:  // 12
            case MYSQLI_TYPE_TIMESTAMP: // 7
                $fila[$columna] = fechaHoraJSON($dato);
                break;

            // BOOLEAN (TINYINT(1))
            case MYSQLI_TYPE_TINY: // 1
                // Solo tratar como boolean si realmente es 0 o 1
                $fila[$columna] = ($dato === '0' || $dato === '1')
                    ? (bool)$dato
                    : (int)$dato;
                break;

            // INT, SMALLINT, BIGINT
			case MYSQLI_TYPE_SHORT: // SMALLINT
			case MYSQLI_TYPE_LONG:  // INT
				$fila[$columna] = (int)$dato;
				break;
			case MYSQLI_TYPE_LONGLONG: // BIGINT
				$fila[$columna] = (string)$dato;
				break;

            // FLOAT, DOUBLE
            case MYSQLI_TYPE_FLOAT:  // 4
            case MYSQLI_TYPE_DOUBLE: // 5
                $fila[$columna] = (float)$dato;
                break;

            // DECIMAL, NUMERIC → STRING (sin pérdida de precisión)
            case MYSQLI_TYPE_DECIMAL:
            case MYSQLI_TYPE_NEWDECIMAL:
                    $fila[$columna] = (string)$dato;
                break;

            // TODO lo demás → string
            default:
                $fila[$columna] = (string)$dato;
        }

        $i++;
    }
}


 /**** Conexión con el servidor MySQL ****/

 $servidor="localhost"; // Ordenador donde reside el servidor MySQL.
 $usuario="root"; // Usuario de MySQL
 $clave="root"; // Contraseña del usuario MySQL
 $base_de_datos="homney_dev"; // Base de datos del $servidor al que queremos conectarnos

 mysqli_report(MYSQLI_REPORT_OFF); // Sin excepciones: controlamos errores manualmente con if(!$resultado) die_por_fallo_en_consulta(...)

 $conexion=@mysqli_connect ($servidor, $usuario, $clave, $base_de_datos); //@ para evitar que se emita el error.
 if (!$conexion)  {
	  header("Content-type: application/json");  // Le indicamos al cliente web que el contenido enviado está en formtato JSON.(Esto asegura una correcta interpretación del contenido).
	  $respuesta[STATUS]=ERROR;
	  $causa_error=@mysqli_connect_error();
	  $respuesta[MESSAGE]='Error de conexión con servidor MySQL, '. utf8_encode($causa_error);
	  die (json_encode($respuesta));
 }
 else {
	 mysqli_set_charset($conexion,"utf8"); // Le indicamos a MySQL codificación a utilizar en el intercambio de datos: UTF8
 }


 function sanarDatos($conexion,$data) {
	
    if (is_object($data)) {
        // Si es un objeto, convertimos a array y aplicamos la sanitización de manera recursiva
        $data = (array)$data;
        foreach ($data as $key => $value) {
            $data[$key] = sanarDatos($conexion,$value);
        }	
        return (object)$data;
    } elseif (is_array($data)) {
        // Si es un array, aplicamos la sanitización de manera recursiva
        foreach ($data as $key => $value) {
            $data[$key] = sanarDatos($conexion,$value);
        }
        return $data;
    } else {
        // Evitar warnings de mysqli_real_escape_string con nulls
        if ($data === null) {
            return null;
        }
        if ($data === false) {
            return '0';
        }
        return mysqli_real_escape_string($conexion, $data);
    }
 }



?>