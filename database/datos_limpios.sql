-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost:8889
-- Tiempo de generación: 03-04-2026 a las 16:13:33
-- Versión del servidor: 8.0.40
-- Versión de PHP: 8.3.14

SET FOREIGN_KEY_CHECKS=0;
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
-- Volcado de datos para la tabla `ASIGNACION_TAREA`
--

INSERT INTO `ASIGNACION_TAREA` (`id_tarea`, `id_usuario`, `observaciones`) VALUES
(1, 1, NULL);

--
-- Volcado de datos para la tabla `CATEGORIA`
--

INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alimentación', NULL, 'Gastos diarios relativos a la compra de comida y bebidas'),
('Alquiler / Hipoteca', NULL, 'Pagos mensuales por vivienda propia o arrendada'),
('Comida a domicilio', 'Alimentación', 'Pedidos de comida a domicilio'),
('Educación', NULL, 'Cursos, libros de texto, matrículas y material de estudio'),
('Inversiones', NULL, 'Dinero invertido en diferentes oportunidades o ahorro'),
('Limpieza / Hogar', NULL, 'Productos de limpieza, detergentes y mantenimiento del hogar'),
('Mascotas', NULL, 'Gastos relacionados con la manutención y crianza de animales de compañía'),
('Ocio / Entretenimiento', NULL, 'Gastos de salidas, cine, suscripciones y actividades recreativas'),
('Otros', NULL, 'Gastos que no se categorizan en las demás líneas de presupuesto'),
('Ropa / Calzado', NULL, 'Compra de prendas de vestir, calzado y accesorios'),
('Salud / Farmacia', NULL, 'Medicamentos, consultas médicas, seguros de salud y análisis'),
('Suministros', NULL, 'Facturas de suministros, electricidad, agua, gas'),
('Tecnología', NULL, 'Dispositivos electrónicos, software y reparaciones digitales, electrodomésticos'),
('Transporte', NULL, 'Combustibles, mantenimiento de vehículos y pasajes de viaje, aparcamiento, transporte público');

--
-- Volcado de datos para la tabla `GASTO`
--

INSERT INTO `GASTO` (`id_gasto`, `fecha`, `categoria`, `concepto`, `modo`, `tipo`, `importe`, `id_hogar`, `id_usuario_pagador`) VALUES
(3, '2026-04-03 00:00:00', 'Alimentacion', 'Compra', 'efectivo', 'ocasional', 20.000, 1, 1);

--
-- Volcado de datos para la tabla `HABITACION`
--

INSERT INTO `HABITACION` (`id_habitacion`, `nombre`, `tipo`, `id_hogar`) VALUES
(1, 'Salon', 'generica', 1);

--
-- Volcado de datos para la tabla `HOGAR`
--

INSERT INTO `HOGAR` (`id_hogar`, `clave_inv`) VALUES
(1, 'HXY1WCI');

--
-- Volcado de datos para la tabla `REPARTO_GASTO`
--

INSERT INTO `REPARTO_GASTO` (`id_gasto`, `id_usuario`, `pagador`, `importe`, `abonado`) VALUES
(3, 1, 1, 20.000, 0);

--
-- Volcado de datos para la tabla `TAREA`
--

INSERT INTO `TAREA` (`id_tarea`, `nombre`, `duracion`, `frecuencia`, `num_veces`, `explicacion_frecuencia_variable`, `id_habitacion`) VALUES
(1, 'Pasar aspirador', NULL, 'semana', 1, NULL, 1);

--
-- Volcado de datos para la tabla `USUARIO`
--

INSERT INTO `USUARIO` (`id_usuario`, `nombre`, `email`, `telefono_movil`, `clave`, `fecha_nacimiento`, `sexo`, `avatar`, `id_hogar`, `rol`, `fecha_registro`) VALUES
(1, 'Alicia ', 'al@al', '654', '1234', NULL, 'otro', 'uploads/perfiles/default.png', 1, 'admin', '2026-04-03 15:26:32'),
(2, 'Paco', 'paquitochoco@choco.com', '567', '1234', '1999-04-12', 'masculino', 'uploads/perfiles/default.png', 1, 'miembro', '2026-04-03 16:12:04');
SET FOREIGN_KEY_CHECKS=1;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
