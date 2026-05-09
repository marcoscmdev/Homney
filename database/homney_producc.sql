-- ============================================================
--  Homney — Base de datos de producción
--  Nombre BD: homney_producc
--  Versión: 1.0  |  Mayo 2026
--  Motor: MySQL 8.0+  |  Charset: utf8mb4 / utf8mb4_spanish_ci
-- ============================================================
--  Cambios respecto a homney_dev:
--    · Tabla ficheros eliminada (no utilizada por la app)
--    · USUARIO.sexo actualizado → hombre / mujer / no_binario / prefiero_no_decirlo
--    · USUARIO.rol  actualizado → fundador / miembro  (antes admin/miembro)
--    · USUARIO.clave  comentario corregido → MD5
--    · HOGAR.nombre  charset unificado a utf8mb4
--    · HOGAR.clave_inv longitud reducida a VARCHAR(10)
--    · GASTO.importe  precisión ajustada a DECIMAL(10,2)
--    · REPARTO_GASTO.importe  ídem
--    · MURO.titulo  ampliado a VARCHAR(100)
--    · TAREA FK id_habitacion → ON DELETE SET NULL (antes RESTRICT)
--    · MURO FK id_usuario     → ON DELETE CASCADE  (antes RESTRICT)
--    · ASIGNACION_TAREA FKs   → ON DELETE CASCADE  (antes RESTRICT)
--    · TAREAS_REALIZADAS FKs  → ON DELETE CASCADE  (antes RESTRICT)
--    · REPARTO_GASTO FK gasto → ON DELETE CASCADE  (antes RESTRICT)
--    · CATEGORIA: jerarquía completa con ≥3 hijos por categoría padre
--    · Sin datos de prueba (BD limpia de producción)
-- ============================================================

SET SQL_MODE            = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone           = "+00:00";
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS  = 0;

-- ─────────────────────────────────────────────────────────────
CREATE DATABASE IF NOT EXISTS `homney_producc`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;
USE `homney_producc`;
-- ─────────────────────────────────────────────────────────────


-- ══════════════════════════════════════════════════════════════
--  1. HOGAR
--     Raíz del modelo: todo usuario, habitación y gasto
--     pertenece a un hogar.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `HOGAR` (
  `id_hogar`  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `clave_inv` VARCHAR(10)   NOT NULL COMMENT 'Código alfanumérico de invitación (6 chars)',
  `nombre`    VARCHAR(100)  DEFAULT NULL,
  PRIMARY KEY (`id_hogar`),
  UNIQUE KEY `uk_clave_inv` (`clave_inv`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  2. USUARIO
--     Un usuario pertenece a exactamente un hogar.
--     Rol 'fundador': quien creó el hogar (etiqueta visual).
--     Rol 'miembro' : quien se unió mediante clave de invitación.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `USUARIO` (
  `id_usuario`      INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  `nombre`          VARCHAR(100)   NOT NULL,
  `email`           VARCHAR(150)   NOT NULL,
  `telefono_movil`  VARCHAR(20)    NOT NULL,
  `clave`           VARCHAR(255)   NOT NULL COMMENT 'Hash MD5 de la contraseña',
  `fecha_nacimiento` DATE          DEFAULT NULL,
  `sexo`            ENUM(
                    'Mujer','Hombre','No binario','Género fluido',
                    'Transgénero','Agénero','Prefiero no decirlo','Otro'
                  ) NOT NULL,
  `id_hogar`        INT UNSIGNED   NOT NULL,
  `avatar`  VARCHAR(100)    DEFAULT NULL,
  `rol`             ENUM('fundador','miembro') NOT NULL,
  `fecha_registro`  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uk_email`     (`email`),
  UNIQUE KEY `uk_telefono`  (`telefono_movil`),
  KEY `fk_usuario_hogar` (`id_hogar`),
  CONSTRAINT `fk_usuario_hogar`
    FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  3. HABITACION
--     Cada habitación pertenece a un hogar.
--     Al borrar un hogar se eliminan en cascada sus habitaciones.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `HABITACION` (
  `id_habitacion` INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `nombre`        VARCHAR(100)  NOT NULL,
  `tipo`          ENUM(
                    'generica','cocina','aseo','garaje','exterior',
                    'dormitorio','infantil','comedor','salon','oficina',
                    'trastero','recibidor','terraza','deportiva'
                  ) NOT NULL,
  `id_hogar`      INT UNSIGNED  NOT NULL,
  PRIMARY KEY (`id_habitacion`),
  KEY `fk_habitacion_hogar` (`id_hogar`),
  CONSTRAINT `fk_habitacion_hogar`
    FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  4. TAREA
--     Una tarea puede estar asociada a una habitación (opcional).
--     Si se borra la habitación, la tarea queda sin asignar (SET NULL),
--     no se elimina.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `TAREA` (
  `id_tarea`                        INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  `nombre`                          VARCHAR(150)   NOT NULL,
  `duracion`                        TINYINT UNSIGNED DEFAULT NULL
                                    COMMENT 'Duración estimada en minutos',
  `frecuencia`                      ENUM('dia','semana','mes','variable')
                                    NOT NULL,
  `num_veces`                       TINYINT        NOT NULL DEFAULT 1,
  `explicacion_frecuencia_variable` VARCHAR(255)   DEFAULT NULL,
  `id_habitacion`                   INT UNSIGNED   DEFAULT NULL,
  PRIMARY KEY (`id_tarea`),
  KEY `fk_tarea_habitacion` (`id_habitacion`),
  CONSTRAINT `fk_tarea_habitacion`
    FOREIGN KEY (`id_habitacion`) REFERENCES `HABITACION` (`id_habitacion`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  5. ASIGNACION_TAREA
--     Tabla N:M entre TAREA y USUARIO.
--     Si se borra la tarea o el usuario, la asignación desaparece.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `ASIGNACION_TAREA` (
  `id_tarea`      INT UNSIGNED  NOT NULL,
  `id_usuario`    INT UNSIGNED  NOT NULL,
  `observaciones` VARCHAR(255)  DEFAULT NULL,
  PRIMARY KEY (`id_tarea`, `id_usuario`),
  KEY `fk_asigna_usuario` (`id_usuario`),
  CONSTRAINT `fk_asigna_tarea`
    FOREIGN KEY (`id_tarea`)   REFERENCES `TAREA`   (`id_tarea`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_asigna_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  6. TAREAS_REALIZADAS
--     Historial de ejecuciones de una tarea por usuario.
--     PK compuesta por usuario + datetime + tarea.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `TAREAS_REALIZADAS` (
  `id_tarea`          INT UNSIGNED  NOT NULL,
  `id_usuario`        INT UNSIGNED  NOT NULL,
  `fecha_realizacion` DATETIME      NOT NULL,
  `observaciones`     VARCHAR(255)  DEFAULT NULL,
  `duracion_real`     TINYINT UNSIGNED DEFAULT NULL
                      COMMENT 'Duración real en minutos',
  PRIMARY KEY (`id_usuario`, `fecha_realizacion`, `id_tarea`),
  KEY `fk_realizadas_tarea` (`id_tarea`),
  CONSTRAINT `fk_realizadas_tarea`
    FOREIGN KEY (`id_tarea`)   REFERENCES `TAREA`   (`id_tarea`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_realizada_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  7. CATEGORIA
--     Auto-referenciada: categ_padre NULL = categoría raíz.
--     Insertar primero los padres, luego los hijos.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `CATEGORIA` (
  `nombre`      VARCHAR(60)   NOT NULL,
  `categ_padre` VARCHAR(60)   DEFAULT NULL,
  `descripcion` VARCHAR(150)  NOT NULL,
  PRIMARY KEY (`nombre`),
  KEY `fk_categ_padre` (`categ_padre`),
  CONSTRAINT `fk_categ_padre`
    FOREIGN KEY (`categ_padre`) REFERENCES `CATEGORIA` (`nombre`)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;

-- ── Categorías padre ─────────────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alimentación',         NULL, 'Compra de comida, bebidas y productos básicos del hogar'),
('Alquiler / Hipoteca',  NULL, 'Pagos mensuales por vivienda propia o arrendada'),
('Educación',            NULL, 'Formación, libros, matrículas y material de estudio'),
('Inversiones',          NULL, 'Dinero destinado a ahorro e inversión financiera'),
('Limpieza / Hogar',     NULL, 'Productos de limpieza y mantenimiento del hogar'),
('Mascotas',             NULL, 'Manutención y cuidado de animales de compañía'),
('Ocio / Entretenimiento', NULL, 'Salidas, suscripciones y actividades recreativas'),
('Otros',                NULL, 'Gastos que no encajan en las demás categorías'),
('Ropa / Calzado',       NULL, 'Prendas de vestir, calzado y accesorios personales'),
('Salud / Farmacia',     NULL, 'Medicamentos, consultas médicas y seguros de salud'),
('Suministros',          NULL, 'Facturas de electricidad, agua, gas e internet'),
('Tecnología',           NULL, 'Dispositivos electrónicos, software y electrodomésticos'),
('Transporte',           NULL, 'Combustible, vehículo, transporte público y aparcamiento');

-- ── Subcategorías: Alimentación ──────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Supermercado y despensa',  'Alimentación', 'Compra semanal en supermercado o hipermercado'),
('Comida a domicilio',       'Alimentación', 'Pedidos de comida mediante apps o teléfono'),
('Restaurantes y bares',     'Alimentación', 'Consumo en establecimientos de hostelería'),
('Mercados y fruterías',     'Alimentación', 'Compra en mercados locales, fruterías y carnicerías'),
('Panadería y pastelería',   'Alimentación', 'Pan, bollería y productos de pastelería');

-- ── Subcategorías: Alquiler / Hipoteca ───────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alquiler mensual',           'Alquiler / Hipoteca', 'Renta mensual del contrato de arrendamiento'),
('Cuota hipotecaria',          'Alquiler / Hipoteca', 'Pago mensual de la hipoteca al banco'),
('Comunidad de propietarios',  'Alquiler / Hipoteca', 'Cuota de la comunidad de vecinos'),
('Seguro del hogar',           'Alquiler / Hipoteca', 'Prima del seguro multirriesgo o del hogar'),
('Gastos de agencia e impuestos', 'Alquiler / Hipoteca', 'ITP, gestoría, notaría y costes de formalización');

-- ── Subcategorías: Educación ─────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Libros y material escolar',  'Educación', 'Libros de texto, cuadernos y material fungible'),
('Matrículas y tasas',         'Educación', 'Inscripciones en colegios, universidades o FP'),
('Cursos y formación',         'Educación', 'Cursos online, academias, idiomas y formación continua'),
('Actividades extraescolares', 'Educación', 'Deportes, música, idiomas u otras actividades fuera del horario escolar'),
('Uniforme y equipamiento',    'Educación', 'Uniformes, material deportivo y equipamiento específico');

-- ── Subcategorías: Inversiones ───────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Fondos de inversión',  'Inversiones', 'Aportaciones a fondos de inversión o planes de pensiones'),
('Acciones y bolsa',     'Inversiones', 'Compra de acciones o ETFs en mercados financieros'),
('Criptomonedas',        'Inversiones', 'Compra de activos digitales como Bitcoin o Ethereum'),
('Plan de ahorro',       'Inversiones', 'Transferencias periódicas a cuentas de ahorro o depósitos'),
('Crowdfunding',         'Inversiones', 'Inversión en proyectos a través de plataformas participativas');

-- ── Subcategorías: Limpieza / Hogar ──────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Productos de limpieza',      'Limpieza / Hogar', 'Detergentes, desinfectantes, suavizante y limpiadoras'),
('Mantenimiento y reparaciones','Limpieza / Hogar', 'Fontanero, electricista, pintor y reparaciones del hogar'),
('Menaje y utensilios',        'Limpieza / Hogar', 'Vajilla, cubertería, ollas y utensilios de cocina'),
('Decoración y bricolaje',     'Limpieza / Hogar', 'Muebles, cuadros, plantas y materiales de bricolaje'),
('Jardinería y exteriores',    'Limpieza / Hogar', 'Plantas, herramientas y mantenimiento de jardín o terraza');

-- ── Subcategorías: Mascotas ──────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alimentación mascota',  'Mascotas', 'Pienso, comida húmeda y snacks para la mascota'),
('Veterinario y salud',   'Mascotas', 'Consultas veterinarias, vacunas y medicación animal'),
('Accesorios mascota',    'Mascotas', 'Juguetes, cama, transportín y accesorios varios'),
('Peluquería mascota',    'Mascotas', 'Baño, corte de pelo y cuidado estético del animal'),
('Seguro de mascota',     'Mascotas', 'Prima del seguro de responsabilidad civil o de salud animal');

-- ── Subcategorías: Ocio / Entretenimiento ────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Suscripciones digitales',  'Ocio / Entretenimiento', 'Netflix, Spotify, Disney+, Amazon Prime y similares'),
('Cine y espectáculos',      'Ocio / Entretenimiento', 'Entradas de cine, teatro, conciertos y eventos'),
('Viajes y vacaciones',      'Ocio / Entretenimiento', 'Vuelos, hoteles, alojamientos y excursiones'),
('Gimnasio y deporte',       'Ocio / Entretenimiento', 'Cuota de gimnasio, clases deportivas y material'),
('Salidas nocturnas',        'Ocio / Entretenimiento', 'Bares, discotecas y ocio nocturno');

-- ── Subcategorías: Otros ─────────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Regalos y donaciones',         'Otros', 'Regalos a familiares, amigos y donaciones benéficas'),
('Multas y tasas administrativas','Otros', 'Multas de tráfico, tasas municipales e impuestos varios'),
('Gastos imprevistos',           'Otros', 'Gastos no planificados que no caben en otra categoría'),
('Gastos bancarios',             'Otros', 'Comisiones bancarias, transferencias y gastos financieros');

-- ── Subcategorías: Ropa / Calzado ────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Ropa adulto',           'Ropa / Calzado', 'Prendas de vestir para adultos'),
('Ropa infantil',         'Ropa / Calzado', 'Ropa y uniformes para niños y adolescentes'),
('Calzado',               'Ropa / Calzado', 'Zapatos, deportivas, botas y zapatillas'),
('Ropa deportiva',        'Ropa / Calzado', 'Equipación y ropa técnica para deporte'),
('Accesorios personales', 'Ropa / Calzado', 'Bolsos, cinturones, relojes y complementos de moda');

-- ── Subcategorías: Salud / Farmacia ──────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Medicamentos',           'Salud / Farmacia', 'Fármacos con o sin receta médica'),
('Consultas médicas',      'Salud / Farmacia', 'Visitas al médico de cabecera, especialistas y urgencias'),
('Dentista y óptica',      'Salud / Farmacia', 'Revisiones dentales, ortodoncia, gafas y lentillas'),
('Seguro médico',          'Salud / Farmacia', 'Prima mensual o anual de seguro de salud privado'),
('Higiene y cosmética',    'Salud / Farmacia', 'Cremas, perfumes, productos de higiene personal y droguería');

-- ── Subcategorías: Suministros ───────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Electricidad',           'Suministros', 'Factura de luz del hogar'),
('Agua',                   'Suministros', 'Factura del suministro de agua'),
('Gas',                    'Suministros', 'Factura de gas natural o butano / propano'),
('Internet y telefonía',   'Suministros', 'Tarifa de fibra, móvil y paquetes convergentes'),
('Calefacción y climatización', 'Suministros', 'Gastos de calefacción central, aire acondicionado o bomba de calor');

-- ── Subcategorías: Tecnología ────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Teléfonos y tablets',      'Tecnología', 'Compra y accesorios de smartphones y tablets'),
('Ordenadores y portátiles', 'Tecnología', 'PCs, portátiles, componentes y periféricos'),
('Electrodomésticos',        'Tecnología', 'Lavadoras, frigoríficos, microondas y pequeño electrodoméstico'),
('Software y suscripciones', 'Tecnología', 'Licencias, antivirus, almacenamiento en la nube y apps de pago'),
('Reparaciones digitales',   'Tecnología', 'Servicio técnico de dispositivos y reparaciones electrónicas');

-- ── Subcategorías: Transporte ────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Combustible',                 'Transporte', 'Gasolina, gasóleo o recarga eléctrica del vehículo'),
('Transporte público y taxi',   'Transporte', 'Autobús, metro, tren, taxi y VTC (Uber, Cabify...)'),
('Mantenimiento de vehículo',   'Transporte', 'ITV, seguro del coche, revisiones y reparaciones mecánicas'),
('Aparcamiento y peajes',       'Transporte', 'Zonas de aparcamiento regulado, garajes y autopistas'),
('Bicicleta y micromovilidad',  'Transporte', 'Patinete eléctrico, bicicleta y accesorios de movilidad urbana');


-- ══════════════════════════════════════════════════════════════
--  8. GASTO
--     importe en DECIMAL(10,3) para permitir hasta 9999999.999, suficiente para cualquier gasto doméstico.:
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `GASTO` (
  `id_gasto`           INT UNSIGNED   NOT NULL AUTO_INCREMENT,
  `fecha`              DATETIME       NOT NULL,
  `categoria`          VARCHAR(60)    NOT NULL,
  `concepto`           VARCHAR(255)   NOT NULL,
  `modo`               ENUM('efectivo','transferencia','tarjeta','bizum')
                                      NOT NULL,
  `tipo`               ENUM('fijo','ocasional') NOT NULL,
  `importe`            DECIMAL(10,3)  NOT NULL,
  `id_hogar`           INT UNSIGNED   NOT NULL,
  `id_usuario_pagador` INT UNSIGNED   NOT NULL,
  PRIMARY KEY (`id_gasto`),
  KEY `fk_gasto_hogar`      (`id_hogar`),
  KEY `fk_gasto_usuario`    (`id_usuario_pagador`),
  KEY `fk_gasto_categoria`  (`categoria`),
  CONSTRAINT `fk_gasto_hogar`
    FOREIGN KEY (`id_hogar`) REFERENCES `HOGAR` (`id_hogar`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_gasto_usuario`
    FOREIGN KEY (`id_usuario_pagador`) REFERENCES `USUARIO` (`id_usuario`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_gasto_categoria`
    FOREIGN KEY (`categoria`) REFERENCES `CATEGORIA` (`nombre`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  9. REPARTO_GASTO
--     Si se elimina un gasto, sus repartos se eliminan en cascada.
--     No se puede eliminar un usuario con repartos pendientes.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `REPARTO_GASTO` (
  `id_gasto`   INT UNSIGNED   NOT NULL,
  `id_usuario` INT UNSIGNED   NOT NULL,
  `pagador`    TINYINT(1)     NOT NULL DEFAULT '0'
               COMMENT '1 = es quien pagó, 0 = es deudor',
  `importe`    DECIMAL(10,3)  NOT NULL,
  `abonado`    TINYINT(1)     NOT NULL DEFAULT '0'
               COMMENT '1 = ya compensó su parte',
  PRIMARY KEY (`id_gasto`, `id_usuario`),
  KEY `fk_reparto_usuario` (`id_usuario`),
  CONSTRAINT `fk_reparto_gasto`
    FOREIGN KEY (`id_gasto`)   REFERENCES `GASTO`   (`id_gasto`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_reparto_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


-- ══════════════════════════════════════════════════════════════
--  10. MURO
--      Si se elimina el usuario autor, sus publicaciones
--      desaparecen automáticamente.
-- ══════════════════════════════════════════════════════════════
CREATE TABLE `MURO` (
  `id_pub`    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `fecha_pub` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `titulo`    VARCHAR(100)  NOT NULL,
  `cuerpo`    TEXT          NOT NULL,
  `id_usuario` INT UNSIGNED NOT NULL,
  `imagen`    VARCHAR(255)  DEFAULT NULL,
  PRIMARY KEY (`id_pub`),
  KEY `fk_muro_usuario` (`id_usuario`),
  CONSTRAINT `fk_muro_usuario`
    FOREIGN KEY (`id_usuario`) REFERENCES `USUARIO` (`id_usuario`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_spanish_ci;


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  FIN DEL SCRIPT
--  La base de datos está lista y vacía para producción.
--  Los únicos datos precargados son las CATEGORÍAS de gastos.
-- ============================================================
