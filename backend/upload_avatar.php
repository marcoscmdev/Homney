<?php
require("conexion.php");

// Solo acepta POST multipart/form-data
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'Método no permitido';
    die(json_encode($r));
}

if (!isset($_FILES['avatar']) || $_FILES['avatar']['error'] !== UPLOAD_ERR_OK) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'No se recibió ningún archivo o hubo un error';
    die(json_encode($r));
}

if (!isset($_POST['id_usuario']) || empty($_POST['id_usuario'])) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'id_usuario requerido';
    die(json_encode($r));
}

$id_usuario = intval($_POST['id_usuario']);

// Validar tipo MIME
$finfo    = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_file($finfo, $_FILES['avatar']['tmp_name']);
finfo_close($finfo);

$allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
if (!in_array($mimeType, $allowed)) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'Tipo de archivo no permitido. Solo JPG, PNG, GIF, WEBP';
    die(json_encode($r));
}

// Limitar tamaño: 2 MB
if ($_FILES['avatar']['size'] > 2 * 1024 * 1024) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'El archivo supera el límite de 2 MB';
    die(json_encode($r));
}

// Directorio destino
$uploadDir = __DIR__ . '/uploads/perfiles/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

// Nombre único
$ext      = pathinfo($_FILES['avatar']['name'], PATHINFO_EXTENSION) ?: 'jpg';
$filename = 'avatar_' . $id_usuario . '_' . time() . '.' . strtolower($ext);
$destPath = $uploadDir . $filename;

if (!move_uploaded_file($_FILES['avatar']['tmp_name'], $destPath)) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'No se pudo guardar el archivo';
    die(json_encode($r));
}

// Ruta relativa que se guardará en la BD
$relativePath = 'uploads/perfiles/' . $filename;

// Actualizar USUARIO
$id_usuario_safe = mysqli_real_escape_string($conexion, $id_usuario);
$path_safe       = mysqli_real_escape_string($conexion, $relativePath);
$consulta = "UPDATE `USUARIO` SET `avatar` = '$path_safe' WHERE `id_usuario` = '$id_usuario_safe'";
$resultado = @mysqli_query($conexion, $consulta);

if (!$resultado) {
    header("Content-type: application/json");
    $r[STATUS] = FAIL;
    $r[DATA]   = 'Archivo subido pero error al actualizar BD: ' . mysqli_error($conexion);
    die(json_encode($r));
}

header("Content-type: application/json");
$respuesta[STATUS] = SUCCESS;
$respuesta[DATA]   = ['path' => $relativePath];
echo json_encode($respuesta);
?>
