const BASE = import.meta.env.VITE_API_URL || '';
const TOKEN_KEY = 'rosterrelay.token';

export function getToken() { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } }
export function setToken(t) { try { if (t) localStorage.setItem(TOKEN_KEY, t); else localStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ } }

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || (status === 403 ? "You don't have access to that" : `Request failed (${status})`));
    this.status = status;
    this.fieldErrors = problem?.errors || {};
  }
}

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

async function request(method, path, body, { raw = false } = {}) {
  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(`${BASE}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  if (res.status === 401 && token) { setToken(null); onUnauthorized(); }
  if (!res.ok) {
    let problem = null;
    try { problem = await res.json(); } catch { /* not json */ }
    throw new ApiError(res.status, problem);
  }
  if (raw) return res;
  return res.status === 204 ? null : res.json();
}

export const api = {
  register: (d) => request('POST', '/api/auth/register', d),
  login: (d) => request('POST', '/api/auth/login', d),
  me: () => request('GET', '/api/me'),
  rotateCalendar: () => request('POST', '/api/me/calendar/rotate'),
  browse: () => request('GET', '/api/shifts'),
  signUp: (id) => request('POST', `/api/shifts/${id}/signup`),
  mine: () => request('GET', '/api/signups/mine'),
  cancel: (id) => request('POST', `/api/signups/${id}/cancel`),
  hours: () => request('GET', '/api/me/hours'),
  inbox: () => request('GET', '/api/me/notifications'),
  markRead: () => request('POST', '/api/me/notifications/read'),
  coordShifts: () => request('GET', '/api/coordinator/shifts'),
  createShift: (d) => request('POST', '/api/coordinator/shifts', d),
  cancelShift: (id) => request('POST', `/api/coordinator/shifts/${id}/cancel`),
  roster: (id) => request('GET', `/api/coordinator/shifts/${id}/roster`),
  attendance: (signupId, attended) => request('POST', `/api/coordinator/signups/${signupId}/attendance`, { attended }),
  dashboard: () => request('GET', '/api/coordinator/dashboard'),
  invite: (shiftId, volunteerId) => request('POST', `/api/coordinator/shifts/${shiftId}/invite/${volunteerId}`),
  downloadHours: async () => {
    const res = await request('GET', '/api/me/hours.csv', undefined, { raw: true });
    const url = URL.createObjectURL(await res.blob());
    const a = document.createElement('a');
    a.href = url; a.download = 'volunteer-hours.csv'; a.click();
    URL.revokeObjectURL(url);
  },
};

export const label = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase().replaceAll('_', ' ') : '');
export const day = (iso) => new Date(iso).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
export const time = (iso) => new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
export const hoursUntil = (iso) => (new Date(iso).getTime() - Date.now()) / 3_600_000;
