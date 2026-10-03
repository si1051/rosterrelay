import { useCallback, useEffect, useState } from 'react';
import { api, hoursUntil, label } from '../api.js';
import ShiftCard from './ShiftCard.jsx';

export default function MyShifts({ notify }) {
  const [items, setItems] = useState(null);
  const load = useCallback(() => api.mine().then(setItems).catch((e) => notify(e.message, 'error')), [notify]);
  useEffect(() => { load(); }, [load]);

  const cancel = async (s) => {
    const late = s.status === 'CONFIRMED' && hoursUntil(s.shift.startsAt) < 24;
    if (late && !window.confirm('This shift starts within 24 hours. Cancelling now counts as a late cancellation. Continue?')) return;
    try { await api.cancel(s.id); notify('Cancelled. Your spot went to the next person on the waitlist.'); load(); } catch (e) { notify(e.message, 'error'); }
  };

  if (!items) return <p className="muted">Loading…</p>;
  const upcoming = items.filter((s) => new Date(s.shift.startsAt) > new Date() && ['CONFIRMED', 'WAITLISTED'].includes(s.status));
  const past = items.filter((s) => !upcoming.includes(s)).reverse();
  return (
    <div className="list">
      <h2>Upcoming</h2>
      {upcoming.length === 0 && <p className="muted">No upcoming shifts. Find one in the Find shifts tab.</p>}
      {upcoming.map((s) => (
        <ShiftCard key={s.id} shift={s.shift}>
          <span className={`pill st-${s.status.toLowerCase()}`}>{s.status === 'WAITLISTED' ? `Waitlist #${s.waitlistPosition}` : 'Confirmed'}</span>
          <button className="btn ghost" onClick={() => cancel(s)}>{s.status === 'WAITLISTED' ? 'Leave waitlist' : 'Cancel'}</button>
        </ShiftCard>
      ))}
      <h2>History</h2>
      {past.map((s) => (
        <ShiftCard key={s.id} shift={s.shift}>
          <span className={`pill st-${s.status.toLowerCase()}`}>{label(s.status)}</span>
          {s.hoursCredited > 0 && <span className="small good">+{s.hoursCredited} h</span>}
        </ShiftCard>
      ))}
    </div>
  );
}
