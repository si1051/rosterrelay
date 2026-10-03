import { day, time } from '../api.js';

export function DateBlock({ iso }) {
  const d = new Date(iso);
  return (
    <div className="dateblock" aria-hidden>
      <span>{d.toLocaleDateString(undefined, { month: 'short' })}</span>
      <strong>{d.getDate()}</strong>
      <span>{d.toLocaleDateString(undefined, { weekday: 'short' })}</span>
    </div>
  );
}

export function FillBar({ confirmed, capacity, waitlisted }) {
  const pct = Math.min(100, Math.round((confirmed / capacity) * 100));
  const cls = pct >= 100 ? 'full' : pct >= 60 ? 'ok' : 'low';
  return (
    <div className="fillbar">
      <div className="track"><div className={`fill ${cls}`} style={{ width: `${pct}%` }} /></div>
      <span className="small muted">{confirmed}/{capacity} filled{waitlisted > 0 && ` · ${waitlisted} waitlisted`}</span>
    </div>
  );
}

export default function ShiftCard({ shift: s, children }) {
  return (
    <article className="card shift">
      <DateBlock iso={s.startsAt} />
      <div className="shift-body">
        <h3>{s.title}</h3>
        <div className="muted small">{s.organization} · {day(s.startsAt)}, {time(s.startsAt)}–{time(s.endsAt)} · {s.hours} h</div>
        <div className="small">📍 {s.location}</div>
        {s.description && <p className="small desc">{s.description}</p>}
        <FillBar confirmed={s.confirmed} capacity={s.capacity} waitlisted={s.waitlisted} />
      </div>
      <div className="shift-actions">{children}</div>
    </article>
  );
}
