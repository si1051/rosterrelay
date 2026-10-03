import { useEffect, useState } from 'react';
import { api, day } from '../api.js';

export default function Hours({ me, notify, onRotated }) {
  const [h, setH] = useState(null);
  useEffect(() => { api.hours().then(setH).catch((e) => notify(e.message, 'error')); }, [notify]);
  if (!h) return <p className="muted">Loading…</p>;
  const calUrl = `${window.location.origin}${me.calendarPath}`;
  const copy = async () => {
    try { await navigator.clipboard.writeText(calUrl); notify('Calendar link copied'); } catch { notify('Copy failed; select the link manually', 'error'); }
  };
  const rotate = async () => {
    try { const r = await api.rotateCalendar(); onRotated(r.calendarPath); notify('New private link created; the old one no longer works'); } catch (e) { notify(e.message, 'error'); }
  };
  const r = h.reliability;
  return (
    <div className="analytics narrow">
      <div className="stats">
        <div className="card stat"><div className="stat-value">{h.totalHours}</div><div className="stat-label">Hours volunteered</div></div>
        <div className="card stat"><div className="stat-value">{h.shiftsAttended}</div><div className="stat-label">Shifts completed</div></div>
        <div className="card stat"><div className="stat-value">{r.score}%</div><div className="stat-label">Reliability</div>
          <div className="muted small">{r.noShows} no-shows · {r.lateCancels} late cancels</div></div>
      </div>
      <section className="card">
        <div className="item-head"><h3>Service log</h3><button className="btn primary" onClick={() => api.downloadHours().catch((e) => notify(e.message, 'error'))}>Download CSV</button></div>
        <p className="muted small">A ready-made record for school, scholarship, court or employer volunteer-hour requirements.</p>
        <ul className="status-list">
          {Object.entries(h.byOrganization).map(([org, hrs]) => <li key={org}><span>{org}</span><strong>{hrs} h</strong></li>)}
        </ul>
        <table className="table">
          <thead><tr><th>Date</th><th>Shift</th><th>Organization</th><th>Hours</th></tr></thead>
          <tbody>{h.entries.map((e) => <tr key={e.signupId}><td>{day(e.date)}</td><td>{e.shift}</td><td>{e.organization}</td><td>{e.hours}</td></tr>)}</tbody>
        </table>
      </section>
      <section className="card">
        <h3>Add your shifts to your calendar</h3>
        <p className="muted small">Subscribe in Google Calendar (Other calendars → From URL), Apple Calendar or Outlook. Confirmed shifts appear automatically.</p>
        <div className="row-inline"><code className="calurl">{calUrl}</code><button className="btn ghost" onClick={copy}>Copy</button><button className="btn ghost" onClick={rotate}>Reset link</button></div>
      </section>
    </div>
  );
}
