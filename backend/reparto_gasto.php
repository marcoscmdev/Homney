<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->id_gasto)   || $datos_recibidos->id_gasto == null   ||
        !isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null ||
        !isset($datos_recibidos->pagador)    || !isset($datos_recibidos->importe)    || $datos_recibidos->importe == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    // abonado por defecto 0 (no abonado) si no llega
    $value_abonado = (isset($datos_recibidos->abonado)) ? (int)$datos_recibidos->abonado : 0;
    $value_pagador  = (int)$datos_recibidos->pagador;

    $consulta = "INSERT INTO `REPARTO_GASTO` (`id_gasto`, `id_usuario`, `pagador`, `importe`, `abonado`)
                 VALUES ('$datos_recibidos->id_gasto', '$datos_recibidos->id_usuario',
                         '$value_pagador', '$datos_recibidos->importe', '$value_abonado')";
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
    // PK compuesta obligatoria + al menos un campo a actualizar
    if (!isset($datos_recibidos->id_gasto)   || $datos_recibidos->id_gasto == null   ||
        !isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $sets = array();
    if (isset($datos_recibidos->importe))
        $sets[] = "`importe` = '$datos_recibidos->importe'";
    if (isset($datos_recibidos->pagador))
        $sets[] = "`pagador` = '" . (int)$datos_recibidos->pagador . "'";
    if (isset($datos_recibidos->abonado))
        $sets[] = "`abonado` = '" . (int)$datos_recibidos->abonado . "'";

    if (count($sets) == 0) die_por_fallo_en_sintaxis_peticion();

    $consulta_update = "UPDATE `REPARTO_GASTO`
                        SET " . implode(', ', $sets) . "
                        WHERE `id_gasto`   = '$datos_recibidos->id_gasto'
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
    // Borrar un reparto concreto (por PK compuesta) o todos los repartos de un gasto
    if (count($_GET) == 2 && isset($id_gasto) && $id_gasto != null
                          && isset($id_usuario) && $id_usuario != null) {
        $consulta_delete = "DELETE FROM `REPARTO_GASTO`
                            WHERE `id_gasto` = '$id_gasto' AND `id_usuario` = '$id_usuario'";
    } elseif (count($_GET) == 1 && isset($id_gasto) && $id_gasto != null) {
        // Borrar todos los repartos de un gasto (útil antes de borrar el propio GASTO)
        $consulta_delete = "DELETE FROM `REPARTO_GASTO` WHERE `id_gasto` = '$id_gasto'";
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
    } elseif (count($_GET) == 1 && isset($id_gasto) && $id_gasto != null) {
        $where = " WHERE rg.id_gasto = '$id_gasto'";
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE rg.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 2 && isset($id_gasto) && $id_gasto != null
                                && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE rg.id_gasto = '$id_gasto' AND rg.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 2 && isset($id_usuario) && $id_usuario != null
                                && isset($abonado) && $abonado != null) {
        $where = " WHERE rg.id_usuario = '$id_usuario' AND rg.abonado = '$abonado'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT rg.id_gasto, rg.id_usuario, rg.pagador, rg.importe, rg.abonado
                 FROM REPARTO_GASTO rg
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
