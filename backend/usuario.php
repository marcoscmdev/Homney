<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') { // INSERT

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    /* ********************************** */
    // Campos obligatorios
    if (!isset($datos_recibidos->nombre)          || $datos_recibidos->nombre == null          ||
        !isset($datos_recibidos->email)            || $datos_recibidos->email == null            ||
        !isset($datos_recibidos->telefono_movil)   || $datos_recibidos->telefono_movil == null   ||
        !isset($datos_recibidos->clave)            || $datos_recibidos->clave == null            ||
        !isset($datos_recibidos->sexo)             || $datos_recibidos->sexo == null             ||
        !isset($datos_recibidos->id_hogar)         || $datos_recibidos->id_hogar == null         ||
        !isset($datos_recibidos->rol)              || $datos_recibidos->rol == null) {
        die_por_fallo_en_sintaxis_peticion();
    }


    // Validación fecha nacimiento (no futura, no anterior a 1900)
    if (isset($datos_recibidos->fecha_nacimiento) && $datos_recibidos->fecha_nacimiento != null) {
        $fecha_ts = strtotime($datos_recibidos->fecha_nacimiento);
        if ($fecha_ts === false || $fecha_ts > time() || date('Y', $fecha_ts) < 1900) {
            $respuesta[STATUS] = FAIL;
            $respuesta[DATA] = 'Fecha de nacimiento inválida';
            header("Content-type: application/json");
            echo json_encode($respuesta);
            exit;
        }
    }

    // Campos opcionales
    $value_fecha_nac = (isset($datos_recibidos->fecha_nacimiento) && $datos_recibidos->fecha_nacimiento != null)
        ? "'$datos_recibidos->fecha_nacimiento'" : "NULL";
    $value_avatar = (isset($datos_recibidos->avatar) && $datos_recibidos->avatar != null)
        ? "'$datos_recibidos->avatar'" : "'uploads/perfiles/default.png'";

    $consulta = "INSERT INTO `USUARIO` (`nombre`, `email`, `telefono_movil`, `clave`,
                                        `fecha_nacimiento`, `sexo`, `avatar`, `id_hogar`, `rol`)
                 VALUES ('$datos_recibidos->nombre', '$datos_recibidos->email',
                         '$datos_recibidos->telefono_movil', '$datos_recibidos->clave',
                         $value_fecha_nac, '$datos_recibidos->sexo',
                         $value_avatar, '$datos_recibidos->id_hogar', '$datos_recibidos->rol')";
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
    // id_usuario obligatorio para identificar el registro
    if (!isset($datos_recibidos->id_usuario) || $datos_recibidos->id_usuario == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $sets = array();

    if (isset($datos_recibidos->nombre)        && $datos_recibidos->nombre != null)
        $sets[] = "`nombre` = '$datos_recibidos->nombre'";
    if (isset($datos_recibidos->email)         && $datos_recibidos->email != null)
        $sets[] = "`email` = '$datos_recibidos->email'";
    if (isset($datos_recibidos->telefono_movil) && $datos_recibidos->telefono_movil != null)
        $sets[] = "`telefono_movil` = '$datos_recibidos->telefono_movil'";
    if (isset($datos_recibidos->clave)         && $datos_recibidos->clave != null)
        $sets[] = "`clave` = '$datos_recibidos->clave'";
    if (isset($datos_recibidos->fecha_nacimiento))
        $sets[] = "`fecha_nacimiento` = " . ($datos_recibidos->fecha_nacimiento != null ? "'$datos_recibidos->fecha_nacimiento'" : "NULL");
    if (isset($datos_recibidos->sexo)          && $datos_recibidos->sexo != null)
        $sets[] = "`sexo` = '$datos_recibidos->sexo'";
    if (isset($datos_recibidos->avatar)        && $datos_recibidos->avatar != null)
        $sets[] = "`avatar` = '$datos_recibidos->avatar'";
    if (isset($datos_recibidos->id_hogar)      && $datos_recibidos->id_hogar != null)
        $sets[] = "`id_hogar` = '$datos_recibidos->id_hogar'";
    if (isset($datos_recibidos->rol)           && $datos_recibidos->rol != null)
        $sets[] = "`rol` = '$datos_recibidos->rol'";

    if (count($sets) == 0) die_por_fallo_en_sintaxis_peticion();


    // Validación fecha nacimiento (no futura, no anterior a 1900)
    if (isset($datos_recibidos->fecha_nacimiento) && $datos_recibidos->fecha_nacimiento != null) {
        $fecha_ts = strtotime($datos_recibidos->fecha_nacimiento);
        if ($fecha_ts === false || $fecha_ts > time() || date('Y', $fecha_ts) < 1900) {
            $respuesta[STATUS] = FAIL;
            $respuesta[DATA] = 'Fecha de nacimiento inválida';
            header("Content-type: application/json");
            echo json_encode($respuesta);
            exit;
        }
    }

    $consulta_update = "UPDATE `USUARIO` SET " . implode(', ', $sets) . "
                        WHERE `id_usuario` = '$datos_recibidos->id_usuario'";
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
    if (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {

        // 1. Obtener el id_hogar del usuario antes de borrarlo
        $res_hogar = @mysqli_query($conexion,
            "SELECT id_hogar FROM USUARIO WHERE id_usuario = '$id_usuario'");
        if (!$res_hogar) die_por_fallo_en_consulta("SELECT id_hogar", $conexion);
        $fila_hogar = mysqli_fetch_assoc($res_hogar);
        $id_hogar_usuario = $fila_hogar ? (int)$fila_hogar['id_hogar'] : 0;

        // 2. Historial de tareas del usuario
        @mysqli_query($conexion,
            "DELETE FROM TAREAS_REALIZADAS WHERE id_usuario = '$id_usuario'");

        // 3. Asignaciones de tareas del usuario
        @mysqli_query($conexion,
            "DELETE FROM ASIGNACION_TAREA WHERE id_usuario = '$id_usuario'");

        // 4. Repartos en los que el usuario participa (no es pagador)
        @mysqli_query($conexion,
            "DELETE FROM REPARTO_GASTO WHERE id_usuario = '$id_usuario'");

        // 5. Repartos de los gastos pagados por el usuario
        @mysqli_query($conexion,
            "DELETE FROM REPARTO_GASTO
             WHERE id_gasto IN (SELECT id_gasto FROM GASTO WHERE id_usuario_pagador = '$id_usuario')");

        // 6. Gastos pagados por el usuario
        @mysqli_query($conexion,
            "DELETE FROM GASTO WHERE id_usuario_pagador = '$id_usuario'");

        // 7. Publicaciones del muro del usuario
        @mysqli_query($conexion,
            "DELETE FROM MURO WHERE id_usuario = '$id_usuario'");

        // 8-9-10. Tareas e historial de las habitaciones del hogar
        if ($id_hogar_usuario > 0) {
            @mysqli_query($conexion,
                "DELETE tr FROM TAREAS_REALIZADAS tr
                 JOIN TAREA t ON tr.id_tarea = t.id_tarea
                 JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
                 WHERE h.id_hogar = '$id_hogar_usuario'");

            @mysqli_query($conexion,
                "DELETE asig FROM ASIGNACION_TAREA asig
                 JOIN TAREA t ON asig.id_tarea = t.id_tarea
                 JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
                 WHERE h.id_hogar = '$id_hogar_usuario'");

            @mysqli_query($conexion,
                "DELETE t FROM TAREA t
                 JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
                 WHERE h.id_hogar = '$id_hogar_usuario'");

            // 11. Habitaciones del hogar
            @mysqli_query($conexion,
                "DELETE FROM HABITACION WHERE id_hogar = '$id_hogar_usuario'");
        }

        // 12. Por último, el propio usuario (el hogar NO se borra)
        $consulta_delete = "DELETE FROM `USUARIO` WHERE `id_usuario` = '$id_usuario'";

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
    } elseif (count($_GET) == 1 && isset($id_usuario) && $id_usuario != null) {
        $where = " WHERE u.id_usuario = '$id_usuario'";
    } elseif (count($_GET) == 1 && isset($id_hogar) && $id_hogar != null) {
        $where = " WHERE u.id_hogar = '$id_hogar'";
    } elseif (count($_GET) == 1 && isset($email) && $email != null) {
        $where = " WHERE u.email = '$email'";
    } else {
        die_por_fallo_en_sintaxis_peticion();
    }

    // NOTA: la clave (hash) no se devuelve 
    $consulta = "SELECT u.id_usuario, u.nombre, u.email, u.telefono_movil,
                        u.fecha_nacimiento, u.sexo, u.avatar,
                        u.id_hogar, u.rol, u.fecha_registro
                 FROM USUARIO u
                 $where
                 ORDER BY u.nombre";
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
