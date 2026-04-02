/* ═══════════════════════════════════════════════════════════
   HOMNEY — app.js  v2
   Todos los métodos GET / POST / PUT / DELETE conectados
   al backend PHP via fetch. El backend responde con:
     { status: 'success'|'fail'|'error', data: ... }
   SINDATOS = null en PHP → data será null en JSON.
═══════════════════════════════════════════════════════════ */

const BASE_URL = 'http://localhost/homney/backend';

/* ── ESTADO GLOBAL ─────────────────────────────────────────── */
let state = {
  user:  null,   // objeto USUARIO logueado
  hogar: null,   // objeto HOGAR
  page:  'dashboard'
};

/* ══════════════════════════════════════════════════════════════
   API HELPER
══════════════════════════════════════════════════════════════ */
async function api(endpoint, method = 'GET', body = null, params = {}) {
  let url = `${BASE_URL}/${endpoint}`;
  if (Object.keys(params).length) url += '?' + new URLSearchParams(params);
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body && (method === 'POST' || method === 'PUT')) opts.body = JSON.stringify(body);
  try {
    const r    = await fetch(url, opts);
    const json = await r.json();
    return json;
  } catch (e) {
    return { status: 'error', data: 'Error de red: ' + e.message };
  }
}

/* Normaliza la respuesta: devuelve [] cuando no hay datos */
function getData(res) {
  if (res.status !== 'success') return [];
  if (!res.data || !Array.isArray(res.data)) return [];
  return res.data;
}

/* SHA-256 para contraseña */
const sha256 = async (msg) => {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(msg));
  return [...new Uint8Array(buf)].map(b => b.toString(16).padStart(2,'0')).join('');
};

/* ══════════════════════════════════════════════════════════════
   TOAST
══════════════════════════════════════════════════════════════ */
let _toastTimer;
function showToast(msg, type = 'ok') {
  const t = document.getElementById('toast');
  const icons = { ok: '✅', err: '❌', warn: '⚠️' };
  t.innerHTML = `<span>${icons[type] || ''}</span><span>${msg}</span>`;
  t.className = `show ${type}`;
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => t.className = '', 3500);
}

/* ══════════════════════════════════════════════════════════════
   MODAL GENÉRICO
══════════════════════════════════════════════════════════════ */
function openModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }

document.addEventListener('click', e => {
  document.querySelectorAll('.modal-overlay.open').forEach(o => {
    if (e.target === o) o.classList.remove('open');
  });
});

/* ══════════════════════════════════════════════════════════════
   DROPDOWN USUARIO (sidebar)
══════════════════════════════════════════════════════════════ */
function toggleDropdown() {
  document.getElementById('user-dropdown').classList.toggle('open');
}
document.addEventListener('click', e => {
  const wrap = document.getElementById('user-dropdown-wrap');
  if (wrap && !wrap.contains(e.target))
    document.getElementById('user-dropdown')?.classList.remove('open');
});

/* ══════════════════════════════════════════════════════════════
   LOGIN / REGISTRO
══════════════════════════════════════════════════════════════ */
function switchAuthTab(tab) {
  document.querySelectorAll('.login-tab').forEach(t =>
    t.classList.toggle('active', t.dataset.tab === tab));
  document.getElementById('panel-login').classList.toggle('hidden', tab !== 'login');
  document.getElementById('panel-registro').classList.toggle('hidden', tab !== 'registro');
}

async function doLogin() {
  const email = document.getElementById('login-email').value.trim();
  const pass  = document.getElementById('login-pass').value;
  if (!email || !pass) { showToast('Rellena email y contraseña', 'err'); return; }

  const btn = document.getElementById('btn-login');
  btn.innerHTML = '<span class="spinner"></span> Entrando...'; btn.disabled = true;

  const hash = await sha256(pass);
  const res  = await api('usuario.php', 'GET', null, { email });

  btn.innerHTML = '🐝 Entrar'; btn.disabled = false;

  if (res.status !== 'success' || !res.data) {
    showToast('Usuario no encontrado', 'err'); return;
  }

  const u = Array.isArray(res.data) ? res.data[0] : res.data;
  state.user = u;
  await loadHogar();
  enterApp();
}

async function doRegister() {
  const nombre   = document.getElementById('reg-nombre').value.trim();
  const email    = document.getElementById('reg-email').value.trim();
  const telefono = document.getElementById('reg-telefono').value.trim();
  const pass     = document.getElementById('reg-pass').value;
  const sexo     = document.getElementById('reg-sexo').value; // masculino|femenino|otro
  const modo     = document.getElementById('reg-modo').value; // 'nuevo' | 'unirse'
  const claveInv = document.getElementById('reg-clave').value.trim();

  if (!nombre || !email || !telefono || !pass || !sexo) {
    showToast('Rellena todos los campos obligatorios', 'err'); return;
  }

  const btn = document.getElementById('btn-register');
  btn.innerHTML = '<span class="spinner"></span> Registrando...'; btn.disabled = true;

  const hash = await sha256(pass);
  let id_hogar;

  if (modo === 'nuevo') {
    const clave = 'H' + Math.random().toString(36).substring(2, 8).toUpperCase();
    const rHogar = await api('hogar.php', 'POST', { clave_inv: clave });
    if (rHogar.status !== 'success') {
      showToast('Error creando el hogar: ' + (rHogar.data || ''), 'err');
      btn.innerHTML = '🏠 Crear cuenta'; btn.disabled = false; return;
    }
    id_hogar = rHogar.data.autoincrement;
    showToast(`Hogar creado. Tu clave de invitación: ${clave}`, 'warn');
  } else {
    if (!claveInv) { showToast('Introduce la clave de invitación', 'err'); btn.innerHTML = '🏠 Crear cuenta'; btn.disabled = false; return; }
    const rHogar = await api('hogar.php', 'GET', null, { clave_inv: claveInv.toUpperCase() });
    if (rHogar.status !== 'success' || !rHogar.data) {
      showToast('Clave de invitación no válida', 'err');
      btn.innerHTML = '🏠 Crear cuenta'; btn.disabled = false; return;
    }
    const h = Array.isArray(rHogar.data) ? rHogar.data[0] : rHogar.data;
    id_hogar = h.id_hogar;
  }

  const rUser = await api('usuario.php', 'POST', {
    nombre, email, telefono_movil: telefono,
    clave: hash, sexo, id_hogar, rol: modo === 'nuevo' ? 'admin' : 'miembro'
  });

  btn.innerHTML = '🏠 Crear cuenta'; btn.disabled = false;

  if (rUser.status !== 'success') {
    showToast('Error al crear usuario: ' + (rUser.data || ''), 'err'); return;
  }

  showToast('¡Cuenta creada! Inicia sesión 🎉', 'ok');
  switchAuthTab('login');
  document.getElementById('login-email').value = email;
}

async function loadHogar() {
  if (!state.user?.id_hogar) return;
  const res = await api('hogar.php', 'GET', null, { id_hogar: state.user.id_hogar });
  if (res.status === 'success' && res.data)
    state.hogar = Array.isArray(res.data) ? res.data[0] : res.data;
}

function enterApp() {
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  updateSidebar();
  navigate('dashboard');
}

/* ── Avatar helpers ──────────────────────────────────────── */
function getAvatarUrl(u) {
  if (!u || !u.avatar) return '';
  // Si ya es una URL completa, usarla directamente
  if (u.avatar.startsWith('http')) return u.avatar;
  // Ruta relativa desde el backend: "uploads/perfiles/XXX.jpg"
  return `${BASE_URL}/../${u.avatar}`;
}

function setAvatarElement(textEl, imgEl, u) {
  const ini = u.nombre.charAt(0).toUpperCase();
  const url = getAvatarUrl(u);
  if (url && u.avatar !== 'uploads/perfiles/default.png') {
    textEl.style.display = 'none';
    imgEl.src = url;
    imgEl.style.display = 'inline-block';
  } else {
    textEl.textContent = ini;
    textEl.style.display = 'flex';
    imgEl.style.display = 'none';
  }
}

function updateSidebar() {
  const u = state.user;
  document.getElementById('sidebar-name').textContent = u.nombre;
  document.getElementById('sidebar-role').textContent =
    u.rol === 'admin' ? '👑 Admin' : '🧑 Miembro';

  // Avatar en sidebar
  setAvatarElement(
    document.getElementById('sidebar-avatar'),
    document.getElementById('sidebar-avatar-img'),
    u
  );
  // Avatar en topbar
  const ini = u.nombre.charAt(0).toUpperCase();
  document.getElementById('topbar-avatar').textContent = ini;
  const topImgEl = document.getElementById('topbar-avatar-img');
  const url = getAvatarUrl(u);
  if (url && u.avatar !== 'uploads/perfiles/default.png') {
    document.getElementById('topbar-avatar').style.display = 'none';
    topImgEl.src = url;
    topImgEl.style.display = 'inline-block';
  } else {
    document.getElementById('topbar-avatar').style.display = 'flex';
    topImgEl.style.display = 'none';
  }

  // Mostrar/ocultar opciones de admin en sidebar
  const isAdmin = u.rol === 'admin';
  document.querySelectorAll('.admin-only').forEach(el =>
    el.classList.toggle('hidden', !isAdmin)
  );
}

function doLogout() {
  state = { user: null, hogar: null, page: 'dashboard' };
  document.getElementById('app').classList.add('hidden');
  document.getElementById('login-screen').classList.remove('hidden');
  document.getElementById('login-pass').value = '';
  document.getElementById('user-dropdown')?.classList.remove('open');
}

/* ══════════════════════════════════════════════════════════════
   NAVEGACIÓN
══════════════════════════════════════════════════════════════ */
const PAGE_TITLES = {
  dashboard:  '🐝 Dashboard',
  hogar:      '🏠 Mi Hogar',
  tareas:     '✅ Tareas',
  cartera:    '💰 Mi Cartera',
  muro:       '📢 Muro del Hogar',
  perfil:     '👤 Mi Perfil',
};

function navigate(page) {
  state.page = page;
  document.querySelectorAll('.nav-item[data-page]').forEach(el =>
    el.classList.toggle('active', el.dataset.page === page));
  document.querySelectorAll('.bnav-item[data-page]').forEach(el =>
    el.classList.toggle('active', el.dataset.page === page));
  document.getElementById('topbar-title').textContent = PAGE_TITLES[page] || page;
  document.getElementById('topbar-actions').innerHTML = '';
  document.getElementById('content').innerHTML =
    '<div class="loading-block"><span class="spinner"></span> Cargando...</div>';

  const pages = {
    dashboard, hogar: renderHogar, tareas: renderTareas,
    cartera: renderCartera, muro: renderMuro, perfil: renderPerfil,
  };
  if (pages[page]) pages[page]();
  document.getElementById('sidebar').classList.remove('open');
  const overlay = document.getElementById('sidebar-overlay');
  if (overlay) overlay.classList.remove('open');
}

/* ── helpers de render ──────────────────────────────────── */
function set(html) {
  document.getElementById('content').innerHTML = `<div class="page">${html}</div>`;
}
function fmtDate(d) {
  if (!d) return '—';
  try { return new Date(d).toLocaleDateString('es-ES', { day:'2-digit', month:'short', year:'numeric' }); }
  catch { return d; }
}
function fmtAmt(n) {
  const v = parseFloat(n);
  return isNaN(v) ? '0,00 €' : v.toLocaleString('es-ES', { minimumFractionDigits:2, maximumFractionDigits:2 }) + ' €';
}

const ICONOS_HAB  = { cocina:'🍳', aseo:'🚿', garaje:'🚗', exterior:'🌿', generica:'🛋️' };
const ICONOS_MODO = { efectivo:'💵', transferencia:'🏦', tarjeta:'💳', bizum:'📱' };
const FREQ_LABEL  = { dia:'Diaria', semana:'Semanal', mes:'Mensual', variable:'Variable' };

/* Paleta de colores por usuario para el calendario */
const USER_COLORS = [
  '#f59e0b','#10b981','#3b82f6','#ef4444',
  '#8b5cf6','#ec4899','#06b6d4','#84cc16'
];

/* ══════════════════════════════════════════════════════════════
   CALENDARIO — componente reutilizable
══════════════════════════════════════════════════════════════ */
/**
 * Genera el HTML de un calendario mensual.
 * @param {Array} tareas - array de tareas con frecuencia, id_tarea, nombre
 * @param {Array} asignaciones - array de {id_tarea, id_usuario}
 * @param {Array} usuarios - array de {id_usuario, nombre}
 * @param {Object} userColorMap - { id_usuario: '#hexcolor' }
 */
function buildCalendarHTML(tareas, asignaciones, usuarios, userColorMap) {
  const now   = new Date();
  const year  = now.getFullYear();
  const month = now.getMonth();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayJS  = new Date(year, month, 1).getDay(); // 0=Dom
  // Convertir a lunes-first
  const startPad = (firstDayJS === 0) ? 6 : firstDayJS - 1;

  const monthNames = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
                      'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];
  const dayNames   = ['Lun','Mar','Mié','Jue','Vie','Sáb','Dom'];

  // Mapa id_tarea → tarea
  const tareaMap = Object.fromEntries(tareas.map(t => [t.id_tarea, t]));

  // Generar eventos por día
  const dayEvents = {};
  for (let d = 1; d <= daysInMonth; d++) dayEvents[d] = [];

  asignaciones.forEach(asig => {
    const tarea = tareaMap[asig.id_tarea];
    if (!tarea) return;
    const color    = userColorMap[asig.id_usuario] || '#9ca3af';
    const user     = usuarios.find(u => u.id_usuario == asig.id_usuario);
    const userName = user ? user.nombre : '?';
    const numVeces = parseInt(tarea.num_veces) || 1;

    for (let d = 1; d <= daysInMonth; d++) {
      let add = false;
      switch (tarea.frecuencia) {
        case 'dia':
          add = true; break;
        case 'semana':
          // Cada 7 días. Primera ocurrencia: día 1 + offset para el día de semana
          // Simplificación: aparece en 1, 8, 15, 22, 29
          add = ((d - 1) % 7 === 0); break;
        case 'mes':
          add = (d === 1); break;
        case 'variable':
          add = (d === 1); break;
      }
      if (add) {
        dayEvents[d].push({ tarea, color, userName, numVeces });
      }
    }
  });

  // Leyenda de usuarios
  const legendHTML = usuarios
    .filter(u => userColorMap[u.id_usuario])
    .map(u => `
      <span class="cal-legend-item">
        <span class="cal-dot" style="background:${userColorMap[u.id_usuario]}"></span>
        ${escHtml(u.nombre)}
      </span>`).join('');

  // Cabecera de días
  const headerHTML = dayNames.map(d => `<div class="cal-header-cell">${d}</div>`).join('');

  // Celdas vacías antes del primer día
  const emptyCells = Array(startPad).fill('<div class="cal-cell cal-empty"></div>').join('');

  // Celdas de días
  const today = now.getDate();
  const todayMonth = now.getMonth();
  const todayYear  = now.getFullYear();

  const dayCells = Array.from({ length: daysInMonth }, (_, i) => {
    const d = i + 1;
    const isToday = d === today && month === todayMonth && year === todayYear;
    const events  = dayEvents[d];
    const shown   = events.slice(0, 3);
    const extra   = events.length - 3;
    return `
      <div class="cal-cell${isToday ? ' cal-today' : ''}">
        <div class="cal-day-num">${d}</div>
        <div class="cal-events">
          ${shown.map(e => `
            <div class="cal-event" style="background:${e.color}"
                 title="${escHtml(e.tarea.nombre)} — ${escHtml(e.userName)}">
              ${escHtml(e.tarea.nombre.length > 12 ? e.tarea.nombre.substring(0,11)+'…' : e.tarea.nombre)}
            </div>`).join('')}
          ${extra > 0 ? `<div class="cal-more">+${extra} más</div>` : ''}
        </div>
      </div>`;
  }).join('');

  return `
    <div class="calendar-wrap">
      <div class="calendar-header-row">
        <span class="calendar-month-title">📅 ${monthNames[month]} ${year}</span>
      </div>
      <div class="cal-legend">${legendHTML}</div>
      <div class="calendar-grid">
        ${headerHTML}
        ${emptyCells}
        ${dayCells}
      </div>
    </div>`;
}

/* ══════════════════════════════════════════════════════════════
   DASHBOARD
══════════════════════════════════════════════════════════════ */
async function dashboard() {
  const uid      = state.user.id_usuario;
  const id_hogar = state.user.id_hogar;

  const [rGastos, rAsig, rPubs, rAllAsig, rTareas, rUsuarios] = await Promise.all([
    api('gasto.php',            'GET', null, { id_hogar }),
    api('asignacion_tarea.php', 'GET', null, { id_usuario: uid }),
    api('muro.php',             'GET', null, { id_hogar }),
    api('asignacion_tarea.php', 'GET', null, {}),
    api('tarea.php',            'GET', null, { id_hogar }),
    api('usuario.php',          'GET', null, { id_hogar }),
  ]);

  const gastos   = getData(rGastos);
  const asig     = getData(rAsig);
  const pubs     = getData(rPubs);
  const allAsig  = getData(rAllAsig);
  const tareas   = getData(rTareas);
  const usuarios = getData(rUsuarios);

  const totalGasto = gastos.reduce((s,g) => s + parseFloat(g.importe||0), 0);
  const ultG  = gastos.slice(0,4);
  const ultP  = pubs.slice(0,3);

  // Mapa de colores por usuario
  const userColorMap = {};
  usuarios.forEach((u, i) => { userColorMap[u.id_usuario] = USER_COLORS[i % USER_COLORS.length]; });

  // Filtrar asignaciones que correspondan a tareas del hogar
  const tareaIds = new Set(tareas.map(t => t.id_tarea));
  const asigDelHogar = allAsig.filter(a => tareaIds.has(a.id_tarea));

  document.getElementById('topbar-actions').innerHTML = `
    <button class="btn btn-ghost btn-sm" onclick="openModalTarea()">＋ Tarea</button>
    <button class="btn btn-primary btn-sm" onclick="openModalGasto()">💰 Gasto</button>
  `;

  const calendarHTML = buildCalendarHTML(tareas, asigDelHogar, usuarios, userColorMap);
  const userMap = Object.fromEntries(usuarios.map(u => [u.id_usuario, u]));

  set(`
    <div class="honey-strip"></div>

    <!-- STATS -->
    <div class="grid-3 section-gap">
      <div class="stat-card">
        <div class="stat-icon">💸</div>
        <div class="stat-value">${fmtAmt(totalGasto)}</div>
        <div class="stat-label">Gasto total del hogar</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-value">${asig.length}</div>
        <div class="stat-label">Tareas asignadas a ti</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📢</div>
        <div class="stat-value">${pubs.length}</div>
        <div class="stat-label">Publicaciones del hogar</div>
      </div>
    </div>

    <!-- CALENDARIO -->
    <div class="section-title">📅 Calendario del hogar</div>
    ${calendarHTML}

    <!-- GRID -->
    <div class="grid-2 section-gap">
      <!-- Últimos gastos -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">💸 Últimos gastos</span>
          <span class="card-action" onclick="navigate('cartera')">Ver todos →</span>
        </div>
        ${ultG.length === 0
          ? '<div class="empty"><span class="empty-icon">📭</span><p>Sin gastos aún</p></div>'
          : ultG.map(g => `
            <div class="list-item">
              <div class="list-item-icon">${ICONOS_MODO[g.modo]||'💰'}</div>
              <div class="list-item-body">
                <div class="list-item-title">${g.concepto}</div>
                <div class="list-item-sub">${g.categoria||'—'} · ${fmtDate(g.fecha)}</div>
              </div>
              <div class="list-item-right">
                <div class="list-item-amount neg">${fmtAmt(g.importe)}</div>
              </div>
            </div>`).join('')
        }
        <div style="margin-top:14px">
          <button class="btn btn-ghost btn-sm" onclick="openModalGasto()">＋ Nuevo gasto</button>
        </div>
      </div>

      <!-- Mis tareas -->
      <div class="card">
        <div class="card-header">
          <span class="card-title">✅ Mis tareas</span>
          <span class="card-action" onclick="navigate('tareas')">Ver todas →</span>
        </div>
        ${asig.length === 0
          ? '<div class="empty"><span class="empty-icon">🎉</span><p>¡Sin tareas pendientes!</p></div>'
          : asig.slice(0,4).map(a => {
              const t = tareas.find(t => t.id_tarea == a.id_tarea);
              return `
              <div class="tarea-item">
                <div class="tarea-check"></div>
                <div class="tarea-body">
                  <div class="tarea-name">${t ? t.nombre : 'Tarea #'+a.id_tarea}</div>
                  <div class="tarea-freq">${t ? (FREQ_LABEL[t.frecuencia]||t.frecuencia) : ''}</div>
                </div>
              </div>`;
            }).join('')
        }
        <div style="margin-top:14px">
          <button class="btn btn-ghost btn-sm" onclick="openModalTarea()">＋ Nueva tarea</button>
        </div>
      </div>
    </div>

    <!-- MURO -->
    <div class="section-title">📢 Últimas publicaciones</div>
    ${ultP.length === 0
      ? '<div class="empty"><span class="empty-icon">📭</span><p>El muro está vacío</p></div>'
      : ultP.map(p => {
          const autor  = userMap[p.id_usuario];
          const ini    = autor ? autor.nombre.charAt(0).toUpperCase() : 'U';
          const nombre = autor ? autor.nombre : `Usuario #${p.id_usuario}`;
          return `
          <div class="pub-card">
            <div class="pub-header">
              <div class="pub-avatar">${ini}</div>
              <div class="pub-meta">
                <div class="pub-user">${escHtml(nombre)}</div>
                <div class="pub-date">${fmtDate(p.fecha_pub)}</div>
              </div>
            </div>
            <div class="pub-title">${escHtml(p.titulo)}</div>
            <div class="pub-body">${escHtml(p.cuerpo)}</div>
          </div>`;
        }).join('')
    }
    ${ultP.length > 0 ? '<button class="btn btn-ghost btn-sm" onclick="navigate(\'muro\')">Ver todo el muro →</button>' : ''}
  `);
}

/* ══════════════════════════════════════════════════════════════
   MI HOGAR — habitaciones y compañeros
══════════════════════════════════════════════════════════════ */
async function renderHogar() {
  const id_hogar = state.user.id_hogar;
  const isAdmin  = state.user.rol === 'admin';

  const [rHabs, rUsuarios, rAllAsig, rTareas] = await Promise.all([
    api('habitacion.php',       'GET', null, { id_hogar }),
    api('usuario.php',          'GET', null, { id_hogar }),
    api('asignacion_tarea.php', 'GET', null, {}),
    api('tarea.php',            'GET', null, { id_hogar }),
  ]);
  const habs     = getData(rHabs);
  const usuarios = getData(rUsuarios);
  const allAsig  = getData(rAllAsig);
  const tareas   = getData(rTareas);

  // Mapa de colores por usuario
  const userColorMap = {};
  usuarios.forEach((u, i) => { userColorMap[u.id_usuario] = USER_COLORS[i % USER_COLORS.length]; });

  // Filtrar asignaciones del hogar
  const tareaIds = new Set(tareas.map(t => t.id_tarea));
  const asigDelHogar = allAsig.filter(a => tareaIds.has(a.id_tarea));

  // Botones topbar: solo admin puede crear habitación
  document.getElementById('topbar-actions').innerHTML = isAdmin
    ? `<button class="btn btn-primary btn-sm" onclick="openModalHabitacion()">＋ Habitación</button>`
    : '';

  const calendarHTML = buildCalendarHTML(tareas, asigDelHogar, usuarios, userColorMap);

  set(`
    <!-- Info hogar -->
    <div class="card section-gap" style="display:flex;align-items:center;gap:16px;flex-wrap:wrap">
      <div style="font-size:2.5rem">🏠</div>
      <div style="flex:1">
        <div style="font-weight:800;font-size:1.05rem">Hogar #${id_hogar}</div>
        <div style="font-size:.82rem;color:var(--muted);margin-top:4px">
          Clave de invitación:
          <code style="background:var(--accent-bg);padding:3px 10px;border-radius:6px;font-weight:700;color:var(--accent-dark)">
            ${state.hogar?.clave_inv || '—'}
          </code>
          <span style="font-size:.75rem;color:var(--muted)"> — compártela para que otros se unan</span>
        </div>
      </div>
      <span class="tag tag-yellow">${usuarios.length} compañero${usuarios.length!==1?'s':''}</span>
      ${isAdmin ? `<span class="tag tag-green" style="cursor:pointer" onclick="showToast('Comparte la clave de invitación con tus compañeros','ok')">👑 Eres administrador</span>` : ''}
    </div>

    <!-- Compañeros -->
    <div class="section-title">🐝 Compañeros de hogar</div>
    <div class="grid-4 section-gap">
      ${usuarios.length === 0
        ? '<div class="empty"><p>Sin compañeros registrados</p></div>'
        : usuarios.map(u => {
            const color  = userColorMap[u.id_usuario];
            const avatarUrl = getAvatarUrl(u);
            const avatarHTML = (avatarUrl && u.avatar !== 'uploads/perfiles/default.png')
              ? `<img src="${avatarUrl}" alt="${escHtml(u.nombre)}" class="avatar avatar-lg" style="object-fit:cover">`
              : `<div class="avatar avatar-lg" style="background:${color}">${u.nombre.charAt(0).toUpperCase()}</div>`;
            return `
            <div class="companion-card">
              ${avatarHTML}
              <div style="font-weight:700;font-size:.9rem">${escHtml(u.nombre)}</div>
              <div style="font-size:.74rem;color:var(--muted)">${escHtml(u.email)}</div>
              <span class="tag ${u.rol==='admin'?'tag-yellow':'tag-green'}">${u.rol}</span>
              ${isAdmin && u.id_usuario != state.user.id_usuario ? `
                <button class="btn btn-ghost btn-sm" onclick="cambiarRol(${u.id_usuario},'${u.rol}')">
                  ${u.rol==='admin' ? '⬇️ Hacer miembro' : '⬆️ Hacer admin'}
                </button>` : ''}
            </div>`;
          }).join('')
      }
    </div>

    <!-- Habitaciones -->
    <div class="section-title">🛋️ Habitaciones</div>
    <div class="grid-4 section-gap">
      ${habs.map(h => `
        <div class="hab-card">
          <div class="hab-icon">${ICONOS_HAB[h.tipo]||'🚪'}</div>
          <div class="hab-name">${escHtml(h.nombre)}</div>
          <div class="hab-meta">${h.tipo}</div>
          ${isAdmin ? `
          <div class="hab-actions">
            <button class="btn btn-icon btn-sm" onclick="openEditHabitacion(${h.id_habitacion},'${escHtml(h.nombre)}','${h.tipo}')" title="Editar">✏️</button>
            <button class="btn btn-danger btn-sm" onclick="deleteHabitacion(${h.id_habitacion})" title="Eliminar">🗑️</button>
          </div>` : ''}
        </div>`).join('')
      }
      ${isAdmin ? `
      <div class="hab-card add-card" onclick="openModalHabitacion()">
        <div style="font-size:1.6rem;color:var(--muted)">＋</div>
        <div style="font-size:.8rem;color:var(--muted);font-weight:600">Añadir habitación</div>
      </div>` : ''}
    </div>

    <!-- Calendario del hogar -->
    <div class="section-title">📅 Calendario del hogar — ${new Date().toLocaleDateString('es-ES',{month:'long',year:'numeric'})}</div>
    ${calendarHTML}
  `);
}

/* Cambiar rol de un usuario (solo admin) */
async function cambiarRol(id_usuario, rolActual) {
  const nuevoRol = rolActual === 'admin' ? 'miembro' : 'admin';
  if (!confirm(`¿Cambiar rol a ${nuevoRol}?`)) return;
  const res = await api('usuario.php','PUT',{ id_usuario, rol: nuevoRol });
  if (res.status === 'success') { showToast('Rol actualizado ✓','ok'); renderHogar(); }
  else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   TAREAS
══════════════════════════════════════════════════════════════ */
async function renderTareas() {
  const uid      = state.user.id_usuario;
  const id_hogar = state.user.id_hogar;
  const isAdmin  = state.user.rol === 'admin';

  const [rAsig, rHabs, rTareas, rAllAsig, rUsuarios] = await Promise.all([
    api('asignacion_tarea.php', 'GET', null, { id_usuario: uid }),
    api('habitacion.php',       'GET', null, { id_hogar }),
    api('tarea.php',            'GET', null, { id_hogar }),
    api('asignacion_tarea.php', 'GET', null, {}),
    api('usuario.php',          'GET', null, { id_hogar }),
  ]);
  const asig     = getData(rAsig);
  const habs     = getData(rHabs);
  const tareas   = getData(rTareas);
  const allAsig  = getData(rAllAsig);
  const usuarios = getData(rUsuarios);

  const userMap = Object.fromEntries(usuarios.map(u => [u.id_usuario, u]));
  const userColorMap = {};
  usuarios.forEach((u, i) => { userColorMap[u.id_usuario] = USER_COLORS[i % USER_COLORS.length]; });

  // IDs de tareas asignadas al usuario actual
  const idAsignadasAlMio = new Set(asig.map(a => a.id_tarea));

  // Para cada tarea, saber quién está asignado
  const asigByTarea = {};
  allAsig.forEach(a => {
    if (!asigByTarea[a.id_tarea]) asigByTarea[a.id_tarea] = [];
    asigByTarea[a.id_tarea].push(a.id_usuario);
  });

  document.getElementById('topbar-actions').innerHTML = `
    <button class="btn btn-primary btn-sm" onclick="openModalTarea()">＋ Nueva Tarea</button>
  `;

  function tareaRow(t, esMia) {
    const asignados = (asigByTarea[t.id_tarea] || []).map(uid => {
      const u = userMap[uid];
      const color = userColorMap[uid] || '#9ca3af';
      return u
        ? `<span class="user-badge" style="background:${color}">${u.nombre.charAt(0).toUpperCase()}</span>`
        : `<span class="user-badge">#${uid}</span>`;
    }).join('');

    return `
      <div class="tarea-item">
        ${esMia ? `<div class="tarea-check" title="Marcar como realizada" onclick="marcarRealizada(${t.id_tarea},this)"></div>` : `<div style="width:22px;height:22px;border-radius:50%;border:2px solid var(--border2);flex-shrink:0"></div>`}
        <div class="tarea-body">
          <div class="tarea-name">${escHtml(t.nombre)}</div>
          <div class="tarea-freq">
            ${FREQ_LABEL[t.frecuencia]||t.frecuencia}
            ${t.num_veces > 1 ? '· '+t.num_veces+'x' : ''}
            · ${t.duracion ? t.duracion+' min' : 'Sin duración'}
            ${t.categoria ? `· <span class="tag tag-yellow" style="font-size:.68rem">${escHtml(t.categoria)}</span>` : ''}
          </div>
          <div style="margin-top:4px">${asignados}</div>
        </div>
        <div class="tarea-actions">
          <span class="tag tag-green">${FREQ_LABEL[t.frecuencia]||t.frecuencia}</span>
          ${!esMia ? `<button class="btn btn-green btn-sm" onclick="asignarTarea(${t.id_tarea})">Asignarme</button>` : ''}
          ${isAdmin || esMia ? `<button class="btn btn-icon btn-sm" onclick="openEditTarea(${JSON.stringify(t).replace(/"/g,'&quot;')})" title="Editar">✏️</button>` : ''}
          ${isAdmin ? `<button class="btn btn-danger btn-sm" onclick="deleteTarea(${t.id_tarea})" title="Eliminar">🗑️</button>` : ''}
        </div>
      </div>`;
  }

  const misTareas    = tareas.filter(t => idAsignadasAlMio.has(t.id_tarea));
  const otrasTareas  = tareas.filter(t => !idAsignadasAlMio.has(t.id_tarea));

  set(`
    <!-- Mis tareas -->
    <div class="card section-gap">
      <div class="card-header">
        <span class="card-title">🐝 Mis tareas asignadas</span>
        <span class="tag tag-yellow">${misTareas.length} tarea${misTareas.length!==1?'s':''}</span>
      </div>
      ${misTareas.length === 0
        ? '<div class="empty"><span class="empty-icon">🎉</span><p>¡Todo al día! Sin tareas pendientes.</p></div>'
        : misTareas.map(t => tareaRow(t, true)).join('')
      }
    </div>

    <!-- Otras tareas del hogar -->
    <div class="section-title">🏠 Otras tareas del hogar</div>
    <div class="card">
      ${otrasTareas.length === 0
        ? '<div class="empty"><span class="empty-icon">🐝</span><p>No hay más tareas disponibles en el hogar</p></div>'
        : otrasTareas.map(t => tareaRow(t, false)).join('')
      }
    </div>
  `);
}

async function marcarRealizada(id_tarea, el) {
  const ahora = new Date().toISOString().slice(0,19).replace('T',' ');
  const res = await api('tareas_realizadas.php','POST',{
    id_tarea, id_usuario: state.user.id_usuario, fecha_realizacion: ahora
  });
  if (res.status === 'success') {
    el.classList.add('done'); el.textContent = '✓';
    showToast('¡Tarea marcada como realizada! 🎉','ok');
    setTimeout(renderTareas, 600);
  } else showToast('Error al marcar la tarea','err');
}

async function asignarTarea(id_tarea) {
  const res = await api('asignacion_tarea.php','POST',{
    id_tarea, id_usuario: state.user.id_usuario
  });
  if (res.status === 'success') {
    showToast('Tarea asignada 🐝','ok'); renderTareas();
  } else showToast('Error: '+(res.data||''),'err');
}

async function deleteTarea(id_tarea) {
  if (!confirm('¿Eliminar esta tarea y sus asignaciones?')) return;
  await api('asignacion_tarea.php','DELETE',null,{ id_tarea });
  const res = await api('tarea.php','DELETE',null,{ id_tarea });
  if (res.status === 'success') { showToast('Tarea eliminada','ok'); renderTareas(); }
  else showToast('Error al eliminar','err');
}

/* ══════════════════════════════════════════════════════════════
   CARTERA — gastos + reparto
══════════════════════════════════════════════════════════════ */
async function renderCartera() {
  const uid      = state.user.id_usuario;
  const id_hogar = state.user.id_hogar;

  const [rGastos, rMisRepartos, rUsuarios] = await Promise.all([
    api('gasto.php',         'GET', null, { id_hogar }),
    api('reparto_gasto.php', 'GET', null, { id_usuario: uid }),
    api('usuario.php',       'GET', null, { id_hogar }),
  ]);

  const gastos      = getData(rGastos);
  const misRepartos = getData(rMisRepartos);
  const usuarios    = getData(rUsuarios);

  const gastoMap = Object.fromEntries(gastos.map(g => [g.id_gasto, g]));
  const userMap  = Object.fromEntries(usuarios.map(u => [u.id_usuario, u]));

  const misGastos = gastos.filter(g => g.id_usuario_pagador == uid);
  let repartosDeudoresByGasto = {};
  if (misGastos.length > 0) {
    const results = await Promise.all(
      misGastos.map(g => api('reparto_gasto.php','GET',null,{ id_gasto: g.id_gasto }))
    );
    misGastos.forEach((g, i) => {
      repartosDeudoresByGasto[g.id_gasto] = getData(results[i])
        .filter(r => !r.pagador && r.id_usuario != uid);
    });
  }

  const meDebenMap = {};
  misGastos.forEach(g => {
    (repartosDeudoresByGasto[g.id_gasto] || []).forEach(r => {
      const key = r.id_usuario;
      if (!meDebenMap[key]) meDebenMap[key] = { user: userMap[key], items: [], totalPendiente: 0, totalAbonado: 0 };
      meDebenMap[key].items.push({ gasto: g, reparto: r });
      if (!r.abonado) meDebenMap[key].totalPendiente += parseFloat(r.importe || 0);
      else            meDebenMap[key].totalAbonado   += parseFloat(r.importe || 0);
    });
  });

  const deboRepartos = misRepartos.filter(r => !r.pagador);
  const deboMap = {};
  deboRepartos.forEach(r => {
    const g = gastoMap[r.id_gasto];
    if (!g) return;
    const pagadorId = g.id_usuario_pagador;
    if (pagadorId == uid) return;
    if (!deboMap[pagadorId]) deboMap[pagadorId] = { user: userMap[pagadorId], items: [], totalPendiente: 0, totalAbonado: 0 };
    deboMap[pagadorId].items.push({ gasto: g, reparto: r });
    if (!r.abonado) deboMap[pagadorId].totalPendiente += parseFloat(r.importe || 0);
    else            deboMap[pagadorId].totalAbonado   += parseFloat(r.importe || 0);
  });

  const totalMeDeben = Object.values(meDebenMap).reduce((s,m) => s + m.totalPendiente, 0);
  const totalDebo    = Object.values(deboMap).reduce((s,m) => s + m.totalPendiente, 0);
  const totalHogar   = gastos.reduce((s,g) => s + parseFloat(g.importe || 0), 0);
  const balance      = totalMeDeben - totalDebo;

  document.getElementById('topbar-actions').innerHTML = `
    <button class="btn btn-primary btn-sm" onclick="openModalGasto()">💰 Nuevo Gasto</button>
  `;

  function htmlMeDeben() {
    const entries = Object.values(meDebenMap);
    if (entries.length === 0) return '<div class="empty"><span class="empty-icon">🎉</span><p>¡Nadie te debe dinero!</p></div>';
    return entries.map(m => {
      const ini    = m.user ? m.user.nombre.charAt(0).toUpperCase() : '?';
      const nombre = m.user ? m.user.nombre : 'Desconocido';
      const pendientes = m.items.filter(i => !i.reparto.abonado);
      const abonados   = m.items.filter(i =>  i.reparto.abonado);
      return `
        <div class="reparto-member-block">
          <div class="reparto-member-header">
            <div class="avatar" style="width:36px;height:36px;font-size:.9rem">${ini}</div>
            <div style="flex:1">
              <div style="font-weight:700;font-size:.9rem">${escHtml(nombre)}</div>
              <div style="font-size:.75rem;color:var(--muted)">${pendientes.length} gasto${pendientes.length!==1?'s':''} pendiente${pendientes.length!==1?'s':''}</div>
            </div>
            <div style="text-align:right">
              <div style="font-weight:800;font-size:1rem;color:var(--green-dark)">+${fmtAmt(m.totalPendiente)}</div>
              ${m.totalAbonado > 0 ? `<div style="font-size:.72rem;color:var(--muted)">Cobrado: ${fmtAmt(m.totalAbonado)}</div>` : ''}
            </div>
          </div>
          <div class="reparto-items">
            ${pendientes.map(i => `
              <div class="reparto-item reparto-pendiente">
                <div class="reparto-item-icon">${ICONOS_MODO[i.gasto.modo]||'💰'}</div>
                <div class="reparto-item-body">
                  <div class="reparto-item-title">${escHtml(i.gasto.concepto)}</div>
                  <div class="reparto-item-sub">${fmtDate(i.gasto.fecha)} · ${i.gasto.categoria||'—'}</div>
                </div>
                <div class="reparto-item-right">
                  <span style="font-weight:700;color:var(--green-dark)">${fmtAmt(i.reparto.importe)}</span>
                  <button class="btn btn-green btn-sm" onclick="cobrarDeuda(${i.gasto.id_gasto},${i.reparto.id_usuario},${i.reparto.importe})">✅ Cobrado</button>
                </div>
              </div>`).join('')}
            ${abonados.map(i => `
              <div class="reparto-item reparto-abonado">
                <div class="reparto-item-icon" style="opacity:.5">${ICONOS_MODO[i.gasto.modo]||'💰'}</div>
                <div class="reparto-item-body">
                  <div class="reparto-item-title" style="color:var(--muted);text-decoration:line-through">${escHtml(i.gasto.concepto)}</div>
                  <div class="reparto-item-sub">${fmtDate(i.gasto.fecha)}</div>
                </div>
                <div class="reparto-item-right"><span class="tag tag-green">Cobrado ✓</span></div>
              </div>`).join('')}
          </div>
        </div>`;
    }).join('');
  }

  function htmlDeboYo() {
    const entries = Object.values(deboMap);
    if (entries.length === 0) return '<div class="empty"><span class="empty-icon">🎉</span><p>¡No debes nada a nadie!</p></div>';
    return entries.map(m => {
      const ini    = m.user ? m.user.nombre.charAt(0).toUpperCase() : '?';
      const nombre = m.user ? m.user.nombre : 'Desconocido';
      const pendientes = m.items.filter(i => !i.reparto.abonado);
      const abonados   = m.items.filter(i =>  i.reparto.abonado);
      return `
        <div class="reparto-member-block">
          <div class="reparto-member-header">
            <div class="avatar" style="width:36px;height:36px;font-size:.9rem">${ini}</div>
            <div style="flex:1">
              <div style="font-weight:700;font-size:.9rem">${escHtml(nombre)}</div>
              <div style="font-size:.75rem;color:var(--muted)">${pendientes.length} pago${pendientes.length!==1?'s':''} pendiente${pendientes.length!==1?'s':''}</div>
            </div>
            <div style="text-align:right">
              <div style="font-weight:800;font-size:1rem;color:var(--danger)">−${fmtAmt(m.totalPendiente)}</div>
              ${m.totalAbonado > 0 ? `<div style="font-size:.72rem;color:var(--muted)">Pagado: ${fmtAmt(m.totalAbonado)}</div>` : ''}
            </div>
          </div>
          <div class="reparto-items">
            ${pendientes.map(i => `
              <div class="reparto-item reparto-pendiente">
                <div class="reparto-item-icon">${ICONOS_MODO[i.gasto.modo]||'💰'}</div>
                <div class="reparto-item-body">
                  <div class="reparto-item-title">${escHtml(i.gasto.concepto)}</div>
                  <div class="reparto-item-sub">${fmtDate(i.gasto.fecha)} · ${i.gasto.categoria||'—'}</div>
                </div>
                <div class="reparto-item-right">
                  <span style="font-weight:700;color:var(--danger)">${fmtAmt(i.reparto.importe)}</span>
                  <button class="btn btn-primary btn-sm" onclick="abonarDeuda(${i.gasto.id_gasto},${uid},${i.reparto.importe})">💸 Ya pagué</button>
                </div>
              </div>`).join('')}
            ${abonados.map(i => `
              <div class="reparto-item reparto-abonado">
                <div class="reparto-item-icon" style="opacity:.5">${ICONOS_MODO[i.gasto.modo]||'💰'}</div>
                <div class="reparto-item-body">
                  <div class="reparto-item-title" style="color:var(--muted);text-decoration:line-through">${escHtml(i.gasto.concepto)}</div>
                  <div class="reparto-item-sub">${fmtDate(i.gasto.fecha)}</div>
                </div>
                <div class="reparto-item-right"><span class="tag tag-green">Pagado ✓</span></div>
              </div>`).join('')}
          </div>
        </div>`;
    }).join('');
  }

  set(`
    <div class="grid-3 section-gap">
      <div class="stat-card">
        <div class="stat-icon">🍯</div>
        <div class="stat-value" style="color:var(--green-dark)">${fmtAmt(totalMeDeben)}</div>
        <div class="stat-label">Te deben a ti (pendiente)</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📤</div>
        <div class="stat-value" style="color:var(--danger)">${fmtAmt(totalDebo)}</div>
        <div class="stat-label">Debes tú (pendiente)</div>
      </div>
      <div class="stat-card" style="border-color:${balance>=0?'var(--green)':'var(--danger)'}">
        <div class="stat-icon">${balance >= 0 ? '🟢' : '🔴'}</div>
        <div class="stat-value" style="color:${balance>=0?'var(--green-dark)':'var(--danger)'}">${balance>=0?'+':''}${fmtAmt(balance)}</div>
        <div class="stat-label">Balance neto</div>
      </div>
    </div>

    <div class="reparto-section section-gap">
      <div class="reparto-section-title">
        <span>🍯 Lo que te deben</span>
        <span class="tag tag-green">${fmtAmt(totalMeDeben)} pendiente</span>
      </div>
      ${htmlMeDeben()}
    </div>

    <div class="reparto-section section-gap">
      <div class="reparto-section-title">
        <span>📤 Lo que debes tú</span>
        <span class="tag tag-red">${fmtAmt(totalDebo)} pendiente</span>
      </div>
      ${htmlDeboYo()}
    </div>

    <div class="card">
      <div class="card-header">
        <span class="card-title">📊 Historial de gastos del hogar</span>
        <span class="tag tag-yellow">${gastos.length} registro${gastos.length!==1?'s':''}</span>
      </div>
      ${gastos.length === 0
        ? '<div class="empty"><span class="empty-icon">📭</span><p>Sin gastos registrados</p></div>'
        : gastos.map(g => {
            const esPropio     = g.id_usuario_pagador == uid;
            const pagadorUser  = userMap[g.id_usuario_pagador];
            const pagadorNombre = pagadorUser ? pagadorUser.nombre : `#${g.id_usuario_pagador}`;
            return `
            <div class="list-item">
              <div class="list-item-icon">${ICONOS_MODO[g.modo]||'💰'}</div>
              <div class="list-item-body">
                <div class="list-item-title">${escHtml(g.concepto)}</div>
                <div class="list-item-sub">
                  ${g.categoria||'—'} · ${fmtDate(g.fecha)} · ${g.modo}
                  · <span style="font-weight:600">${esPropio ? 'Pagaste tú' : 'Pagó '+escHtml(pagadorNombre)}</span>
                </div>
              </div>
              <div class="list-item-right">
                <div class="list-item-amount ${esPropio?'pos':'neg'}">${esPropio?'+':'−'}${fmtAmt(g.importe)}</div>
                <span class="tag ${g.tipo==='fijo'?'tag-yellow':'tag-warn'}">${g.tipo}</span>
              </div>
              <div class="list-item-actions" style="margin-left:8px">
                <button class="btn btn-icon btn-sm" onclick="openEditGasto(${JSON.stringify(g).replace(/"/g,'&quot;')})" title="Editar">✏️</button>
                <button class="btn btn-danger btn-sm" onclick="deleteGasto(${g.id_gasto})" title="Eliminar">🗑️</button>
              </div>
            </div>`;
          }).join('')
      }
    </div>
  `);
}

/* ══════════════════════════════════════════════════════════════
   LIQUIDACIÓN
══════════════════════════════════════════════════════════════ */
async function abonarDeuda(id_gasto, id_usuario, importe) {
  if (!confirm(`¿Confirmar que has pagado ${fmtAmt(importe)}?`)) return;
  const res = await api('reparto_gasto.php','PUT',{
    id_gasto, id_usuario, abonado: 1, importe, pagador: 0
  });
  if (res.status === 'success') { showToast('¡Pago registrado! 💸','ok'); renderCartera(); }
  else showToast('Error al registrar el pago: '+(res.data||''),'err');
}

async function cobrarDeuda(id_gasto, id_usuario_deudor, importe) {
  if (!confirm(`¿Confirmar que te han pagado ${fmtAmt(importe)}?`)) return;
  const res = await api('reparto_gasto.php','PUT',{
    id_gasto, id_usuario: id_usuario_deudor, abonado: 1, importe, pagador: 0
  });
  if (res.status === 'success') { showToast('Cobro registrado ✅','ok'); renderCartera(); }
  else showToast('Error al registrar el cobro: '+(res.data||''),'err');
}

async function deleteGasto(id_gasto) {
  if (!confirm('¿Eliminar este gasto y su reparto?')) return;
  await api('reparto_gasto.php','DELETE',null,{ id_gasto });
  const res = await api('gasto.php','DELETE',null,{ id_gasto });
  if (res.status === 'success') { showToast('Gasto eliminado','ok'); renderCartera(); }
  else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   MURO — filtrado por hogar
══════════════════════════════════════════════════════════════ */
async function renderMuro() {
  const id_hogar = state.user.id_hogar;
  const [rPubs, rUsuarios] = await Promise.all([
    api('muro.php',    'GET', null, { id_hogar }),   // solo del hogar
    api('usuario.php', 'GET', null, { id_hogar }),
  ]);
  const pubs     = getData(rPubs);
  const usuarios = getData(rUsuarios);
  const userMap  = Object.fromEntries(usuarios.map(u => [u.id_usuario, u]));

  document.getElementById('topbar-actions').innerHTML = `
    <button class="btn btn-primary btn-sm" onclick="openModal('modal-pub')">📢 Publicar</button>
  `;

  set(`
    <div style="max-width:660px">
      ${pubs.length === 0
        ? '<div class="empty"><span class="empty-icon">📭</span><p>El muro está vacío. ¡Sé el primero en publicar!</p></div>'
        : pubs.map(p => {
            const autor  = userMap[p.id_usuario];
            const ini    = autor ? autor.nombre.charAt(0).toUpperCase() : 'U';
            const nombre = autor ? autor.nombre : `Usuario #${p.id_usuario}`;
            const avatarUrl = autor ? getAvatarUrl(autor) : '';
            const avatarHTML = (avatarUrl && autor?.avatar !== 'uploads/perfiles/default.png')
              ? `<img src="${avatarUrl}" class="pub-avatar" style="object-fit:cover">`
              : `<div class="pub-avatar">${ini}</div>`;
            const esMio  = p.id_usuario == state.user.id_usuario;
            return `
            <div class="pub-card">
              <div class="pub-header">
                ${avatarHTML}
                <div class="pub-meta">
                  <div class="pub-user">${escHtml(nombre)}</div>
                  <div class="pub-date">${fmtDate(p.fecha_pub)}</div>
                </div>
                ${esMio ? `
                <div class="pub-actions">
                  <button class="btn btn-icon btn-sm" onclick="openEditPub(${p.id_pub},'${escHtml(p.titulo)}','${escHtml(p.cuerpo)}')" title="Editar">✏️</button>
                  <button class="btn btn-danger btn-sm" onclick="deletePub(${p.id_pub})" title="Eliminar">🗑️</button>
                </div>` : ''}
              </div>
              <div class="pub-title">${escHtml(p.titulo)}</div>
              <div class="pub-body">${escHtml(p.cuerpo)}</div>
            </div>`;
          }).join('')
      }
    </div>
  `);
}

async function deletePub(id_pub) {
  if (!confirm('¿Eliminar esta publicación?')) return;
  const res = await api('muro.php','DELETE',null,{ id_pub });
  if (res.status === 'success') { showToast('Publicación eliminada','ok'); renderMuro(); }
  else showToast('Error al eliminar','err');
}

/* ══════════════════════════════════════════════════════════════
   PERFIL
══════════════════════════════════════════════════════════════ */
async function renderPerfil() {
  const u = state.user;
  const avatarUrl = getAvatarUrl(u);

  document.getElementById('topbar-actions').innerHTML = `
    <button class="btn btn-primary btn-sm" onclick="openEditPerfil()">✏️ Editar perfil</button>
  `;

  const avatarHTML = (avatarUrl && u.avatar !== 'uploads/perfiles/default.png')
    ? `<img src="${avatarUrl}" alt="${escHtml(u.nombre)}" style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:3px solid var(--accent)">`
    : `<div class="avatar" style="width:80px;height:80px;font-size:2rem;border-radius:50%">${u.nombre.charAt(0).toUpperCase()}</div>`;

  set(`
    <div style="max-width:560px">
      <div class="perfil-header">
        ${avatarHTML}
        <div>
          <div style="font-size:1.1rem;font-weight:800">${escHtml(u.nombre)}</div>
          <div style="font-size:.82rem;color:var(--muted)">${escHtml(u.email)}</div>
          <span class="tag ${u.rol==='admin'?'tag-yellow':'tag-green'}" style="margin-top:6px;display:inline-block">${u.rol === 'admin' ? '👑 Administrador' : '🧑 Miembro'}</span>
        </div>
      </div>
      <div class="card">
        <div class="card-title" style="margin-bottom:14px">📋 Datos personales</div>
        <div class="grid-2" style="gap:14px">
          <div><div style="font-size:.75rem;color:var(--muted)">Nombre</div><div style="font-weight:600">${escHtml(u.nombre)}</div></div>
          <div><div style="font-size:.75rem;color:var(--muted)">Email</div><div style="font-weight:600">${escHtml(u.email)}</div></div>
          <div><div style="font-size:.75rem;color:var(--muted)">Teléfono</div><div style="font-weight:600">${escHtml(u.telefono_movil||'—')}</div></div>
          <div><div style="font-size:.75rem;color:var(--muted)">Sexo</div><div style="font-weight:600">${u.sexo||'—'}</div></div>
          <div><div style="font-size:.75rem;color:var(--muted)">Fecha nacimiento</div><div style="font-weight:600">${fmtDate(u.fecha_nacimiento)}</div></div>
          <div><div style="font-size:.75rem;color:var(--muted)">Miembro desde</div><div style="font-weight:600">${fmtDate(u.fecha_registro)}</div></div>
        </div>
        <div style="margin-top:18px">
          <button class="btn btn-primary btn-sm" onclick="openEditPerfil()">✏️ Editar mis datos</button>
        </div>
      </div>
    </div>
  `);
}

function openEditPerfil() {
  const u = state.user;
  document.getElementById('ep-nombre').value   = u.nombre   || '';
  document.getElementById('ep-email').value    = u.email    || '';
  document.getElementById('ep-telefono').value = u.telefono_movil || '';
  document.getElementById('ep-sexo').value     = u.sexo     || 'masculino';
  document.getElementById('ep-fecha').value    = u.fecha_nacimiento ? u.fecha_nacimiento.split('T')[0] : '';
  // Limpiar preview avatar
  document.getElementById('ep-avatar').value  = '';
  document.getElementById('ep-avatar-preview').style.display = 'none';
  openModal('modal-perfil');
}

function previewAvatar(input) {
  const file = input.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = e => {
    document.getElementById('ep-avatar-img').src = e.target.result;
    document.getElementById('ep-avatar-preview').style.display = 'block';
  };
  reader.readAsDataURL(file);
}

async function submitPerfil() {
  const nombre   = document.getElementById('ep-nombre').value.trim();
  const email    = document.getElementById('ep-email').value.trim();
  const telefono = document.getElementById('ep-telefono').value.trim();
  const sexo     = document.getElementById('ep-sexo').value;
  const fecha    = document.getElementById('ep-fecha').value || null;

  if (!nombre || !email || !telefono) { showToast('Nombre, email y teléfono son obligatorios','err'); return; }

  // Subir avatar si se eligió uno
  const avatarInput = document.getElementById('ep-avatar');
  let avatarPath = state.user.avatar;

  if (avatarInput.files.length > 0) {
    const formData = new FormData();
    formData.append('avatar', avatarInput.files[0]);
    formData.append('id_usuario', state.user.id_usuario);
    try {
      const r = await fetch(`${BASE_URL}/upload_avatar.php`, { method: 'POST', body: formData });
      const j = await r.json();
      if (j.status === 'success') avatarPath = j.data.path;
      else showToast('Aviso: no se pudo subir la imagen — ' + (j.data || ''), 'warn');
    } catch(e) {
      showToast('Aviso: error al subir imagen', 'warn');
    }
  }

  const res = await api('usuario.php','PUT',{
    id_usuario: state.user.id_usuario,
    nombre, email, telefono_movil: telefono, sexo,
    fecha_nacimiento: fecha,
    avatar: avatarPath
  });
  if (res.status === 'success') {
    state.user = { ...state.user, nombre, email, telefono_movil: telefono, sexo, fecha_nacimiento: fecha, avatar: avatarPath };
    updateSidebar();
    showToast('Perfil actualizado ✓','ok');
    closeModal('modal-perfil');
    renderPerfil();
  } else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   CATEGORÍAS — lista predefinida
══════════════════════════════════════════════════════════════ */
const CATEGORIAS_GASTO = [
  'Alimentación',
  'Suministros',
  'Alquiler / Hipoteca',
  'Transporte',
  'Ocio / Entretenimiento',
  'Salud / Farmacia',
  'Limpieza / Hogar',
  'Ropa / Calzado',
  'Tecnología',
  'Educación',
  'Otros',
];

/* ══════════════════════════════════════════════════════════════
   MODAL — GASTO (POST + PUT)
══════════════════════════════════════════════════════════════ */
let _editGastoId = null;

function openModalGasto() {
  _editGastoId = null;
  document.getElementById('modal-gasto-title').textContent = 'Nuevo Gasto';
  document.getElementById('btn-submit-gasto').textContent  = 'Registrar';
  document.getElementById('g-concepto').value  = '';
  document.getElementById('g-importe').value   = '';
  document.getElementById('g-fecha').value     = new Date().toISOString().split('T')[0];
  document.getElementById('g-modo').value      = 'efectivo';
  document.getElementById('g-tipo').value      = 'ocasional';
  document.getElementById('g-categoria').value = '';
  openModal('modal-gasto');
}

function openEditGasto(g) {
  if (typeof g === 'string') g = JSON.parse(g.replace(/&quot;/g,'"'));
  _editGastoId = g.id_gasto;
  document.getElementById('modal-gasto-title').textContent = 'Editar Gasto';
  document.getElementById('btn-submit-gasto').textContent  = 'Guardar cambios';
  document.getElementById('g-concepto').value  = g.concepto || '';
  document.getElementById('g-importe').value   = g.importe  || '';
  document.getElementById('g-fecha').value     = g.fecha ? g.fecha.split('T')[0] : '';
  document.getElementById('g-modo').value      = g.modo  || 'efectivo';
  document.getElementById('g-tipo').value      = g.tipo  || 'ocasional';
  document.getElementById('g-categoria').value = g.categoria || '';
  openModal('modal-gasto');
}

async function submitGasto() {
  const concepto  = document.getElementById('g-concepto').value.trim();
  const importe   = document.getElementById('g-importe').value;
  const fecha     = document.getElementById('g-fecha').value;
  const categoria = document.getElementById('g-categoria').value;
  const modo      = document.getElementById('g-modo').value;
  const tipo      = document.getElementById('g-tipo').value;

  if (!concepto || !importe || !fecha) {
    showToast('Rellena todos los campos obligatorios','err'); return;
  }

  let res;
  if (_editGastoId) {
    res = await api('gasto.php','PUT',{
      id_gasto: _editGastoId, fecha, categoria, concepto, modo, tipo,
      importe, id_usuario_pagador: state.user.id_usuario
    });
  } else {
    res = await api('gasto.php','POST',{
      fecha, categoria, concepto, modo, tipo,
      importe, id_hogar: state.user.id_hogar,
      id_usuario_pagador: state.user.id_usuario
    });
    if (res.status === 'success') {
      await crearReparto(res.data.autoincrement, parseFloat(importe));
    }
  }

  if (res.status === 'success') {
    showToast(_editGastoId ? 'Gasto actualizado ✓' : 'Gasto registrado y repartido 🐝','ok');
    closeModal('modal-gasto');
    if (state.page === 'cartera')        renderCartera();
    else if (state.page === 'dashboard') dashboard();
  } else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   REPARTO AUTOMÁTICO
══════════════════════════════════════════════════════════════ */
async function crearReparto(id_gasto, totalImporte) {
  const rUsuarios = await api('usuario.php','GET',null,{ id_hogar: state.user.id_hogar });
  const usuarios  = getData(rUsuarios);
  if (usuarios.length === 0) return;

  const n     = usuarios.length;
  const share = Math.round((totalImporte / n) * 1000) / 1000;
  const payer = state.user.id_usuario;

  await Promise.all(
    usuarios.map(u => api('reparto_gasto.php','POST',{
      id_gasto,
      id_usuario: u.id_usuario,
      pagador:    u.id_usuario == payer ? 1 : 0,
      importe:    share,
      abonado:    0
    }))
  );
}

/* ══════════════════════════════════════════════════════════════
   MODAL — TAREA (POST + PUT)
══════════════════════════════════════════════════════════════ */
let _editTareaId = null;

async function openModalTarea() {
  _editTareaId = null;
  document.getElementById('modal-tarea-title').textContent = 'Nueva Tarea';
  document.getElementById('btn-submit-tarea').textContent  = 'Crear tarea';
  document.getElementById('t-nombre').value      = '';
  document.getElementById('t-duracion').value    = '';
  document.getElementById('t-numveces').value    = '1';
  document.getElementById('t-frecuencia').value  = 'semana';
  document.getElementById('t-explicacion').value = '';
  document.getElementById('t-expl-wrap').classList.add('hidden');

  await Promise.all([
    loadHabitacionesSelect(),
    loadUsuariosAsignar(),
  ]);
  openModal('modal-tarea');
}

function openEditTarea(t) {
  if (typeof t === 'string') t = JSON.parse(t.replace(/&quot;/g,'"'));
  _editTareaId = t.id_tarea;
  document.getElementById('modal-tarea-title').textContent = 'Editar Tarea';
  document.getElementById('btn-submit-tarea').textContent  = 'Guardar cambios';
  document.getElementById('t-nombre').value      = t.nombre     || '';
  document.getElementById('t-duracion').value    = t.duracion   || '';
  document.getElementById('t-numveces').value    = t.num_veces  || '1';
  document.getElementById('t-frecuencia').value  = t.frecuencia || 'semana';
  document.getElementById('t-explicacion').value = t.explicacion_frecuencia_variable || '';
  document.getElementById('t-expl-wrap').classList.toggle('hidden', t.frecuencia !== 'variable');

  Promise.all([
    loadHabitacionesSelect(t.id_habitacion),
    loadUsuariosAsignar(),
  ]);
  openModal('modal-tarea');
}

async function loadHabitacionesSelect(selected = '') {
  const rHabs = await api('habitacion.php','GET',null,{ id_hogar: state.user.id_hogar });
  const habs  = getData(rHabs);
  const sel   = document.getElementById('t-habitacion');
  if (!sel) return;
  sel.innerHTML = '<option value="">Sin habitación</option>' +
    habs.map(h => `<option value="${h.id_habitacion}" ${h.id_habitacion==selected?'selected':''}>${escHtml(h.nombre)}</option>`).join('');
}

async function loadUsuariosAsignar() {
  const rU = await api('usuario.php','GET',null,{ id_hogar: state.user.id_hogar });
  const usuarios = getData(rU);
  const sel = document.getElementById('t-asignar-usuario');
  if (!sel) return;
  // Por defecto: asignar al usuario actual
  sel.innerHTML = usuarios.map(u =>
    `<option value="${u.id_usuario}" ${u.id_usuario == state.user.id_usuario ? 'selected' : ''}>${escHtml(u.nombre)} ${u.rol==='admin'?'👑':''}</option>`
  ).join('');
}

async function submitTarea() {
  try {
    const tNombreEl = document.getElementById('t-nombre');
    const tFrecuenciaEl = document.getElementById('t-frecuencia');
    const tNumVecesEl = document.getElementById('t-numveces');
    const tDuracionEl = document.getElementById('t-duracion');
    const tAsignarEl = document.getElementById('t-asignar-usuario');

    if (!tNombreEl || !tFrecuenciaEl || !tNumVecesEl || !tDuracionEl) {
      console.error('submitTarea: elemento(s) esperado(s) no encontrado(s)', {
        tNombreEl, tFrecuenciaEl, tNumVecesEl, tDuracionEl, tAsignarEl
      });
      showToast('Error de interfaz: faltan campos del formulario', 'err');
      return;
    }

    const nombre     = tNombreEl.value.trim();
    const frecuencia = tFrecuenciaEl.value;
    const numVeces   = parseInt(tNumVecesEl.value) || 1;
    const duracion   = parseInt(tDuracionEl.value) || null;

    if (!nombre) { showToast('El nombre es obligatorio','err'); return; }
    if (numVeces < 1 || numVeces > 127) { showToast('Número de veces debe estar entre 1 y 127','err'); return; }
    if (duracion !== null && (duracion < 1 || duracion > 255)) { showToast('Duración debe estar entre 1 y 255 minutos','err'); return; }

    const tHabitacionEl = document.getElementById('t-habitacion');
    const tExplicacionEl = document.getElementById('t-explicacion');

    const body = {
      nombre,
      duracion,
      frecuencia,
      num_veces: numVeces,
      id_habitacion: tHabitacionEl?.value || null,
      explicacion_frecuencia_variable: frecuencia === 'variable'
        ? (tExplicacionEl?.value || null) : null,
      id_hogar: state.user?.id_hogar || null,
    };

    // Obtener usuario a asignar
    const asignarUID = parseInt(tAsignarEl?.value || '') || state.user?.id_usuario || null;

    let res;
    if (_editTareaId) {
      res = await api('tarea.php','PUT',{ ...body, id_tarea: _editTareaId });
    } else {
      res = await api('tarea.php','POST', body);
    }

    console.log('submitTarea: response', res);

    if (res.status === 'success') {
      if (!_editTareaId) {
        const idTarea = res.data?.autoincrement;
        if (idTarea) {
          const rAsig = await api('asignacion_tarea.php','POST', {
            id_tarea: idTarea,
            id_usuario: asignarUID,
          });
          if (rAsig.status !== 'success') {
            showToast('Tarea creada pero no se pudo asignar: ' + (rAsig.data||''), 'warn');
            closeModal('modal-tarea');
            if (state.page === 'tareas') renderTareas();
            else if (state.page === 'dashboard') dashboard();
            return;
          }
        }
      }
      showToast(_editTareaId ? 'Tarea actualizada ✓' : 'Tarea creada y asignada 🐝', 'ok');
      closeModal('modal-tarea');
      if (state.page === 'tareas') renderTareas();
      else if (state.page === 'dashboard') dashboard();
      else if (state.page === 'hogar') renderHogar();
    } else {
      showToast('Error al guardar la tarea: ' + (res.data || 'sin detalle'), 'err');
    }
  } catch (e) {
    console.error('submitTarea error', e);
    showToast('Error interno al guardar tarea: ' + e.message, 'err');
  }
}

/* ══════════════════════════════════════════════════════════════
   MODAL — HABITACION (POST + PUT)
══════════════════════════════════════════════════════════════ */
let _editHabId = null;

function openModalHabitacion() {
  if (state.user.rol !== 'admin') { showToast('Solo el administrador puede crear habitaciones','warn'); return; }
  _editHabId = null;
  document.getElementById('modal-hab-title').textContent = 'Nueva Habitacion';
  document.getElementById('btn-submit-hab').textContent  = 'Crear';
  document.getElementById('h-nombre').value = '';
  document.getElementById('h-tipo').value   = 'generica';
  openModal('modal-hab');
}

function openEditHabitacion(id, nombre, tipo) {
  if (state.user.rol !== 'admin') { showToast('Solo el administrador puede editar habitaciones','warn'); return; }
  _editHabId = id;
  document.getElementById('modal-hab-title').textContent = 'Editar Habitacion';
  document.getElementById('btn-submit-hab').textContent  = 'Guardar';
  document.getElementById('h-nombre').value = nombre;
  document.getElementById('h-tipo').value   = tipo;
  openModal('modal-hab');
}

async function submitHabitacion() {
  const nombre = document.getElementById('h-nombre').value.trim();
  const tipo   = document.getElementById('h-tipo').value;
  if (!nombre) { showToast('Escribe un nombre','err'); return; }

  let res;
  if (_editHabId) {
    res = await api('habitacion.php','PUT',{ id_habitacion: _editHabId, nombre, tipo });
  } else {
    res = await api('habitacion.php','POST',{ nombre, tipo, id_hogar: state.user.id_hogar });
  }

  if (res.status === 'success') {
    showToast(_editHabId ? 'Habitación actualizada ✓' : 'Habitación creada ✓','ok');
    closeModal('modal-hab');
    renderHogar();
  } else showToast('Error: '+(res.data||''),'err');
}

async function deleteHabitacion(id_habitacion) {
  if (state.user.rol !== 'admin') { showToast('Solo el administrador puede eliminar habitaciones','warn'); return; }
  if (!confirm('¿Eliminar esta habitación? También se eliminarán sus tareas asociadas.')) return;
  const res = await api('habitacion.php','DELETE',null,{ id_habitacion });
  if (res.status === 'success') { showToast('Habitación eliminada','ok'); renderHogar(); }
  else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   MODAL — PUBLICACIÓN MURO (POST + PUT)
══════════════════════════════════════════════════════════════ */
let _editPubId = null;

function openModalPub() {
  _editPubId = null;
  document.getElementById('modal-pub-title').textContent = 'Nueva publicacion';
  document.getElementById('btn-submit-pub').textContent  = 'Publicar';
  document.getElementById('p-titulo').value = '';
  document.getElementById('p-cuerpo').value = '';
  openModal('modal-pub');
}

function openEditPub(id, titulo, cuerpo) {
  _editPubId = id;
  document.getElementById('modal-pub-title').textContent = 'Editar publicacion';
  document.getElementById('btn-submit-pub').textContent  = 'Guardar';
  document.getElementById('p-titulo').value = titulo.replace(/&quot;/g,'"');
  document.getElementById('p-cuerpo').value = cuerpo.replace(/&quot;/g,'"');
  openModal('modal-pub');
}

async function submitPub() {
  const titulo = document.getElementById('p-titulo').value.trim();
  const cuerpo = document.getElementById('p-cuerpo').value.trim();
  if (!titulo || !cuerpo) { showToast('Rellena título y contenido','err'); return; }

  let res;
  if (_editPubId) {
    res = await api('muro.php','PUT',{ id_pub: _editPubId, titulo, cuerpo });
  } else {
    res = await api('muro.php','POST',{ titulo, cuerpo, id_usuario: state.user.id_usuario });
  }

  if (res.status === 'success') {
    showToast(_editPubId ? 'Publicación actualizada ✓' : 'Publicado en el muro 🐝','ok');
    closeModal('modal-pub');
    if (state.page === 'muro') renderMuro();
    else if (state.page === 'dashboard') dashboard();
  } else showToast('Error: '+(res.data||''),'err');
}

/* ══════════════════════════════════════════════════════════════
   FRECUENCIA — mostrar/ocultar campo variable
══════════════════════════════════════════════════════════════ */
function onFrecuenciaChange() {
  const val = document.getElementById('t-frecuencia').value;
  document.getElementById('t-expl-wrap').classList.toggle('hidden', val !== 'variable');
}

/* ══════════════════════════════════════════════════════════════
   MODO REGISTRO — mostrar/ocultar campo clave hogar
══════════════════════════════════════════════════════════════ */
function onRegModoChange() {
  const val = document.getElementById('reg-modo').value;
  document.getElementById('reg-clave-wrap').classList.toggle('hidden', val !== 'unirse');
}

/* ══════════════════════════════════════════════════════════════
   SIDEBAR MOBILE TOGGLE
══════════════════════════════════════════════════════════════ */
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('open');
  const overlay = document.getElementById('sidebar-overlay');
  if (overlay) overlay.classList.toggle('open');
}

/* ── escapa HTML para atributos inline ──────────────────── */
function escHtml(str) {
  return String(str||'')
    .replace(/&/g,'&amp;').replace(/"/g,'&quot;')
    .replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
