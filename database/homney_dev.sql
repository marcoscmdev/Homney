-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost:8889
-- Tiempo de generación: 20-03-2026 a las 12:31:00
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
-- Eliminar la base de datos si existe y crearla de nuevo
DROP DATABASE IF EXISTS `homney_dev`;
CREATE DATABASE `homney_dev`;
USE `homney_dev`;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ASIGNACION_TAREA`
--

CREATE TABLE `ASIGNACION_TAREA` (
  `id_tarea` int UNSIGNED NOT NULL,
  `id_usuario` int UNSIGNED NOT NULL,
  `observaciones` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `CATEGORIA`
--

CREATE TABLE `CATEGORIA` (
  `nombre` varchar(50) NOT NULL,
  `categ_padre` varchar(50) DEFAULT NULL,
  `descripcion` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `GASTO`
--

CREATE TABLE `GASTO` (
  `id_gasto` int UNSIGNED NOT NULL,
  `fecha` datetime NOT NULL,
  `categoria` varchar(50) NOT NULL,
  `concepto` varchar(255) NOT NULL,
  `modo` enum('efectivo','transferencia','tarjeta','bizum') NOT NULL,
  `tipo` enum('fijo','ocasional') NOT NULL,
  `importe` decimal(10,3) NOT NULL,
  `id_hogar` int UNSIGNED NOT NULL,
  `id_usuario_pagador` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `HABITACION`
--

CREATE TABLE `HABITACION` (
  `id_habitacion` int UNSIGNED NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `tipo` enum('generica','cocina','aseo','garaje','exterior') NOT NULL,
  `id_hogar` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `HOGAR`
--

CREATE TABLE `HOGAR` (
  `id_hogar` int UNSIGNED NOT NULL,
  `clave_inv` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `MURO`
--

CREATE TABLE `MURO` (
  `id_pub` int UNSIGNED NOT NULL,
  `fecha_pub` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `titulo` varchar(50) NOT NULL,
  `cuerpo` text NOT NULL,
  `id_usuario` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `REPARTO_GASTO`
--

CREATE TABLE `REPARTO_GASTO` (
  `id_gasto` int UNSIGNED NOT NULL,
  `id_usuario` int UNSIGNED NOT NULL,
  `pagador` tinyint(1) NOT NULL,
  `importe` decimal(10,3) NOT NULL,
  `abonado` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `TAREA`
--

CREATE TABLE `TAREA` (
  `id_tarea` int UNSIGNED NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `duracion` tinyint UNSIGNED DEFAULT NULL,
  `frecuencia` enum('dia','semana','mes','variable') NOT NULL,
  `num_veces` tinyint NOT NULL,
  `explicacion_frecuencia_variable` varchar(255) DEFAULT NULL,
  `id_habitacion` int UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `TAREAS_REALIZADAS`
--

CREATE TABLE `TAREAS_REALIZADAS` (
  `id_tarea` int UNSIGNED NOT NULL,
  `id_usuario` int UNSIGNED NOT NULL,
  `fecha_realizacion` datetime NOT NULL,
  `observaciones` varchar(255) DEFAULT NULL,
  `duracion_real` tinyint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `USUARIO`
--

CREATE TABLE `USUARIO` (
  `id_usuario` int UNSIGNED NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `telefono_movil` varchar(20) NOT NULL,
  `clave` varchar(255) NOT NULL COMMENT 'SHA-256',
  `fecha_nacimiento` date DEFAULT NULL,
  `sexo` enum('femenino','masculino','otro') NOT NULL,
  `avatar` varchar(255) NOT NULL DEFAULT 'uploads/perfiles/default.png',
  `id_hogar` int UNSIGNED NOT NULL,
  `rol` enum('admin','miembro') NOT NULL,
  `fecha_registro` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `ASIGNACION_TAREA`
--
ALTER TABLE `ASIGNACION_TAREA`
  ADD PRIMARY KEY (`id_tarea`,`id_usuario`),
  ADD KEY `fk_asigna_usuario` (`id_usuario`);

--
-- Indices de la tabla `CATEGORIA`
--
ALTER TABLE `CATEGORIA`
  ADD PRIMARY KEY (`nombre`),
  ADD KEY `categoria_padre` (`categ_padre`);

--
-- Indices de la tabla `GASTO`
--
ALTER TABLE `GASTO`
  ADD PRIMARY KEY (`id_gasto`),
  ADD KEY `fk_gasto_hogar` (`id_hogar`),
  ADD KEY `fk_gasto_usuario` (`id_usuario_pagador`),
  ADD KEY `fk_gasto_categoria` (`categoria`);

--
-- Indices de la tabla `HABITACION`
--
ALTER TABLE `HABITACION`
  ADD PRIMARY KEY (`id_habitacion`),
  ADD KEY `fk_habitacion_hogar` (`id_hogar`);

--
-- Indices de la tabla `HOGAR`
--
ALTER TABLE `HOGAR`
  ADD PRIMARY KEY (`id_hogar`),
  ADD UNIQUE KEY `clave_inv` (`clave_inv`);

--
-- Indices de la tabla `MURO`
--
ALTER TABLE `MURO`
  ADD PRIMARY KEY (`id_pub`),
  ADD KEY `fk_muro_usuario` (`id_usuario`);

--
-- Indices de la tabla `REPARTO_GASTO`
--
ALTER TABLE `REPARTO_GASTO`
  ADD PRIMARY KEY (`id_gasto`,`id_usuario`),
  ADD KEY `fk_reparto_usuario` (`id_usuario`);

--
-- Indices de la tabla `TAREA`
--
ALTER TABLE `TAREA`
  ADD PRIMARY KEY (`id_tarea`),
  ADD KEY `fk_tarea_habitacion` (`id_habitacion`);

--
-- Indices de la tabla `TAREAS_REALIZADAS`
--
ALTER TABLE `TAREAS_REALIZADAS`
  ADD PRIMARY KEY (`id_usuario`,`fecha_realizacion`,`id_tarea`),
  ADD KEY `fk_realizadas_tarea` (`id_tarea`);

--
-- Indices de la tabla `USUARIO`
--
ALTER TABLE `USUARIO`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `telefono_movil` (`telefono_movil`),
  ADD KEY `fk_usuario_hogar` (`id_hogar`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `GASTO`
--
ALTER TABLE `GASTO`
  MODIFY `id_gasto` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `HABITACION`
--
ALTER TABLE `HABITACION`
  MODIFY `id_habitacion` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `HOGAR`
--
ALTER TABLE `HOGAR`
  MODIFY `id_hogar` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `MURO`
--
ALTER TABLE `MURO`
  MODIFY `id_pub` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `TAREA`
--
ALTER TABLE `TAREA`
  MODIFY `id_tarea` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `USUARIO`
--
ALTER TABLE `USUARIO`
  MODIFY `id_usuario` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `ASIGNACION_TAREA`
--
ALTER TABLE `ASIGNACION_TAREA`
  ADD CONSTRAINT `fk_asigna_tarea` FOREIGN KEY (`id_tarea`) REFERENCES `TAREA` (`id_tarea`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_asigna_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `CATEGORIA`
--
ALTER TABLE `CATEGORIA`
  ADD CONSTRAINT `fk_categoria_padre` FOREIGN KEY (`categ_padre`) REFERENCES `CATEGORIA` (`nombre`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `GASTO`
--
ALTER TABLE `GASTO`
  ADD CONSTRAINT `fk_gasto_categoria` FOREIGN KEY (`categoria`) REFERENCES `CATEGORIA` (`nombre`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_gasto_hogar` FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_gasto_usuario` FOREIGN KEY (`id_usuario_pagador`) REFERENCES `USUARIO` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `HABITACION`
--
ALTER TABLE `HABITACION`
  ADD CONSTRAINT `fk_habitacion_hogar` FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `MURO`
--
ALTER TABLE `MURO`
  ADD CONSTRAINT `fk_muro_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `REPARTO_GASTO`
--
ALTER TABLE `REPARTO_GASTO`
  ADD CONSTRAINT `fk_reparto_gasto` FOREIGN KEY (`id_gasto`) REFERENCES `GASTO` (`id_gasto`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_reparto_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `TAREA`
--
ALTER TABLE `TAREA`
  ADD CONSTRAINT `fk_tarea_habitacion` FOREIGN KEY (`id_habitacion`) REFERENCES `HABITACION` (`id_habitacion`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `TAREAS_REALIZADAS`
--
ALTER TABLE `TAREAS_REALIZADAS`
  ADD CONSTRAINT `fk_realizada_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_realizadas_tarea` FOREIGN KEY (`id_tarea`) REFERENCES `TAREA` (`id_tarea`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Filtros para la tabla `USUARIO`
--
ALTER TABLE `USUARIO`
  ADD CONSTRAINT `fk_usuario_hogar` FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`) ON DELETE RESTRICT ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
