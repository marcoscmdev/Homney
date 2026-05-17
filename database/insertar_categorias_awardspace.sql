-- =====================================================================
-- SCRIPT: Insertar categorías completas en la BD de producción AwardSpace
-- Ejecutar en phpMyAdmin de AwardSpace (pestaña SQL)
-- IMPORTANTE: Ejecutar completo, los padres deben existir antes que los hijos
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Limpiar tabla por si hay datos parciales/incorrectos
DELETE FROM `CATEGORIA`;

-- ── Categorías padre (categ_padre = NULL) ─────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alimentación',           NULL, 'Compra de comida, bebidas y productos básicos del hogar'),
('Alquiler / Hipoteca',    NULL, 'Pagos mensuales por vivienda propia o arrendada'),
('Educación',              NULL, 'Formación, libros, matrículas y material de estudio'),
('Inversiones',            NULL, 'Dinero destinado a ahorro e inversión financiera'),
('Limpieza / Hogar',       NULL, 'Productos de limpieza y mantenimiento del hogar'),
('Mascotas',               NULL, 'Manutención y cuidado de animales de compañía'),
('Ocio / Entretenimiento', NULL, 'Salidas, suscripciones y actividades recreativas'),
('Otros',                  NULL, 'Gastos que no encajan en las demás categorías'),
('Ropa / Calzado',         NULL, 'Prendas de vestir, calzado y accesorios personales'),
('Salud / Farmacia',       NULL, 'Medicamentos, consultas médicas y seguros de salud'),
('Suministros',            NULL, 'Facturas de electricidad, agua, gas e internet'),
('Tecnología',             NULL, 'Dispositivos electrónicos, software y electrodomésticos'),
('Transporte',             NULL, 'Combustible, vehículo, transporte público y aparcamiento');

-- ── Subcategorías: Alimentación ───────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Supermercado y despensa',  'Alimentación', 'Compra semanal en supermercado o hipermercado'),
('Comida a domicilio',       'Alimentación', 'Pedidos de comida mediante apps o teléfono'),
('Restaurantes y bares',     'Alimentación', 'Consumo en establecimientos de hostelería'),
('Mercados y fruterías',     'Alimentación', 'Compra en mercados locales, fruterías y carnicerías'),
('Panadería y pastelería',   'Alimentación', 'Pan, bollería y productos de pastelería');

-- ── Subcategorías: Alquiler / Hipoteca ───────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alquiler mensual',              'Alquiler / Hipoteca', 'Renta mensual del contrato de arrendamiento'),
('Cuota hipotecaria',             'Alquiler / Hipoteca', 'Pago mensual de la hipoteca al banco'),
('Comunidad de propietarios',     'Alquiler / Hipoteca', 'Cuota de la comunidad de vecinos'),
('Seguro del hogar',              'Alquiler / Hipoteca', 'Prima del seguro multirriesgo o del hogar'),
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
('Fondos de inversión', 'Inversiones', 'Aportaciones a fondos de inversión o planes de pensiones'),
('Acciones y bolsa',    'Inversiones', 'Compra de acciones o ETFs en mercados financieros'),
('Criptomonedas',       'Inversiones', 'Compra de activos digitales como Bitcoin o Ethereum'),
('Plan de ahorro',      'Inversiones', 'Transferencias periódicas a cuentas de ahorro o depósitos'),
('Crowdfunding',        'Inversiones', 'Inversión en proyectos a través de plataformas participativas');

-- ── Subcategorías: Limpieza / Hogar ──────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Productos de limpieza',       'Limpieza / Hogar', 'Detergentes, desinfectantes, suavizante y limpiadoras'),
('Mantenimiento y reparaciones','Limpieza / Hogar', 'Fontanero, electricista, pintor y reparaciones del hogar'),
('Menaje y utensilios',         'Limpieza / Hogar', 'Vajilla, cubertería, ollas y utensilios de cocina'),
('Muebles y decoración',        'Limpieza / Hogar', 'Compra de muebles, cuadros y elementos decorativos'),
('Jardín y exterior',           'Limpieza / Hogar', 'Herramientas, plantas y mantenimiento del espacio exterior');

-- ── Subcategorías: Mascotas ──────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Alimentación de mascota', 'Mascotas', 'Pienso, comida húmeda y snacks para la mascota'),
('Veterinario y salud',     'Mascotas', 'Consultas, vacunas, medicamentos y seguros de mascota'),
('Accesorios y juguetes',   'Mascotas', 'Correas, camas, juguetes y accesorios varios'),
('Peluquería y estética',   'Mascotas', 'Baño, corte de pelo y cuidado estético de la mascota'),
('Guardería y hotel',       'Mascotas', 'Servicios de cuidado de mascotas durante ausencias');

-- ── Subcategorías: Ocio / Entretenimiento ────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Suscripciones digitales', 'Ocio / Entretenimiento', 'Netflix, Spotify, Disney+ y otras plataformas'),
('Cine y teatro',           'Ocio / Entretenimiento', 'Entradas a cines, teatros, conciertos y espectáculos'),
('Deportes y gimnasio',     'Ocio / Entretenimiento', 'Cuota de gimnasio, actividades deportivas y equipamiento'),
('Viajes y vacaciones',     'Ocio / Entretenimiento', 'Alojamiento, transporte y gastos durante viajes'),
('Hobbies y aficiones',     'Ocio / Entretenimiento', 'Material para aficiones, colecciones y actividades creativas');

-- ── Subcategorías: Ropa / Calzado ────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Ropa de adulto',    'Ropa / Calzado', 'Prendas de vestir para adultos'),
('Ropa de niño',      'Ropa / Calzado', 'Ropa infantil y de bebé'),
('Calzado',           'Ropa / Calzado', 'Zapatos, zapatillas, botas y sandalias'),
('Accesorios moda',   'Ropa / Calzado', 'Bolsos, cinturones, gafas y complementos de moda'),
('Ropa deportiva',    'Ropa / Calzado', 'Ropa y calzado específicos para actividades deportivas');

-- ── Subcategorías: Salud / Farmacia ──────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Medicamentos y farmacia', 'Salud / Farmacia', 'Medicamentos con y sin receta, parafarmacia y suplementos'),
('Médico y especialistas',  'Salud / Farmacia', 'Consultas médicas, especialistas y pruebas diagnósticas'),
('Seguro médico',           'Salud / Farmacia', 'Prima mensual o anual del seguro de salud privado'),
('Óptica y dental',         'Salud / Farmacia', 'Gafas, lentillas, revisiones dentales y ortodoncia'),
('Psicología y bienestar',  'Salud / Farmacia', 'Terapia psicológica, fisioterapia y tratamientos de bienestar');

-- ── Subcategorías: Suministros ───────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Electricidad',        'Suministros', 'Factura de la luz y contratación eléctrica'),
('Agua',                'Suministros', 'Factura del agua y saneamiento'),
('Gas',                 'Suministros', 'Factura del gas natural o butano'),
('Internet y telefonía','Suministros', 'Fibra óptica, móvil y paquetes de telecomunicaciones'),
('Seguro del hogar',    'Suministros', 'Prima del seguro multirriesgo del hogar');

-- ── Subcategorías: Tecnología ────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Teléfonos y tablets',      'Tecnología', 'Compra y accesorios de smartphones y tablets'),
('Ordenadores y portátiles', 'Tecnología', 'PCs, portátiles, componentes y periféricos'),
('Electrodomésticos',        'Tecnología', 'Lavadoras, frigoríficos, microondas y pequeño electrodoméstico'),
('Software y suscripciones', 'Tecnología', 'Licencias, antivirus, almacenamiento en la nube y apps de pago'),
('Reparaciones digitales',   'Tecnología', 'Servicio técnico de dispositivos y reparaciones electrónicas');

-- ── Subcategorías: Transporte ────────────────────────────────
INSERT INTO `CATEGORIA` (`nombre`, `categ_padre`, `descripcion`) VALUES
('Combustible',               'Transporte', 'Gasolina, gasóleo o recarga eléctrica del vehículo'),
('Transporte público y taxi', 'Transporte', 'Autobús, metro, tren, taxi y VTC (Uber, Cabify...)'),
('Mantenimiento de vehículo', 'Transporte', 'ITV, seguro del coche, revisiones y reparaciones mecánicas'),
('Aparcamiento y peajes',     'Transporte', 'Zonas de aparcamiento regulado, garajes y autopistas'),
('Bicicleta y micromovilidad','Transporte', 'Patinete eléctrico, bicicleta y accesorios de movilidad urbana');

SET FOREIGN_KEY_CHECKS = 1;

-- Verificar resultado
SELECT nombre, categ_padre FROM CATEGORIA ORDER BY categ_padre IS NULL DESC, categ_padre, nombre;
