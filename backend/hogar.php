<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // clave_inv es obligatoria y única
    if (!isset($datos_recibidos->clave_inv) || $datos_recibidos->clave_inv == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "INSERT INTO `HOGAR` (`clave_inv`)
                 VALUES ('$datos_recibidos->clave_inv')";
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
    // id_hogar y clave_inv obligatorios
    if (!isset($datos_recibidos->id_hogar) || $datos_recibidos->id_hogar == null ||
        !isset($datos_recibidos->clave_inv) || $datos_recibidos->clave_inv == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta_update = "UPDATE `HOGAR`
                        SET `clave_inv` = '$datos_recibidos->clave_inv'
                        WHERE `id_hogar` = '$datos_recibidos->id_hogar'";
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
    if (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        $consulta_delete = "DELETE FROM `HOGAR` WHERE `id_hogar` = '$id_hogar'";
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
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        $where = " WHERE h.id_hogar = '$id_hogar'";
    } elseif (count($_GET) == 1 && isset($clave_inv) && $clave_inv != null) {
        $where = " WHERE h.clave_inv = '$clave_inv'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT h.id_hogar, h.clave_inv
                 FROM HOGAR h
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
