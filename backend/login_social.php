<?php
/**
 * login_social.php — Login por email sin contraseña.
 * Usado cuando el usuario se autentica con Google o Apple vía Firebase.
 * Firebase ya ha verificado la identidad; aquí solo buscamos la cuenta Homney asociada.
 *
 * POST { email: "..." }
 * Responde igual que login.php para que el cliente Android lo procese igual.
 */
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    die_por_fallo_en_sintaxis_peticion();
}

$json = file_get_contents('php://input');
if (strlen($json) == 0) die_por_fallo_en_sintaxis_peticion();

$datos = json_decode($json);
$datos = sanarDatos($conexion, $datos);

if (!isset($datos->email) || $datos->email == null) {
    die_por_fallo_en_sintaxis_peticion();
}

$email = $datos->email;

$consulta = "SELECT u.id_usuario, u.nombre, u.email, u.telefono_movil,
                    u.fecha_nacimiento, u.sexo, u.avatar,
                    u.id_hogar, u.rol, u.fecha_registro,
                    h.nombre AS nombre_hogar
             FROM USUARIO u
             LEFT JOIN HOGAR h ON u.id_hogar = h.id_hogar
             WHERE u.email = '$email'
             LIMIT 1";

$resultado = @mysqli_query($conexion, $consulta);
if (!$resultado) die_por_fallo_en_consulta($consulta, $conexion);

header("Content-type: application/json; charset=utf-8");

if (mysqli_num_rows($resultado) > 0) {
    $fila = mysqli_fetch_array($resultado, MYSQLI_ASSOC);
    ajustaColumnasFormatoJSON($resultado, $fila);
    $respuesta[STATUS] = SUCCESS;
    $respuesta[DATA]   = array($fila); // Array para que RespuestaLista<Usuario> funcione en Android
} else {
    $respuesta[STATUS] = FAIL;
    $respuesta[DATA]   = 'usuario_no_encontrado'; // El cliente Android distingue este literal
}

echo json_encode($respuesta);
?>
