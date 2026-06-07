<?php
/**
 * contexto_ia.php  —  Devuelve el contexto del hogar para que el asistente IA
 *                     sea construido en el cliente Android (Groq se llama desde
 *                     Android porque AwardSpace bloquea el puerto 443 saliente).
 *
 * GET ?id_hogar=X
 * Responde: { status:"success", data: {
 *   nombre_hogar, miembros, habitaciones, tareas, asignaciones,
 *   realizadas_mes, gastos, gastos_por_categoria, categorias
 * }}
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

// ── Tareas del hogar (máx. 50, con habitación) ────────────────────────────
$tareas = [];
$res = mysqli_query($conexion,
    "SELECT t.id_tarea, t.nombre, t.frecuencia, t.num_veces,
            COALESCE(h.nombre, 'General') AS habitacion
     FROM TAREA t
     LEFT JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
     WHERE h.id_hogar = $id_hogar OR t.id_habitacion IS NULL
     ORDER BY habitacion, t.nombre LIMIT 50");
while ($fila = mysqli_fetch_assoc($res)) $tareas[] = $fila;

// ── Asignaciones: quién tiene asignada cada tarea ─────────────────────────
$asignaciones = [];
$res = mysqli_query($conexion,
    "SELECT u.nombre AS usuario, t.nombre AS tarea,
            COALESCE(h.nombre, 'General') AS habitacion
     FROM ASIGNACION_TAREA a
     JOIN TAREA t    ON a.id_tarea   = t.id_tarea
     JOIN USUARIO u  ON a.id_usuario = u.id_usuario
     LEFT JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
     WHERE u.id_hogar = $id_hogar
     ORDER BY u.nombre, t.nombre
     LIMIT 80");
while ($fila = mysqli_fetch_assoc($res)) $asignaciones[] = $fila;

// ── Tareas realizadas en los últimos 30 días ──────────────────────────────
$realizadas_mes = [];
$res = mysqli_query($conexion,
    "SELECT u.nombre AS usuario, t.nombre AS tarea,
            DATE(tr.fecha_realizacion) AS fecha,
            tr.duracion_real
     FROM TAREAS_REALIZADAS tr
     JOIN TAREA t   ON tr.id_tarea   = t.id_tarea
     JOIN USUARIO u ON tr.id_usuario = u.id_usuario
     LEFT JOIN HABITACION h ON t.id_habitacion = h.id_habitacion
     WHERE (h.id_hogar = $id_hogar OR t.id_habitacion IS NULL)
       AND u.id_hogar = $id_hogar
       AND tr.fecha_realizacion >= DATE_SUB(NOW(), INTERVAL 30 DAY)
     ORDER BY tr.fecha_realizacion DESC
     LIMIT 100");
while ($fila = mysqli_fetch_assoc($res)) $realizadas_mes[] = $fila;

// ── Últimos 30 gastos ─────────────────────────────────────────────────────
$gastos = [];
$res = mysqli_query($conexion,
    "SELECT DATE_FORMAT(fecha,'%Y-%m-%d') AS fecha, concepto, importe, categoria
     FROM GASTO
     WHERE id_hogar = $id_hogar
     ORDER BY fecha DESC
     LIMIT 30");
while ($fila = mysqli_fetch_assoc($res)) $gastos[] = $fila;

// ── Gastos del mes actual agrupados por categoría ─────────────────────────
$gastos_por_categoria = [];
$res = mysqli_query($conexion,
    "SELECT categoria, SUM(importe) AS total, COUNT(*) AS num_gastos
     FROM GASTO
     WHERE id_hogar = $id_hogar
       AND DATE_FORMAT(fecha,'%Y-%m') = DATE_FORMAT(NOW(),'%Y-%m')
     GROUP BY categoria
     ORDER BY total DESC");
while ($fila = mysqli_fetch_assoc($res)) $gastos_por_categoria[] = $fila;

// ── Respuesta ─────────────────────────────────────────────────────────────
header("Content-type: application/json; charset=utf-8");
echo json_encode([
    STATUS => SUCCESS,
    DATA   => [
        'nombre_hogar'         => $nombre_hogar,
        'miembros'             => $miembros,
        'habitaciones'         => $habitaciones,
        'categorias'           => $categorias,
        'tareas'               => $tareas,
        'asignaciones'         => $asignaciones,
        'realizadas_mes'       => $realizadas_mes,
        'gastos'               => $gastos,
        'gastos_por_categoria' => $gastos_por_categoria,
    ]
]);
