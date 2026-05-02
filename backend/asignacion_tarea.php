<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // PK compuesta: id_tarea + id_usuario, ambos obligatorios
    if (!isset($datos_recibidos->id_tarea)   || $datos_recibidos->id_tarea == null   ||
        !isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_observaciones = (isset($datos_recibidos->observaciones) && $datos_recibidos->observaciones != null)
        ? "'$datos_recibidos->observaciones'" : "NULL";

    $consulta = "INSERT INTO `ASIGNACION_TAREA` (`id_tarea`, `id_usuario`, `observaciones`)
                 VALUES ('$datos_recibidos->id_tarea', '$datos_recibidos->id_usuario',
                         $value_observaciones)";
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
    // Solo se puede editar las observaciones
    if (!isset($datos_recibidos->id_tarea)   || $datos_recibidos->id_tarea == null   ||
        !isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_observaciones = (isset($datos_recibidos->observaciones) && $datos_recibidos->observaciones != null)
        ? "'$datos_recibidos->observaciones'" : "NULL";

    $consulta_update = "UPDATE `ASIGNACION_TAREA`
                        SET `observaciones` = $value_observaciones
                        WHERE `id_tarea`   = '$datos_recibidos->id_tarea'
                          AND `id_usuario` = '$datos_recibidos->id_usuario'";
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
    if (count($_GET) == 2 && isset($id_tarea) && $id_tarea != null
                          && isset($id_usuario) && $id_usuario != null) {
        // Eliminar asignación concreta
        $consulta_delete = "DELETE FROM `ASIGNACION_TAREA`
                            WHERE `id_tarea` = '$id_tarea' AND `id_usuario` = '$id_usuario'";
    } elseif (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        // Eliminar todas las asignaciones de una tarea
        $consulta_delete = "DELETE FROM `ASIGNACION_TAREA` WHERE `id_tarea` = '$id_tarea'";
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
    $extra_join = '';
    if (count($_GET) == 0) {
        $where = '';
    } elseif (count($_GET) == 1 && isset($id_tarea) && $id_tarea != null) {
        $where = " WHERE at.id_tarea = '$id_tarea'";
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE at.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        // Todas las asignaciones de tareas que pertenecen al hogar indicado
        $extra_join = " JOIN TAREA t ON at.id_tarea = t.id_tarea
                        JOIN HABITACION h ON t.id_habitacion = h.id_habitacion";
        $where = " WHERE h.id_hogar = '$id_hogar'";
    } elseif (count($_GET) == 2 && isset($id_tarea) && $id_tarea != null
                                && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE at.id_tarea = '$id_tarea' AND at.id_usuario = '$id_usuario'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT at.id_tarea, at.id_usuario, at.observaciones
                 FROM ASIGNACION_TAREA at
                 $extra_join
                 $where";
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
