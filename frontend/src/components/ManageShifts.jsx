import { useCallback, useEffect, useState } from 'react';
import { api, label } from '../api.js';
import ShiftCard from './ShiftCard.jsx';

function local(dt) {
  const p = (n) => String(n).padStart(2, '0');
  return `${dt.getFullYear()}-${p(dt.getMonth() + 1)}-${p(dt.getDate())}T${p(dt.getHours())}:${p(dt.getMinutes())}`;
}

function Roster({ shift, notify, onChanged }) {
  const [rows, setRows] = useState(null);
  const load = useCallback(() => api.roster(shift.id).then(setRows).catch((e) => notify(e.message, 'error')), [shift.id, notify]);
  useEffect(() => { load(); }, [load]);
  const mark = async (r, attended) => {
    try { await api.attendance(r.signupId, attended); load(); onChanged(); } catch (e) { notify(e.message, 'error'); }
  };
  if (!rows) return <p className="muted small">Loading roster…</p>;
  if (rows.length === 0) return <p className="muted small">No signups yet.</p>;
  const started = new Date(shift.startsAt) <= new Date();
  return (
    <table className="table roster">
      <thead><tr><th>Volunteer</th><th>Contact</th><th>Reliability</th><th>Status</th><th /></tr></thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.signupId}>
            <td>{r.name}</td>
            <td className="small">{r.email}{r.phone && <><br />{r.phone}</>}</td>
            <td><span className={r.reliability >= 80 ? 'good' : r.reliability < 60 ? 'bad' : ''}>{r.reliability}%</span></td>
            <td><span className={`pill st-${r.status.toLowerCase()}`}>{r.status === 'WAITLISTED' ? `Waitlist #${r.waitlistPosition}` : label(r.status)}</span></td>
            <td>
              {started && ['CONFIRMED', 'ATTENDED', 'NO_SHOW'].includes(r.status) && (
                <div className="row-inline">
                  <button className={r.status === 'ATTENDED' ? 'btn primary' : 'btn ghost'} onClick={() => mark(r, true)}>Present</button>
                  <button className={r.status === 'NO_SHOW' ? 'btn danger' : 'btn ghost'} onClick={() => mark(r, false)}>No-show</button>
                </div>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default function ManageShifts({ notify }) {
  const start = new Date(Date.now() + 2 * 86_400_000); start.setMinutes(0, 0, 0);
  const [f, setF] = useState({ title: '', description: '', location: '', startsAt: local(start), endsAt: local(new Date(start.getTime() + 3 * 3_600_000)), capacity: 8 });
  const [errors, setErrors] = useState({});
  const [shifts, setShifts] = useState(null);
  const [open, setOpen] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const load = useCallback(() => api.coordShifts().then(setShifts).catch((e) => notify(e.message, 'error')), [notify]);
  useEffect(() => { load(); }, [load]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const create = async (e) => {
    e.preventDefault(); setErrors({});
    try {
      await api.createShift({ ...f, capacity: Number(f.capacity), startsAt: new Date(f.startsAt).toISOString(), endsAt: new Date(f.endsAt).toISOString() });
      notify('Shift posted'); setShowForm(false); setF({ ...f, title: '', description: '' }); load();
    } catch (err) { setErrors({ ...err.fieldErrors, _: err.message }); }
  };
  const cancelShift = async (s) => {
    if (!window.confirm(`Cancel "${s.title}"? Everyone signed up will be notified.`)) return;
    try { await api.cancelShift(s.id); notify('Shift cancelled and volunteers notified'); load(); } catch (e) { notify(e.message, 'error'); }
  };
  const err = (k) => errors[k] && <small className="err">{errors[k]}</small>;

  return (
    <div className="list">
      <div className="item-head"><h2>Your shifts</h2><button className="btn primary" onClick={() => setShowForm(!showForm)}>{showForm ? 'Close' : '+ New shift'}</button></div>
      {showForm && (
        <form className="card form" onSubmit={create}>
          <div className="row">
            <label>Title<input value={f.title} onChange={set('title')} required maxLength={120} />{err('title')}</label>
            <label>Location<input value={f.location} onChange={set('location')} required />{err('location')}</label>
          </div>
          <div className="row">
            <label>Starts<input type="datetime-local" value={f.startsAt} onChange={set('startsAt')} required />{err('startsAt')}</label>
            <label>Ends<input type="datetime-local" value={f.endsAt} onChange={set('endsAt')} required />{err('durationValid')}</label>
            <label>Volunteers needed<input type="number" min="1" max="500" value={f.capacity} onChange={set('capacity')} />{err('capacity')}</label>
          </div>
          <label>What will volunteers do?<textarea rows={3} value={f.description} onChange={set('description')} /></label>
          {errors._ && <div className="alert">{errors._}</div>}
          <div className="actions"><button className="btn primary">Post shift</button></div>
        </form>
      )}
      {!shifts ? <p className="muted">Loading…</p> : shifts.map((s) => (
        <div key={s.id}>
          <ShiftCard shift={s}>
            {s.cancelled ? <span className="pill st-cancelled">Cancelled</span> : (
              <>
                <button className="btn ghost" onClick={() => setOpen(open === s.id ? null : s.id)}>{open === s.id ? 'Hide roster' : 'Roster'}</button>
                {new Date(s.startsAt) > new Date() && <button className="btn danger" onClick={() => cancelShift(s)}>Cancel</button>}
              </>
            )}
          </ShiftCard>
          {open === s.id && <div className="card roster-wrap"><Roster shift={s} notify={notify} onChanged={load} /></div>}
        </div>
      ))}
    </div>
  );
}
