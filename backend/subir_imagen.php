<?php
/**
 * subir_imagen.php — Endpoint genérico de subida de imágenes
 *
 * Recibe multipart/form-data con:
 *   fichero   → el archivo de imagen (campo "fichero" según VolleyMultipartRequest)
 *   tipo      → "perfil" | "muro"  (determina el subdirectorio de destino)
 *   id        → id_usuario o id_pub (se usa para nombrar el fichero)
 *
 * Devuelve:
 *   {"status":"success","data":{"ruta":"uploads/perfiles/usuario_5.jpg"}}
 *   {"status":"fail",   "data":"Mensaje de error"}
 */

require("conexion.php");

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([STATUS => FAIL, DATA => 'Solo se admite POST multipart']);
    exit;
}

// ── Parámetros ────────────────────────────────────────────────────────────────
$tipo = isset($_POST['tipo']) ? trim($_POST['tipo']) : '';
$id   = isset($_POST['id'])   ? (int)trim($_POST['id']) : 0;

if (!in_array($tipo, ['perfil', 'muro'])) {
    echo json_encode([STATUS => FAIL, DATA => 'Parámetro "tipo" inválido (perfil|muro)']);
    exit;
}
if ($id <= 0) {
    echo json_encode([STATUS => FAIL, DATA => 'Parámetro "id" requerido']);
    exit;
}

// ── Validar fichero recibido ──────────────────────────────────────────────────
if (!isset($_FILES['fichero']) || $_FILES['fichero']['error'] !== UPLOAD_ERR_OK) {
    $codigo = isset($_FILES['fichero']) ? $_FILES['fichero']['error'] : -1;
    echo json_encode([STATUS => FAIL, DATA => "Error al recibir el fichero (código $codigo)"]);
    exit;
}

$fichero    = $_FILES['fichero'];
$mime       = mime_content_type($fichero['tmp_name']);
$extensiones = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];

if (!array_key_exists($mime, $extensiones)) {
    echo json_encode([STATUS => FAIL, DATA => "Tipo de imagen no permitido ($mime). Usa JPG, PNG o WebP."]);
    exit;
}

// Límite de tamaño: 5 MB
if ($fichero['size'] > 5 * 1024 * 1024) {
    echo json_encode([STATUS => FAIL, DATA => 'La imagen supera el límite de 5 MB']);
    exit;
}

// ── Determinar ruta de destino ────────────────────────────────────────────────
$subcarpeta = ($tipo === 'perfil') ? 'perfiles' : 'muro';
$dirDestino = __DIR__ . "/uploads/$subcarpeta/";

if (!is_dir($dirDestino)) {
    mkdir($dirDestino, 0755, true);
}

$prefijo    = ($tipo === 'perfil') ? 'usuario' : 'pub';
$ext        = $extensiones[$mime];
$nombreFich = "{$prefijo}_{$id}.{$ext}";
$rutaFinal  = $dirDestino . $nombreFich;

// ── Mover el fichero ──────────────────────────────────────────────────────────
if (!move_uploaded_file($fichero['tmp_name'], $rutaFinal)) {
    echo json_encode([STATUS => FAIL, DATA => 'Error al guardar el fichero en el servidor']);
    exit;
}

// Ruta relativa (la que se guarda en la BD y se devuelve al cliente)
$rutaRelativa = "uploads/$subcarpeta/$nombreFich";

// ── Si es perfil, actualizar la columna avatar en USUARIO ─────────────────────
if ($tipo === 'perfil') {
    $rutaSanitizada = mysqli_real_escape_string($conexion, $rutaRelativa);
    @mysqli_query($conexion,
        "UPDATE `USUARIO` SET `avatar` = '$rutaSanitizada' WHERE `id_usuario` = $id");
}

// ── Respuesta ─────────────────────────────────────────────────────────────────
echo json_encode([STATUS => SUCCESS, DATA => ['ruta' => $rutaRelativa]]);
