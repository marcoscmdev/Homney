<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // nombre y descripcion obligatorios; categ_padre opcional (NULL = categoría padre)
    if (!isset($datos_recibidos->nombre)      || $datos_recibidos->nombre == null ||
        !isset($datos_recibidos->descripcion) || $datos_recibidos->descripcion == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_categ_padre = (isset($datos_recibidos->categ_padre) && $datos_recibidos->categ_padre != null)
        ? "'$datos_recibidos->categ_padre'" : "NULL";

    $consulta = "INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`)
                 VALUES ('$datos_recibidos->nombre', $value_categ_padre, '$datos_recibidos->descripcion')";
    /* ********************************** */

    $resultado_consulta = @mysqli_query($conexion, $consulta);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta, $conexion);

    $respuesta[STATUS] = SUCCESS;
    // CATEGORIA no tiene AUTO_INCREMENT (PK es el nombre)
    $respuesta[DATA] = array("num_filas" => mysqli_affected_rows($conexion));

    header("Content-type: application/json");
    echo json_encode($respuesta);

} elseif ($_SERVER['REQUEST_METHOD'] === 'PUT') { // UPDATE

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->nombre)      || $datos_recibidos->nombre == null ||
        !isset($datos_recibidos->descripcion) || $datos_recibidos->descripcion == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_categ_padre = (isset($datos_recibidos->categ_padre) && $datos_recibidos->categ_padre != null)
        ? "'$datos_recibidos->categ_padre'" : "NULL";

    $consulta_update = "UPDATE `CATEGORIA`
                        SET `categ_padre`  = $value_categ_padre,
                            `descripcion`  = '$datos_recibidos->descripcion'
                        WHERE `nombre` = '$datos_recibidos->nombre'";
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
    if (count($_GET) == 1 && isset($nombre) && $nombre != null) {
        $consulta_delete = "DELETE FROM `CATEGORIA` WHERE `nombre` = '$nombre'";
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
    } elseif (count($_GET) == 1 && isset($nombre) && $nombre != null) {
        $where = " WHERE c.nombre = '$nombre'";
    } elseif (count($_GET) == 1 && isset($categ_padre) && $categ_padre != null) {
        $where = " WHERE c.categ_padre = '$categ_padre'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT c.nombre, c.categ_padre, c.descripcion
                 FROM CATEGORIA c
                 $where
                 ORDER BY c.categ_padre, c.nombre";
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
