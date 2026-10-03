import { useState } from 'react';
import { api } from '../api.js';

export default function AuthPage({ onAuthed }) {
  const [mode, setMode] = useState('login');
  const [f, setF] = useState({ email: '', password: '', name: '', role: 'VOLUNTEER', organization: '', phone: '' });
  const [error, setError] = useState('');
  const [fe, setFe] = useState({});
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError(''); setFe({});
    try {
      const res = mode === 'login' ? await api.login({ email: f.email, password: f.password }) : await api.register(f);
      onAuthed(res.token);
    } catch (err) { setError(err.message); setFe(err.fieldErrors || {}); } finally { setBusy(false); }
  };
  const err = (k) => fe[k] && <small className="err">{fe[k]}</small>;

  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit}>
        <div className="brand big"><span className="logo">◆</span> ShiftMate</div>
        <p className="muted">Volunteer scheduling that actually fills shifts: waitlists that refill themselves, reminders, and a no-show-aware dashboard.</p>
        {mode === 'register' && (
          <>
            <div className="role-pick two" role="radiogroup" aria-label="I am a">
              {[['VOLUNTEER', 'I want to volunteer', 'Find shifts and track hours'], ['COORDINATOR', 'I run an organization', 'Post shifts and manage rosters']].map(([r, t, d]) => (
                <button type="button" key={r} className={f.role === r ? 'role-opt active' : 'role-opt'} onClick={() => setF({ ...f, role: r })} aria-pressed={f.role === r}>
                  <strong>{t}</strong><small>{d}</small>
                </button>
              ))}
            </div>
            <label>Your name<input value={f.name} onChange={set('name')} required />{err('name')}</label>
            {f.role === 'COORDINATOR' && <label>Organization<input value={f.organization} onChange={set('organization')} required />{err('organizationPresent')}</label>}
            <label>Phone (for reminders)<input value={f.phone} onChange={set('phone')} inputMode="tel" /></label>
          </>
        )}
        <label>Email<input type="email" value={f.email} onChange={set('email')} required autoComplete="email" />{err('email')}</label>
        <label>Password<input type="password" value={f.password} onChange={set('password')} required autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />{err('password')}</label>
        {error && <div className="alert">{error}</div>}
        <button className="btn primary full" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <button type="button" className="btn link" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}>
          {mode === 'login' ? 'New here? Create an account' : 'Have an account? Sign in'}
        </button>
      </form>
    </div>
  );
}
