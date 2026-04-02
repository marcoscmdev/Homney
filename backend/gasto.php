<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->fecha)               || $datos_recibidos->fecha == null               ||
        !isset($datos_recibidos->categoria)           || $datos_recibidos->categoria == null           ||
        !isset($datos_recibidos->concepto)            || $datos_recibidos->concepto == null            ||
        !isset($datos_recibidos->modo)                || $datos_recibidos->modo == null                ||
        !isset($datos_recibidos->tipo)                || $datos_recibidos->tipo == null                ||
        !isset($datos_recibidos->importe)             || $datos_recibidos->importe == null             ||
        !isset($datos_recibidos->id_hogar)            || $datos_recibidos->id_hogar == null            ||
        !isset($datos_recibidos->id_usuario_pagador)  || $datos_recibidos->id_usuario_pagador == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "INSERT INTO `GASTO` (`fecha`, `categoria`, `concepto`, `modo`, `tipo`,
                                      `importe`, `id_hogar`, `id_usuario_pagador`)
                 VALUES ('$datos_recibidos->fecha', '$datos_recibidos->categoria',
                         '$datos_recibidos->concepto', '$datos_recibidos->modo',
                         '$datos_recibidos->tipo', '$datos_recibidos->importe',
                         '$datos_recibidos->id_hogar', '$datos_recibidos->id_usuario_pagador')";
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
    if (!isset($datos_recibidos->id_gasto) || $datos_recibidos->id_gasto == null ||
        !isset($datos_recibidos->fecha)     || $datos_recibidos->fecha == null     ||
        !isset($datos_recibidos->categoria) || $datos_recibidos->categoria == null ||
        !isset($datos_recibidos->concepto)  || $datos_recibidos->concepto == null  ||
        !isset($datos_recibidos->modo)      || $datos_recibidos->modo == null      ||
        !isset($datos_recibidos->tipo)      || $datos_recibidos->tipo == null      ||
        !isset($datos_recibidos->importe)   || $datos_recibidos->importe == null   ||
        !isset($datos_recibidos->id_usuario_pagador) || $datos_recibidos->id_usuario_pagador == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta_update = "UPDATE `GASTO`
                        SET `fecha`              = '$datos_recibidos->fecha',
                            `categoria`          = '$datos_recibidos->categoria',
                            `concepto`           = '$datos_recibidos->concepto',
                            `modo`               = '$datos_recibidos->modo',
                            `tipo`               = '$datos_recibidos->tipo',
                            `importe`            = '$datos_recibidos->importe',
                            `id_usuario_pagador` = '$datos_recibidos->id_usuario_pagador'
                        WHERE `id_gasto` = '$datos_recibidos->id_gasto'";
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
    if (count($_GET) == 1 && isset($id_gasto) && $id_gasto != null) {
        $consulta_delete = "DELETE FROM `GASTO` WHERE `id_gasto` = '$id_gasto'";
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
        $where = " WHERE g.id_gasto = '$id_gasto'";
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        $where = " WHERE g.id_hogar = '$id_hogar'";
    } elseif (count($_GET) == 1 && isset($id_usuario_pagador) && $id_usuario_pagador != null) {
        $where = " WHERE g.id_usuario_pagador = '$id_usuario_pagador'";
    } elseif (count($_GET) == 2 && isset($id_hogar) && $id_hogar != null
                                && isset($categoria) && $categoria != null) {
        $where = " WHERE g.id_hogar = '$id_hogar' AND g.categoria = '$categoria'";
    } elseif (count($_GET) == 2 && isset($id_hogar) && $id_hogar != null
                                && isset($tipo) && $tipo != null) {
        $where = " WHERE g.id_hogar = '$id_hogar' AND g.tipo = '$tipo'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT g.id_gasto, g.fecha, g.categoria, g.concepto,
                        g.modo, g.tipo, g.importe, g.id_hogar, g.id_usuario_pagador
                 FROM GASTO g
                 $where
                 ORDER BY g.fecha DESC";
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
