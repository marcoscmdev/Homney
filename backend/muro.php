<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    if (!isset($datos_recibidos->titulo)     || $datos_recibidos->titulo == null     ||
        !isset($datos_recibidos->cuerpo)     || $datos_recibidos->cuerpo == null     ||
        !isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    // Imagen opcional (ruta devuelta por subir_imagen.php)
    $value_imagen = (isset($datos_recibidos->imagen) && $datos_recibidos->imagen != null)
        ? "'$datos_recibidos->imagen'" : "NULL";

    // fecha_pub tiene DEFAULT CURRENT_TIMESTAMP, no es necesaria en el INSERT
    $consulta = "INSERT INTO `MURO` (`titulo`, `cuerpo`, `id_usuario`, `imagen`)
                 VALUES ('$datos_recibidos->titulo', '$datos_recibidos->cuerpo',
                         '$datos_recibidos->id_usuario', $value_imagen)";
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
    if (!isset($datos_recibidos->id_pub)  || $datos_recibidos->id_pub == null  ||
        !isset($datos_recibidos->titulo)  || $datos_recibidos->titulo == null  ||
        !isset($datos_recibidos->cuerpo)  || $datos_recibidos->cuerpo == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $value_imagen_upd = (isset($datos_recibidos->imagen) && $datos_recibidos->imagen != null)
        ? "'$datos_recibidos->imagen'" : "NULL";

    $consulta_update = "UPDATE `MURO`
                        SET `titulo`  = '$datos_recibidos->titulo',
                            `cuerpo`  = '$datos_recibidos->cuerpo',
                            `imagen`  = $value_imagen_upd
                        WHERE `id_pub` = '$datos_recibidos->id_pub'";
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
    if (count($_GET) == 1 && isset($id_pub) && $id_pub != null) {
        $consulta_delete = "DELETE FROM `MURO` WHERE `id_pub` = '$id_pub'";
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
    $join = '';
    if (count($_GET) == 0) {
        $where = '';
    } elseif (count($_GET) == 1 && isset($id_pub) && $id_pub != null) {
        $where = " WHERE m.id_pub = '$id_pub'";
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE m.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        // Filtrar publicaciones del muro solo del hogar indicado
        $join  = " JOIN USUARIO u ON m.id_usuario = u.id_usuario";
        $where = " WHERE u.id_hogar = '$id_hogar'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    $consulta = "SELECT m.id_pub, m.fecha_pub, m.titulo, m.cuerpo, m.id_usuario, m.imagen
                 FROM MURO m
                 $join
                 $where
                 ORDER BY m.fecha_pub DESC";
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
