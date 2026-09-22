# 🏠 Homney
> Organiza tu hogar compartido de forma sencilla: tareas, gastos y convivencia en una sola app.

Proyecto de **Trabajo de Fin de Ciclo (TFC)** del ciclo formativo de **Desarrollo de Aplicaciones Multiplataforma (DAM)** — [TESDAI](https://www.tesdai.com), Santiago de Compostela.

Aplicación Android nativa desarrollada íntegramente desde cero: diseño de base de datos, arquitectura cliente-servidor, API REST propia, integración de IA y publicación en Google Play Store.

**Tutor:** Oscar Fernández López

---

## 🚀 Características principales

- 🏡 **Hogares compartidos** — Varios usuarios conectados a un mismo hogar mediante clave de invitación
- ✅ **Gestión de tareas** — Creación, asignación por habitación y usuario, marcado como realizadas, recurrencia configurable
- 💸 **Gestión de gastos** — Registro, categorización y reparto automático entre miembros con seguimiento de deudas
- 📢 **Muro del hogar** — Publicaciones compartidas con soporte de imágenes
- 📊 **Panel de estadísticas** — Resumen de actividad, gastos y tareas con gráficos interactivos
- 🐝 **Homney Mate** — Asistente de IA integrado con contexto real del hogar, capaz de responder preguntas y ejecutar acciones con confirmación del usuario
- 🔐 **Autenticación** — Login por email/contraseña y login social con Google (Firebase Authentication)
- 🌍 **Disponible en Google Play Store**

---

## 🛠️ Tecnologías

| Capa | Tecnología |
|---|---|
| App Android | Java · Android SDK · Android Studio |
| Navegación | Navigation Drawer · Bottom Navigation |
| Comunicación HTTP | Volley (peticiones asíncronas) |
| Parseo JSON | Gson + TypeToken |
| Imágenes | Glide · FileProvider · CameraX |
| Gráficos | AnyChart |
| Autenticación | Firebase Authentication (email + Google) |
| Backend | PHP 7.4+ · API REST · Formato JSend |
| Base de datos | MySQL 5.7+ · 10 tablas relacionales |
| Asistente IA | Groq API (LLM llama-3.3-70b) |
| Hosting producción | AwardSpace |
| Control de versiones | Git · GitLab · GitHub |
| Testing API | Postman |
| Servidor local | MAMP |

---

## 🏗️ Arquitectura

Arquitectura cliente-servidor en cuatro capas:

```
[ App Android (Java) ]
        ↓ HTTP / JSON (Volley)
[ API REST (PHP) — 15 endpoints ]
        ↓
[ Base de datos MySQL — Producción AwardSpace ]
        ↓
[ Servicios externos: Firebase Auth · Groq IA ]
```

El backend expone **15 endpoints REST** con soporte completo GET / POST / PUT / DELETE, respuestas estandarizadas en formato JSend y validaciones de pertenencia al hogar en cada operación.

---

## 📁 Estructura del proyecto

```
homney/
├── android/        # Proyecto Android Studio (Java)
├── backend/        # API REST en PHP (15 endpoints)
├── database/       # Scripts SQL (dev + producción)
├── frontend/       # Web de soporte
└── docs/           # Diagramas, wireframes y documentación
```

---

## 📲 Instalación

La forma más sencilla de probar Homney es a través de **Google Play Store**.

[play.google.com/store/apps/details?id=com.homney.app](https://play.google.com/store/apps/details?id=com.homney.app)

Requisitos mínimos: **Android 11 o superior (API 30+)**

> La distribución por APK directa están previstos en próximas versiones.

---

## ⚙️ Entorno de desarrollo local

Si quieres explorar o compilar el proyecto en local:

**Backend:**
```bash
git clone https://gitlab.com/MarcosCMDev/homney.git
cp backend/conexion.example.php backend/conexion.php
# Configura credenciales de base de datos en conexion.php
# Importa database/homney_dev.sql en tu MySQL local
# Configura tu clave de Groq en backend/asistenteia.php
```

**Android:**
```bash
# Abre android/homney_app en Android Studio
# Copia secrets.properties.example → secrets.properties
# Añade google-services.json de tu proyecto Firebase
# Cambia SERVIDOR en WebService.java a tu backend local
# Run > Run 'app'
```

Requisitos: Android 11+ (API 30) · JDK 17 · Gradle 9.1.0

---

## 📌 Estado del proyecto

✅ **v1.0 — Publicada en Google Play Store**

Defensa y entrega proyecto de fin de ciclo **Superado**.
El desarrollo continúa con mejoras incrementales.

🔗 [Descargar en Google Play](https://play.google.com/store/apps/details?id=com.homney.app)
🌐 [Web de soporte](https://homneyapp.atwebpages.com)

---

## 🗺️ Roadmap

- [ ] Notificaciones push (Firebase Cloud Messaging)
- [ ] Chat en tiempo real entre miembros del hogar
- [ ] OCR para escaneo automático de tickets de compra
- [ ] Lista de la compra compartida
- [ ] Soporte para múltiples hogares por cuenta
- [ ] Migración de UI a Jetpack Compose
- [ ] Versión web multiplataforma + Versión IOs
- [ ] Integración con Calendar

---

## 🤝 ¿Quieres colaborar?

Si te interesa contribuir al desarrollo de Homney — ya sea con nuevas funcionalidades, mejoras de UX, optimización del backend o cualquier otra idea — escríbeme y lo hablamos.

📩 [info@marcoscm.dev](mailto:info@marcoscm.dev) · 💼 [LinkedIn](https://linkedin.com/in/marcoscmdev)

---

## 📄 Licencia

Este repositorio es público solo para fines de consulta/portfolio. Todos los derechos reservados.
No se permite el uso, copia o modificación sin mi permiso explícito (Marcos CM).

---

## 👤 Autor

**Marcos Castro Miranda**
DAM · TESDAI Santiago de Compostela

🌐 [marcoscm.dev](https://marcoscm.dev) · 💼 [LinkedIn](https://linkedin.com/in/marcoscmdev) · 📩 info@marcoscm.dev