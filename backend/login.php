<?php
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] === 'POST') {

    $texto_json = file_get_contents('php://input');
    if (strlen($texto_json) == 0) die_por_fallo_en_sintaxis_peticion();

    $datos_recibidos = json_decode($texto_json);
    $datos_recibidos = sanarDatos($conexion, $datos_recibidos);

    // Campos obligatorios para el login
    if (!isset($datos_recibidos->email)    || $datos_recibidos->email    == null ||
        !isset($datos_recibidos->password) || $datos_recibidos->password == null) {
        die_por_fallo_en_sintaxis_peticion();
    }

    $email    = $datos_recibidos->email;
    $password = $datos_recibidos->password;

    // Nota: la clave se compara directamente (texto plano).
    // Si en el futuro se usa hash, cambiar por: password_verify($password, $fila['clave'])
    $consulta = "SELECT u.id_usuario, u.nombre, u.email, u.telefono_movil,
                        u.fecha_nacimiento, u.sexo, u.avatar,
                        u.id_hogar, u.rol, u.fecha_registro
                 FROM USUARIO u
                 WHERE u.email = '$email' AND u.clave = '$password'
                 LIMIT 1";

    $resultado_consulta = @mysqli_query($conexion, $consulta);
    if (!$resultado_consulta) die_por_fallo_en_consulta($consulta, $conexion);

    header("Content-type: application/json");

    if (mysqli_num_rows($resultado_consulta) > 0) {
        $fila = mysqli_fetch_array($resultado_consulta, MYSQLI_ASSOC);
        ajustaColumnasFormatoJSON($resultado_consulta, $fila);

        $respuesta[STATUS] = SUCCESS;
        $respuesta[DATA]   = array($fila); // Array para que RespuestaLista<Usuario> funcione en Android
    } else {
        $respuesta[STATUS] = FAIL;
        $respuesta[DATA]   = 'Credenciales incorrectas';
    }

    echo json_encode($respuesta);

} else {
    die_por_fallo_en_sintaxis_peticion();
}
?>
