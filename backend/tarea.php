<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->nombre)     || $datos_recibidos->nombre == null     ||
        !isset($datos_recibidos->frecuencia) || $datos_recibidos->frecuencia == null ||
        !isset($datos_recibidos->num_veces)  || $datos_recibidos->num_veces == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    // Campos opcionales
    $value_duracion       = (isset($datos_recibidos->duracion) && $datos_recibidos->duracion != null && $datos_recibidos->duracion !== '')
        ? intval($datos_recibidos->duracion) : "NULL";
    $value_expl_variable  = (isset($datos_recibidos->explicacion_frecuencia_variable) && $datos_recibidos->explicacion_frecuencia_variable != null)
        ? "'$datos_recibidos->explicacion_frecuencia_variable'" : "NULL";
    $value_id_habitacion  = (isset($datos_recibidos->id_habitacion) && $datos_recibidos->id_habitacion != null && $datos_recibidos->id_habitacion !== '')
        ? "'$datos_recibidos->id_habitacion'" : "NULL";

    $consulta = "INSERT INTO `TAREA` (`nombre`, `duracion`, `frecuencia`, `num_veces`,
                                      `explicacion_frecuencia_variable`, `id_habitacion`)
                 VALUES ('$datos_recibidos->nombre', $value_duracion,
                         '$datos_recibidos->frecuencia', '$datos_recibidos->num_veces',
                         $value_expl_variable, $value_id_habitacion)";
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta, $conexion);

    $respuesta[STATUS] = SUCCESS;
    $id_generado = mysqli_insert_id($conexion);
    $respuesta[DATA] = array("autoincrement" => $id_generado);

    header("Content-type: application/json");
    echo json_encode($respuesta);

} elseif ($_SERVER['REQUEST_METHOD'] === 'PUT') { // UPDATE

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->id_tarea)   || $datos_recibidos->id_tarea == null   ||
        !isset($datos_recibidos->nombre)      || $datos_recibidos->nombre == null      ||
        !isset($datos_recibidos->frecuencia)  || $datos_recibidos->frecuencia == null  ||
        !isset($datos_recibidos->num_veces)   || $datos_recibidos->num_veces == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_duracion      = (isset($datos_recibidos->duracion) && $datos_recibidos->duracion != null && $datos_recibidos->duracion !== '')
        ? intval($datos_recibidos->duracion) : "NULL";
    $value_expl_variable = (isset($datos_recibidos->explicacion_frecuencia_variable) && $datos_recibidos->explicacion_frecuencia_variable != null)
        ? "'$datos_recibidos->explicacion_frecuencia_variable'" : "NULL";
    $value_id_habitacion = (isset($datos_recibidos->id_habitacion) && $datos_recibidos->id_habitacion != null && $datos_recibidos->id_habitacion !== '')
        ? "'$datos_recibidos->id_habitacion'" : "NULL";
    $consulta_update = "UPDATE `TAREA`
                        SET `nombre`                          = '$datos_recibidos->nombre',
                            `duracion`                        = $value_duracion,
                            `frecuencia`                      = '$datos_recibidos->frecuencia',
                            `num_veces`                       = '$datos_recibidos->num_veces',
                            `explicacion_frecuencia_variable` = $value_expl_variable,
                            `id_habitacion`                   = $value_id_habitacion
                        WHERE `id_tarea` = '$datos_recibidos->id_tarea'";
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta_update);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta_update, $conexion);

    $respuesta[STATUS] = SUCCESS;
    $respuesta[DATA] = array("num_filas" => mysqli_affected_rows($conexion));

    header("Content-type: application/json");
    echo json_encode($respuesta);

} elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') { // DELETE

    $_GET = sanarDatos($conexion, $_GET);
    extract($_GET);

    /* ********************************** */
    if (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        $consulta_delete = "DELETE FROM `TAREA` WHERE `id_tarea` = '$id_tarea'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta_delete);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta_delete, $conexion);

    $respuesta[STATUS] = SUCCESS;
    $respuesta[DATA] = array("num_filas" => mysqli_affected_rows($conexion));

    header("Content-type: application/json");
    echo json_encode($respuesta);

} elseif ($_SERVER['REQUEST_METHOD'] === 'GET') { // SELECT

    $_GET = sanarDatos($conexion, $_GET);
    extract($_GET);

    /* ********************************** */
    $join         = '';
    $select_extra = '';
    if (count($_GET) == 0) {
        $where = '';
    } elseif (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        $where = " WHERE t.id_tarea = '$id_tarea'";
    } elseif (count($_GET) == 1 && isset($id_habitacion) && $id_habitacion != null) {
        $where = " WHERE t.id_habitacion = '$id_habitacion'";
    } elseif (count($_GET) == 1 && isset($frecuencia) && $frecuencia != null) {
        $where = " WHERE t.frecuencia = '$frecuencia'";
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        // Tareas del hogar: JOIN con habitaciones del hogar indicado
        $join         = " JOIN HABITACION h ON t.id_habitacion = h.id_habitacion";
        $where        = " WHERE h.id_hogar = '$id_hogar'";
        $select_extra = ", h.nombre as nombre_habitacion";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT t.id_tarea, t.nombre, t.duracion, t.frecuencia,
                        t.num_veces, t.explicacion_frecuencia_variable, t.id_habitacion
                        $select_extra
                 FROM TAREA t
                 $join
                 $where
                 ORDER BY t.nombre";
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta, $conexion);

    $respuesta[STATUS] = SUCCESS;
    if (mysqli_num_rows($resultado_consulta) > 0) {
        while ($fila = mysqli_fetch_array($resultado_consulta, MYSQLI_ASSOC)) {
            ajustaColumnasFormatoJSON($resultado_consulta, $fila);
            $datos[] = $fila;
        }
        $respuesta[DATA] = $datos;
    } else {
        $respuesta[DATA] = SINDATOS;
    }

    header("Content-type: application/json");
    echo json_encode($respuesta);
}
?>
