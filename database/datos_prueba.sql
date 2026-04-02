-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost:8889
-- Tiempo de generación: 20-03-2026 a las 13:23:58
-- Versión del servidor: 8.0.40
-- Versión de PHP: 8.3.14

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `homney_dev`
--

--
-- Volcado de datos para la tabla `GASTO`
--

INSERT INTO `GASTO` (`id_gasto`, `fecha`, `categoria`, `concepto`, `modo`, `tipo`, `importe`, `id_hogar`, `id_usuario_pagador`) VALUES
(1, '2026-03-18 13:08:57', 'Alimentación', 'total compra', 'tarjeta', 'ocasional', 130.000, 1, 1),
(4, '2026-03-20 13:13:48', 'Suministros', 'factura del mes de diciembre', 'transferencia', 'ocasional', 120.000, 1, 1),
(5, '2026-03-01 14:13:48', 'Alquiler / Hipoteca', 'Alquiler', 'transferencia', 'fijo', 500.000, 2, 4);

--
-- Volcado de datos para la tabla `HABITACION`
--

INSERT INTO `HABITACION` (`id_habitacion`, `nombre`, `tipo`, `id_hogar`) VALUES
(1, 'salon', 'generica', 1),
(2, 'baño', 'aseo', 1),
(3, 'cocina', 'cocina', 1),
(4, 'Salon/Dormitorio', 'generica', 2),
(5, 'cocina', 'cocina', 2);

--
-- Volcado de datos para la tabla `HOGAR`
--

INSERT INTO `HOGAR` (`id_hogar`, `clave_inv`) VALUES
(1, 'clave_invitacion_hogar_1'),
(2, 'clave_invitacion_hogar_2');

--
-- Volcado de datos para la tabla `MURO`
--

INSERT INTO `MURO` (`id_pub`, `fecha_pub`, `titulo`, `cuerpo`, `id_usuario`) VALUES
(1, '2026-03-20 14:17:30', 'Lista de la compra', 'Hay que comprar\r\nMahonesa\r\nAceitunas \r\nAtun \r\nY pasar por el estanco', 3),
(2, '2026-03-20 14:17:30', 'Clases de piano', 'hoy empieza Cristina a clases de piano, hay que ir a buscarla a las 17:00', 1);

--
-- Volcado de datos para la tabla `REPARTO_GASTO`
--

INSERT INTO `REPARTO_GASTO` (`id_gasto`, `id_usuario`, `pagador`, `importe`, `abonado`) VALUES
(1, 1, 1, 123.343, 0);

--
-- Volcado de datos para la tabla `TAREA`
--

INSERT INTO `TAREA` (`id_tarea`, `nombre`, `duracion`, `frecuencia`, `num_veces`, `explicacion_frecuencia_variable`, `id_habitacion`, `id_hogar`) VALUES
(1, 'Fregar platos', NULL, 'dia', 2, 'Despues de comer y despues de cenar', 3, 1),
(2, 'Pasar el aspirador', 30, 'semana', 2, 'Sobre todo en el salon', 1, 1);

--
-- Volcado de datos para la tabla `TAREAS_REALIZADAS`
--

INSERT INTO `TAREAS_REALIZADAS` (`id_tarea`, `id_usuario`, `fecha_realizacion`, `observaciones`, `duracion_real`) VALUES
(2, 2, '2026-03-20 13:20:47', NULL, 60),
(1, 4, '2026-03-20 13:20:47', 'Quedo pendiente la airfrier', 30);

--
-- Volcado de datos para la tabla `USUARIO`
--

INSERT INTO `USUARIO` (`id_usuario`, `nombre`, `email`, `telefono_movil`, `clave`, `fecha_nacimiento`, `sexo`, `avatar`, `id_hogar`, `rol`, `fecha_registro`) VALUES
(1, 'Ana García', 'ana@example.com', '600111222', 'pass1234', '1998-05-14', 'femenino', 'uploads/perfiles/default.png', 1, 'admin', '2026-03-20 12:59:31'),
(2, 'Carlos López', 'carlos@example.com', '600333444', 'pass1234', '1996-11-20', 'masculino', 'uploads/perfiles/default.png', 1, 'miembro', '2026-03-20 12:59:31'),
(3, 'María Pérez', 'maria@example.com', '600555666', 'pass1234', '2000-03-08', 'femenino', 'uploads/perfiles/default.png', 2, 'admin', '2026-03-20 12:59:31'),
(4, 'Therian Carrasco', 'therian@example.com', '555987987', 'guau', NULL, 'otro', 'uploads/perfiles/default.png', 2, 'miembro', '2026-03-20 13:02:29');
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
