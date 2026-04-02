<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // PK compuesta: id_tarea + id_usuario + fecha_realizacion
    if (!isset($datos_recibidos->id_tarea)          || $datos_recibidos->id_tarea == null          ||
        !isset($datos_recibidos->id_usuario)        || $datos_recibidos->id_usuario == null        ||
        !isset($datos_recibidos->fecha_realizacion) || $datos_recibidos->fecha_realizacion == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_observaciones  = (isset($datos_recibidos->observaciones) && $datos_recibidos->observaciones != null)
        ? "'$datos_recibidos->observaciones'" : "NULL";
    $value_duracion_real  = (isset($datos_recibidos->duracion_real) && $datos_recibidos->duracion_real != null)
        ? "'$datos_recibidos->duracion_real'" : "NULL";

    $consulta = "INSERT INTO `TAREAS_REALIZADAS` (`id_tarea`, `id_usuario`, `fecha_realizacion`,
                                                   `observaciones`, `duracion_real`)
                 VALUES ('$datos_recibidos->id_tarea', '$datos_recibidos->id_usuario',
                         '$datos_recibidos->fecha_realizacion', $value_observaciones,
                         $value_duracion_real)";
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta, $conexion);

    $respuesta[STATUS] = SUCCESS;
    // PK compuesta, no hay AUTO_INCREMENT
    $respuesta[DATA] = array("num_filas" => mysqli_affected_rows($conexion));

    header("Content-type: application/json");
    echo json_encode($respuesta);

} elseif ($_SERVER['REQUEST_METHOD'] === 'PUT') { // UPDATE

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // PK completa obligatoria para identificar el registro
    if (!isset($datos_recibidos->id_tarea)          || $datos_recibidos->id_tarea == null          ||
        !isset($datos_recibidos->id_usuario)        || $datos_recibidos->id_usuario == null        ||
        !isset($datos_recibidos->fecha_realizacion) || $datos_recibidos->fecha_realizacion == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_observaciones = (isset($datos_recibidos->observaciones) && $datos_recibidos->observaciones != null)
        ? "'$datos_recibidos->observaciones'" : "NULL";
    $value_duracion_real = (isset($datos_recibidos->duracion_real) && $datos_recibidos->duracion_real != null)
        ? "'$datos_recibidos->duracion_real'" : "NULL";

    $consulta_update = "UPDATE `TAREAS_REALIZADAS`
                        SET `observaciones` = $value_observaciones,
                            `duracion_real` = $value_duracion_real
                        WHERE `id_tarea`          = '$datos_recibidos->id_tarea'
                          AND `id_usuario`        = '$datos_recibidos->id_usuario'
                          AND `fecha_realizacion` = '$datos_recibidos->fecha_realizacion'";
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
    if (count($_GET) == 3 && isset($id_tarea)          && $id_tarea != null
                          && isset($id_usuario)        && $id_usuario != null
                          && isset($fecha_realizacion) && $fecha_realizacion != null) {
        // Eliminar registro concreto (PK completa)
        $consulta_delete = "DELETE FROM `TAREAS_REALIZADAS`
                            WHERE `id_tarea`          = '$id_tarea'
                              AND `id_usuario`        = '$id_usuario'
                              AND `fecha_realizacion` = '$fecha_realizacion'";
    } elseif (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        // Eliminar todo el historial de una tarea
        $consulta_delete = "DELETE FROM `TAREAS_REALIZADAS` WHERE `id_tarea` = '$id_tarea'";
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        // Eliminar todo el historial de un usuario
        $consulta_delete = "DELETE FROM `TAREAS_REALIZADAS` WHERE `id_usuario` = '$id_usuario'";
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
    if (count($_GET) == 0) {
        $where = '';
    } elseif (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        $where = " WHERE tr.id_tarea = '$id_tarea'";
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE tr.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 2 && isset($id_tarea) && $id_tarea != null
                                && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE tr.id_tarea = '$id_tarea' AND tr.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 3 && isset($id_tarea) && $id_tarea != null
                                && isset($id_usuario) && $id_usuario != null
                                && isset($fecha_realizacion) && $fecha_realizacion != null) {
        $where = " WHERE tr.id_tarea = '$id_tarea'
                     AND tr.id_usuario = '$id_usuario'
                     AND tr.fecha_realizacion = '$fecha_realizacion'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT tr.id_tarea, tr.id_usuario, tr.fecha_realizacion,
                        tr.observaciones, tr.duracion_real
                 FROM TAREAS_REALIZADAS tr
                 $where
                 ORDER BY tr.fecha_realizacion DESC";
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
