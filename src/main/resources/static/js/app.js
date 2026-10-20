/**
 * Privacy Awareness System — Shared JavaScript Utilities
 * Handles: API calls with CSRF, auth state, navigation helpers
 */

/* ── CSRF Token ─────────────────────────────────────────────────────────── */
function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/* ── API Helper ─────────────────────────────────────────────────────────── */
async function api(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const csrf = getCsrfToken();
  if (csrf && method !== 'GET') headers['X-XSRF-TOKEN'] = csrf;

  const opts = { method, headers, credentials: 'include' };
  if (body && method !== 'GET') opts.body = JSON.stringify(body);

  const res = await fetch(path, opts);
  if (res.status === 204 || res.headers.get('Content-Length') === '0') return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) throw Object.assign(new Error(data?.message || 'Request failed'), { status: res.status, data });
  return data;
}

const API = {
  get:    (path)         => api('GET',    path, null),
  post:   (path, body)   => api('POST',   path, body),
  put:    (path, body)   => api('PUT',    path, body),
  patch:  (path, body)   => api('PATCH',  path, body),
  delete: (path)         => api('DELETE', path, null),
};

/* ── Auth Helpers ───────────────────────────────────────────────────────── */
let _currentUser = null;

async function getCurrentUser() {
  if (_currentUser) return _currentUser;
  try {
    _currentUser = await API.get('/api/auth/me');
    return _currentUser;
  } catch {
    return null;
  }
}

async function requireAuth(redirectTo = '/login.html') {
  const user = await getCurrentUser();
  if (!user) { window.location.href = redirectTo; return null; }
  return user;
}

async function requireAdmin() {
  const user = await requireAuth();
  if (!user) return null;
  if (user.role !== 'ADMIN') { window.location.href = '/dashboard.html'; return null; }
  return user;
}

async function logout() {
  try { await API.post('/api/auth/logout'); } catch {}
  _currentUser = null;
  window.location.href = '/login.html';
}

/* ── UI Utilities ───────────────────────────────────────────────────────── */
function showAlert(element, message, type = 'error') {
  element.textContent = message;
  element.className = `alert alert-${type}`;
  element.classList.remove('hidden');
  element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  setTimeout(() => element.classList.add('hidden'), 6000);
}

function setLoading(btn, loading) {
  if (loading) {
    btn.dataset.originalText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner"></span>';
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
    btn.disabled = false;
  }
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatDateTime(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function riskBadge(level) {
  const map = { LOW: 'badge-low', MEDIUM: 'badge-medium', HIGH: 'badge-high', CRITICAL: 'badge-critical' };
  return `<span class="badge ${map[level] || ''}">${level}</span>`;
}

function statusBadge(status) {
  const map = {
    OPEN: 'badge-open',
    UNDER_REVIEW: 'badge-under-review',
    RESOLVED: 'badge-resolved',
    DISMISSED: 'badge-dismissed'
  };
  return `<span class="badge ${map[status] || ''}">${status.replace('_', ' ')}</span>`;
}

function roleBadge(role) {
  return `<span class="badge ${role === 'ADMIN' ? 'badge-admin' : 'badge-user'}">${role}</span>`;
}

/* ── Sidebar Setup ──────────────────────────────────────────────────────── */
function initSidebar(activeLink) {
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.dataset.page === activeLink) link.classList.add('active');
  });

  const hamburger = document.getElementById('hamburger');
  const sidebar   = document.getElementById('sidebar');
  const overlay   = document.getElementById('sidebar-overlay');

  if (hamburger && sidebar) {
    hamburger.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      if (overlay) overlay.classList.toggle('hidden');
    });
  }
  if (overlay) {
    overlay.addEventListener('click', () => {
      sidebar?.classList.remove('open');
      overlay.classList.add('hidden');
    });
  }
}

async function populateSidebarUser() {
  const user = await getCurrentUser();
  if (!user) return;
  const nameEl = document.getElementById('sidebar-username');
  const roleEl = document.getElementById('sidebar-role');
  const avatarEl = document.getElementById('sidebar-avatar');
  if (nameEl) nameEl.textContent = user.username;
  if (roleEl) roleEl.textContent = user.role;
  if (avatarEl) avatarEl.textContent = user.username[0].toUpperCase();
}

/* ── Toast Notifications System ─────────────────────────────────────────── */
function showToast(title, message = '', type = 'info', duration = 4000) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const icons = {
    success: '✅',
    error: '❌',
    info: 'ℹ️',
    warning: '⚠️'
  };

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div class="toast-icon">${icons[type] || '🔔'}</div>
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      ${message ? `<div class="toast-message">${message}</div>` : ''}
    </div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('fade-out');
    setTimeout(() => toast.remove(), 320);
  }, duration);
}

/* ── Animated Counter Helper ────────────────────────────────────────────── */
function animateCounter(el, target, duration = 800, suffix = '') {
  if (!el) return;
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    el.textContent = target + suffix;
    return;
  }
  const numericTarget = typeof target === 'number' ? target : parseFloat(target);
  if (isNaN(numericTarget)) {
    el.textContent = target + suffix;
    return;
  }
  const isFloat = String(target).includes('.');
  const startTime = performance.now();
  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const easeOut = 1 - Math.pow(1 - progress, 3);
    const current = numericTarget * easeOut;
    el.textContent = (isFloat ? current.toFixed(1) : Math.round(current)) + suffix;
    if (progress < 1) {
      requestAnimationFrame(update);
    } else {
      el.textContent = target + suffix;
    }
  }
  requestAnimationFrame(update);
}

/* ── HTML Escaping ──────────────────────────────────────────────────────── */
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/* ── Change Password Modal Logic ────────────────────────────────────────── */
function openChangePasswordModal() {
  const modal = document.getElementById('change-password-modal');
  if (!modal) return;
  const form = document.getElementById('change-pwd-form');
  if (form) form.reset();
  const alertEl = document.getElementById('change-pwd-alert');
  if (alertEl) alertEl.classList.add('hidden');
  modal.classList.add('active');
}

function closeChangePasswordModal() {
  const modal = document.getElementById('change-password-modal');
  if (modal) modal.classList.remove('active');
}

async function submitChangePassword() {
  const currentPassword = document.getElementById('pwd-current')?.value;
  const newPassword = document.getElementById('pwd-new')?.value;
  const confirmPassword = document.getElementById('pwd-confirm')?.value;
  const alertEl = document.getElementById('change-pwd-alert');
  const btn = document.getElementById('pwd-submit-btn');

  if (!currentPassword || !newPassword || !confirmPassword) {
    if (alertEl) showAlert(alertEl, 'Please fill in all password fields.');
    return;
  }
  if (newPassword !== confirmPassword) {
    if (alertEl) showAlert(alertEl, 'New password and confirmation do not match.');
    return;
  }
  if (newPassword.length < 8) {
    if (alertEl) showAlert(alertEl, 'Password must be at least 8 characters.');
    return;
  }

  if (btn) setLoading(btn, true);
  try {
    const res = await API.post('/api/auth/change-password', {
      currentPassword,
      newPassword,
      confirmPassword
    });
    closeChangePasswordModal();
    showToast('Password Updated', res?.message || 'Password changed successfully.', 'success');
  } catch (e) {
    const msg = e.data?.message || e.message || 'Failed to update password.';
    if (alertEl) showAlert(alertEl, msg);
    showToast('Error', msg, 'error');
  } finally {
    if (btn) setLoading(btn, false);
  }
}

