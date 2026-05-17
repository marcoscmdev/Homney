<?php
/**
 * contexto_ia.php  —  Devuelve el contexto del hogar para que el asistente IA
 *                     sea construido en el cliente Android (Groq se llama desde
 *                     Android porque AwardSpace bloquea el puerto 443 saliente).
 *
 * GET ?id_hogar=X
 * Responde: { status:"success", data: { miembros, habitaciones, tareas, gastos, categorias } }
 */
require("conexion.php");

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    echo json_encode([STATUS => FAIL, DATA => 'Solo se admite GET']);
    exit;
}

$id_hogar = isset($_GET['id_hogar']) ? (int)$_GET['id_hogar'] : 0;

if ($id_hogar <= 0) {
    echo json_encode([STATUS => FAIL, DATA => 'id_hogar requerido']);
    exit;
}

// ── Nombre del hogar ──────────────────────────────────────────────────────
$nombre_hogar = '';
$res_h = mysqli_query($conexion, "SELECT nombre FROM HOGAR WHERE id_hogar = $id_hogar");
if ($res_h && $row_h = mysqli_fetch_assoc($res_h)) {
    $nombre_hogar = $row_h['nombre'] ?? '';
}

// ── Miembros ──────────────────────────────────────────────────────────────
$miembros = [];
$res = mysqli_query($conexion,
    "SELECT id_usuario, nombre, rol FROM USUARIO
     WHERE id_hogar = $id_hogar ORDER BY nombre");
while ($fila = mysqli_fetch_assoc($res)) $miembros[] = $fila;

// ── Habitaciones ──────────────────────────────────────────────────────────
$habitaciones = [];
$res = mysqli_query($conexion,
    "SELECT id_habitacion, nombre FROM HABITACION
     WHERE id_hogar = $id_hogar ORDER BY nombre");
while ($fila = mysqli_fetch_assoc($res)) $habitaciones[] = $fila;

// ── Categorías de gasto ───────────────────────────────────────────────────
$categorias = [];
$res = mysqli_query($conexion, "SELECT nombre FROM CATEGORIA ORDER BY nombre");
while ($fila = mysqli_fetch_assoc($res)) $categorias[] = $fila['nombre'];

// ── Tareas (máx. 50, con nombre de habitación) ────────────────────────────
$tareas = [];
$res = mysqli_query($conexion,
    "SELECT t.nombre, t.frecuencia, t.num_veces, h.nombre AS habitacion
     FROM TAREA t
     JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
     WHERE h.id_hogar = $id_hogar
     ORDER BY h.nombre, t.nombre LIMIT 50");
while ($fila = mysqli_fetch_assoc($res)) $tareas[] = $fila;

// ── Últimos 30 gastos ─────────────────────────────────────────────────────
$gastos = [];
$res = mysqli_query($conexion,
    "SELECT fecha, concepto, importe, categoria
     FROM GASTO WHERE id_hogar = $id_hogar ORDER BY fecha DESC LIMIT 30");
while ($fila = mysqli_fetch_assoc($res)) $gastos[] = $fila;

// ── Respuesta ─────────────────────────────────────────────────────────────
header("Content-type: application/json; charset=utf-8");
echo json_encode([
    STATUS => SUCCESS,
    DATA   => [
        'nombre_hogar' => $nombre_hogar,
        'miembros'     => $miembros,
        'habitaciones' => $habitaciones,
        'categorias'   => $categorias,
        'tareas'       => $tareas,
        'gastos'       => $gastos,
    ]
]);
