<?php
/**
 * get_imagen.php — Proxy para servir imágenes subidas vía PHP
 *
 * AwardSpace (atwebpages.com) bloquea el acceso directo a ficheros estáticos
 * en subdirectorios como uploads/. Este script actúa de intermediario:
 * recibe la petición, localiza el fichero en disco y lo devuelve como binario.
 *
 * Param GET:
 *   f  → ruta relativa al directorio backend/
 *         Ejemplos:
 *           uploads/perfiles/usuario_5.jpg
 *           uploads/muro/pub_12.jpg
 *
 * Seguridad:
 *   - Solo se sirven ficheros dentro de uploads/
 *   - Se rechaza cualquier ruta con ".." para evitar path traversal
 *   - Solo se sirven ficheros con MIME de tipo imagen
 */

// ── Obtener y validar parámetro ──────────────────────────────────────────────
$f = isset($_GET['f']) ? trim($_GET['f']) : '';

// Debe empezar por "uploads/" y no contener ".."
if (strpos($f, 'uploads/') !== 0 || strpos($f, '..') !== false || empty($f)) {
    http_response_code(403);
    header('Content-Type: text/plain');
    exit('Acceso no permitido');
}

// ── Localizar fichero en disco ───────────────────────────────────────────────
$rutaAbsoluta = __DIR__ . '/' . $f;

if (!file_exists($rutaAbsoluta) || !is_file($rutaAbsoluta)) {
    http_response_code(404);
    header('Content-Type: text/plain');
    exit('Imagen no encontrada');
}

// ── Validar que es una imagen ────────────────────────────────────────────────
$mime = mime_content_type($rutaAbsoluta);
if (strpos($mime, 'image/') !== 0) {
    http_response_code(403);
    header('Content-Type: text/plain');
    exit('Solo se sirven imágenes');
}

// ── Servir el fichero ────────────────────────────────────────────────────────
// Cache de 1 hora para aliviar carga. El cliente añade ?t= como cache-buster
// cuando sube una nueva foto, por lo que no hay problema con imágenes obsoletas.
header('Content-Type: ' . $mime);
header('Cache-Control: public, max-age=3600');
header('Content-Length: ' . filesize($rutaAbsoluta));
header('X-Content-Type-Options: nosniff');
readfile($rutaAbsoluta);
