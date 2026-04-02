# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**Homney** is a household management app (TFC – DAM) for shared living: task scheduling, shared expense splitting, room management, and a household wall. It consists of:

- **Backend**: PHP REST API (no framework), served by Apache (MAMP/XAMPP)
- **Frontend**: Single-page app — one HTML + one JS + one CSS file, vanilla JS only
- **Database**: MySQL 8.0 (`homney_dev`)
- **Android**: Planned (Java + Volley + GSON); currently only design diagrams exist

## Local Development Setup

This project requires a local AMP stack (MAMP, XAMPP, or similar) — there is no build step, no npm, no Composer.

1. Place the project so it is accessible at `http://localhost/homney/` (e.g. symlink or copy into the web server's document root).
2. Import the database schema:
   ```bash
   mysql -u root -proot < database/homney_dev.sql
   ```
3. Optionally load test data:
   ```bash
   mysql -u root -proot homney_dev < database/datos_prueba.sql
   ```
4. Open `http://localhost/homney/frontend/index.html` in a browser (must be served through Apache, not opened as a local file, due to `fetch` calls).

### Database credentials (hardcoded in `backend/conexion.php`)
- Host: `localhost`
- User: `root`
- Password: `root`
- Database: `homney_dev`

### Testing API endpoints manually
Use Postman or the Advanced REST Client browser extension. All endpoints are at `http://localhost/homney/backend/<entity>.php`.

Example:
```
GET    http://localhost/homney/backend/usuario.php?id_hogar=1
POST   http://localhost/homney/backend/hogar.php       (body: JSON)
PUT    http://localhost/homney/backend/tarea.php        (body: JSON)
DELETE http://localhost/homney/backend/tarea.php?id_tarea=3
```

## Architecture

### Backend — `backend/`

Each entity has one PHP file that handles all four HTTP methods on the same URL:

| File | Entity | Notes |
|---|---|---|
| `conexion.php` | — | Central: DB connection, JSend constants, `sanarDatos()`, `ajustaColumnasFormatoJSON()` |
| `usuario.php` | USUARIO | GET supports filter by `id_usuario`, `id_hogar`, or `email`; `clave` (SHA-256) is never returned |
| `hogar.php` | HOGAR | Lookup by `id_hogar` or `clave_inv` |
| `tarea.php` | TAREA | Filter by `id_tarea`, `id_hogar`, `id_habitacion`, `frecuencia` |
| `asignacion_tarea.php` | ASIGNACION_TAREA | Composite PK `(id_tarea, id_usuario)` |
| `tareas_realizadas.php` | TAREAS_REALIZADAS | Composite PK `(id_tarea, id_usuario, fecha_realizacion)` |
| `gasto.php` | GASTO | Filter by `id_gasto`, `id_hogar`, `id_usuario_pagador`, or combo `id_hogar+categoria/tipo` |
| `reparto_gasto.php` | REPARTO_GASTO | Composite PK `(id_gasto, id_usuario)` |
| `habitacion.php` | HABITACION | Belongs to HOGAR |
| `categoria.php` | CATEGORIA | Self-referential `categ_padre`; `nombre` is the PK (not an integer) |
| `muro.php` | MURO | GET by `id_hogar` uses a JOIN through USUARIO |
| `upload_avatar.php` | — | `multipart/form-data` POST only; saves to `backend/uploads/perfiles/` |

**Pattern in every entity file:**
```php
require("conexion.php");
if ($_SERVER['REQUEST_METHOD'] === 'POST')   { /* INSERT */ }
elseif (... === 'PUT')                        { /* UPDATE */ }
elseif (... === 'DELETE')                     { /* DELETE, params via $_GET */ }
elseif (... === 'GET')                        { /* SELECT */ }
```

**Response format** (JSend): `{ "status": "success"|"fail"|"error", "data": ... }`. `data` is `null` (PHP `SINDATOS`) when there are no results.

**Input sanitisation**: `sanarDatos($conexion, $data)` wraps `mysqli_real_escape_string` recursively. All user input goes through this before being interpolated into SQL strings (no prepared statements are used).

**Type conversion**: `ajustaColumnasFormatoJSON()` converts MySQL types to proper JSON types (int, float, bool, ISO-8601 dates). Always call this when building a response row.

**IMPORTANT**: Never add a blank line or whitespace before `<?php` in any PHP file — it breaks `header()` calls (HTTP headers must be sent before any output).

### Frontend — `frontend/`

- `index.html`: full app shell — login/register screen, sidebar nav, main content area `<div id="content">`, and all modals declared inline
- `js/app.js`: entire SPA logic (~1700 lines). Key globals:
  - `BASE_URL = 'http://localhost/homney/backend'` — change this if deploying elsewhere
  - `state = { user, hogar, page }` — the only global state
  - `api(endpoint, method, body, params)` — central fetch wrapper; returns parsed JSend JSON
  - `getData(res)` — normalises the response, returns `[]` on no data or error
- `css/styles.css`: all styling

**Navigation**: `navigate(page)` swaps `#content` innerHTML by calling a `render*()` function per page. Pages: `dashboard`, `hogar`, `tareas`, `cartera`, `muro`, `perfil`, `categorias`.

**Auth**: Login fetches the user by email, then compares the SHA-256 hash client-side (`crypto.subtle`). There is no session token — the logged-in user is kept only in `state.user` (in-memory). Refresh = logout.

**Roles**: `admin` vs `miembro`. Admin-only UI elements use the CSS class `.admin-only` and are toggled by `updateSidebar()`. Only admins can create/edit/delete rooms and change member roles.

**Expense splitting**: When a new `GASTO` is created, `crearReparto()` is called automatically. It divides the total equally among all household members and POSTs one `REPARTO_GASTO` row per user.

**Avatar upload**: Uses a separate `fetch` with `FormData` (not the `api()` helper) to `upload_avatar.php`. Stored path is relative: `uploads/perfiles/<filename>`. The frontend reconstructs the full URL as `${BASE_URL}/../${u.avatar}`.

### Database — `database/`

- `homney_dev.sql` — drops and recreates the full schema with all foreign keys. Safe to re-run from scratch.
- `datos_prueba.sql` — sample data for development.

**Key relationships:**
- Everything is scoped to a `HOGAR`. A `USUARIO` belongs to exactly one `HOGAR`.
- `HOGAR.clave_inv` is the invitation code (unique); new users join a household by entering it.
- `TAREA` → `ASIGNACION_TAREA` (many-to-many with USUARIO).
- `GASTO` → `REPARTO_GASTO` (many-to-many with USUARIO, tracks who paid and who owes).
- `CATEGORIA.categ_padre` is a self-referential FK (parent category). `categ_padre = NULL` means top-level category.
- Passwords stored as SHA-256 hex strings in `USUARIO.clave`.

### Android — `android/`

Only design artefacts (ER diagram, flow diagram) exist here. The Android project has not been started yet.
